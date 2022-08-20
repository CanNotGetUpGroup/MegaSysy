package analysis;

import ir.*;
import analysis.MemoryAccess.*;
import ir.instructions.Instructions;
import org.antlr.v4.runtime.misc.Pair;
import pass.PassManager;
import pass.passes.Mem2Reg;
import pass.passes.SimplifyCFG;
import pass.test.testPass;

import java.io.IOException;
import java.security.PublicKey;
import java.util.*;

/**
 * 精确到数组元素的SSA(ArraySSA)
 */
public class MemorySSA {
    public static boolean arraySSA=false;//TODO:暂时关闭了ArraySSA
    private final HashMap<Value, MemoryAccess> ValueToMemAcc;
    private final HashMap<Value,HashMap<BasicBlock,MemoryAccess>> PointerToPhi;
    //指令移动后可能失效
    public final HashMap<BasicBlock, LinkedList<MemoryAccess>> BlockToMemAccList;//基本块中储存的MemoryAccess
    public final HashMap<BasicBlock, LinkedList<MemoryAccess>> BlockToMemDefList;//基本块中储存的MemoryDef和MemoryPhi
    private final MemoryAccess LiveOnEntry;
    private int ID;
    private final Function F;
    private final DominatorTree DT;

    private final HashMap<BasicBlock,Integer> BBNumbers= new HashMap<>();
    private final HashMap<Instructions.CallInst,ArrayList<Value>> CI2Pointers=new HashMap<>();
    private final HashMap<Value,ArrayList<BasicBlock>> Pointer2Defs=new HashMap<>();

    private final HashMap<MemoryPhi,Integer> PhiToLoad =new HashMap<>();
    HashMap<Instructions.LoadInst,Integer> LoadLoopUp=new HashMap<>();
    ArrayList<Instructions.LoadInst> Loads=new ArrayList<>();

    //这里DimInfo判断是否相等，需要GVNGCM多跑几遍来确保准确
    private final HashMap<Value,HashMap<DimInfo,ArrayList<BasicBlock>>> Mem2Defs=new HashMap<>();
    private final HashMap<Value,ArrayList<DimInfo>> Pointer2DimInfo=new HashMap<>();
    private final HashMap<Value,HashMap<DimInfo,HashMap<BasicBlock,MemoryAccess>>> Mem2Phi=new HashMap<>();
    private final HashMap<Value,DimInfo> MemAcc2Dim=new HashMap<>();
    private static final DimInfo nullDimInfo=new DimInfo("notDim");

    public MemorySSA(Function F, DominatorTree DT) {
        this.F = F;
        this.DT = DT;
        ValueToMemAcc = new HashMap<>();
        PointerToPhi = new HashMap<>();
        BlockToMemAccList = new HashMap<>();
        BlockToMemDefList = new HashMap<>();
        LiveOnEntry = new MemoryAccess(Instruction.Ops.MemDef, F.getEntryBB());
        ID=1;
        buildMemorySSA();
    }

    public void clear(){
        Pointer2DimInfo.clear();
        Mem2Defs.clear();
        BlockToMemAccList.forEach((bb,accesses)->{
            accesses.forEach(MemoryAccess::remove);
        });
    }

    public boolean callAlias(Instructions.CallInst CI, Value Pointer){
        return CI2Pointers.getOrDefault(CI,new ArrayList<>()).contains(Pointer);
    }

    /**
     * 判断dominator是否支配dominated，若在不同基本块，则通过Dominator Tree判断，
     * 若在同一基本块，则调用localDominates()
     */
    public boolean dominates(MemoryAccess dominator, MemoryAccess dominated) {
        if (dominator == dominated) return true;
        if (isLiveOnEntry(dominated)) return false;
        if (dominator.getParent() != dominated.getParent()) {
            return DT.dominates(dominator.getParent(), dominated.getParent());
        }
        return localDominates(dominator, dominated);
    }

    //TODO:同一基本块支配判断
    public boolean localDominates(MemoryAccess dominator, MemoryAccess dominated) {
        if(dominator==dominated) return true;
        if(isLiveOnEntry(dominated)) return false;
        if(isLiveOnEntry(dominator)) return true;
        //否则比较二者顺序，在前面的支配后面的
        BasicBlock BB=dominator.getParent();
        LinkedList<MemoryAccess> defs=BlockToMemDefList.get(BB);
        return defs.indexOf(dominator)<defs.indexOf(dominated);
    }

    private void getAllDimInfo(){
        for(GlobalVariable g:F.getParent().getGlobalVariables()){
            Pointer2Defs.put(g,new ArrayList<>());
            Mem2Defs.put(g,new HashMap<>());
            ArrayList<DimInfo> dimInfos=new ArrayList<>();
            if(g.getOperand(0).getType().isArrayTy()){
                dfsUse(g,dimInfos);
            }else{
                dimInfos.add(nullDimInfo);
            }
            Pointer2DimInfo.put(g,dimInfos);
        }
        for(Instruction I:F.getEntryBB().getInstList()){
            if(!(I instanceof Instructions.AllocaInst)) break;
            Pointer2Defs.put(I,new ArrayList<>());
            Mem2Defs.put(I,new HashMap<>());
            ArrayList<DimInfo> dimInfos=new ArrayList<>();
            Type allocaTy=((Instructions.AllocaInst) I).getAllocatedType();
            if(allocaTy.isArrayTy()||allocaTy.isPointerTy()){
                dfsUse(I,dimInfos);
            }else{
                dimInfos.add(nullDimInfo);
            }
            Pointer2DimInfo.put(I,dimInfos);
        }
    }

    private void dfsUse(Value v,ArrayList<DimInfo> arrayList){
        for (Use use : v.getUseList()) {
            //只考虑本函数内的user
            if((use.getU() instanceof Instructions.GetElementPtrInst
                    ||(v instanceof Instructions.AllocaInst&&use.getU() instanceof Instructions.LoadInst))
                    &&((Instruction) use.getU()).getFunction().equals(F)){
                if(use.getU() instanceof Instructions.GetElementPtrInst&&
                        !((Instructions.GetElementPtrInst) use.getU()).getDimInfoDirectly().getOperandList().isEmpty()){
                    arrayList.add(((Instructions.GetElementPtrInst) use.getU()).getDimInfoDirectly());
                }
                dfsUse(use.getU(),arrayList);
            }
        }
    }

    public void buildMemorySSA() {
        getAllDimInfo();
        //先生成MemoryDef和MemoryUse，但不为他们指定definingAccess
        for (BasicBlock BB : F.getBbList()) {
            LinkedList<MemoryAccess> Accesses = null;
            LinkedList<MemoryAccess> Defs = null;
            for (Instruction I : BB.getInstList()) {
                MemoryDefOrUse MUD = createNewAccess(I);
                if (MUD == null) {
                    continue;
                }
                if (Accesses == null) {
                    Accesses = getOrAddAccessList(BB);
                }
                Accesses.add(MUD);
                if ((MUD instanceof MemoryDef)) {
                    if (Defs == null) {
                        Defs = getOrAddDefList(BB);
                    }
                    Defs.add(MUD);
                }
            }
        }
        Set<BasicBlock> Visited = new HashSet<>();
        if(!arraySSA){
            Pointer2Defs.forEach((pointer, defs) -> {
                ArrayList<BasicBlock> PHIBasicBlocks = new ArrayList<>();
                placePHINodes(DT, defs, pointer, PHIBasicBlocks);
            });

            HashMap<Value, MemoryAccess> IncomingValues = new HashMap<>();
            Pointer2Defs.keySet().forEach((pointer) -> {
                IncomingValues.put(pointer, LiveOnEntry);
            });

            RenamePass(DT, IncomingValues, Visited);
        }else{
            //ArraySSA def
            Mem2Defs.forEach((pointer,dimInfos)->{
                dimInfos.forEach((dim,defs)->{
                    ArrayList<BasicBlock> PHIBasicBlocks = new ArrayList<>();
                    placeArrayPHINodes(DT, defs,pointer,dim, PHIBasicBlocks);
                });
            });

            HashMap<Value,HashMap<DimInfo,MemoryAccess>> IncomingValues=new HashMap<>();
            Pointer2DimInfo.forEach((pointer,dimInfos)->{
                IncomingValues.put(pointer,new HashMap<>());
                dimInfos.forEach((dim)->{
                    IncomingValues.get(pointer).put(dim,LiveOnEntry);
                });
            });
//            Mem2Defs.forEach((pointer,dimInfos)->{
//                if(!IncomingValues.containsKey(pointer)){
//                    IncomingValues.put(pointer,new HashMap<>());
//                    dimInfos.keySet().forEach((dim)->{
//                        IncomingValues.get(pointer).put(dim,LiveOnEntry);
//                    });
//                }
//            });
            RenameArrayPass(DT,IncomingValues,Visited);
        }

        //Load MemPhi
        for(Instructions.LoadInst LI:Loads){
            ArrayList<BasicBlock> PHIBasicBlocks = new ArrayList<>();
            placeLoadPHINodes(DT, LI.getParent(),LI,PHIBasicBlocks);
        }

        Visited = new HashSet<>();
        HashMap<Value,Instruction> IncomingLoadValues=new HashMap<>();

        for(Instruction I:Loads){
            IncomingLoadValues.put(I,LiveOnEntry);
        }

        Stack<RenameLoadPassData> RenamePassWorkList=new Stack<>();
        RenamePassWorkList.add(new RenameLoadPassData(DT.Parent.getEntryBB(),null,IncomingLoadValues));
        do{
            RenameLoadPassData RPD=RenamePassWorkList.pop();
            RenameLoadPass(RPD.BB,RPD.Pred,RPD.Val,RenamePassWorkList,Visited);
        }while(!RenamePassWorkList.isEmpty());

        //clear
        boolean changed=true;
        while (changed){
            changed=false;
            for(BasicBlock BB:F.getBbList()){
                LinkedList<MemoryAccess> defs=BlockToMemDefList.get(BB);
                if(defs!=null){
                    Stack<MemoryAccess> delete=new Stack<>();
                    for(MemoryAccess MA:defs){
                        if(!(MA instanceof MemoryPhi)) break;
                        if(MA.getUseList().isEmpty()){
                            MA.remove();
                            delete.push(MA);
                            changed=true;
                        }
                    }
                    while (!delete.isEmpty()){
                        MemoryAccess deleted=delete.pop();
                        BlockToMemDefList.get(deleted.getParent()).remove(deleted);
                        BlockToMemAccList.get(deleted.getParent()).remove(deleted);
                    }
                }
            }
        }

        //TODO：将无法到达的基本块设为LiveOnEntry
    }

    /**
     * 计算支配树边界，找到插入MemPhi指令的位置，参考mem2reg的IDFCalculate方法
     */
    public void placePHINodes(DominatorTree DT, ArrayList<BasicBlock> DefiningBlocks,
                              Value pointer, ArrayList<BasicBlock> IDFBlocks) {
        if(DefiningBlocks==null) return;
        Mem2Reg.IDFCalculate(DT, DefiningBlocks, null, IDFBlocks);

        if(BBNumbers.size()==0){
            int ID=0;
            for(BasicBlock basicBlock:DT.Parent.getBbList()){
                BBNumbers.put(basicBlock,ID++);
            }
        }
        //升序排列，便于处理
        IDFBlocks.sort(Comparator.comparingInt(BBNumbers::get));

        //插入phi
        for (BasicBlock BB : IDFBlocks) {
            MemoryPhi memPhi = new MemoryPhi(BB, ID++);
            memPhi.setPointer(pointer);
            getOrAddAccessList(BB).addFirst(memPhi);
            getOrAddDefList(BB).addFirst(memPhi);
//            ValueToMemAcc.put(BB, memPhi);
            HashMap<BasicBlock,MemoryAccess> bbPhis = PointerToPhi.getOrDefault(pointer,new HashMap<>());
            bbPhis.put(BB,memPhi);
            PointerToPhi.put(pointer,bbPhis);
        }
    }

    public void placeLoadPHINodes(DominatorTree DT, BasicBlock DefiningBlock,
                                  Instructions.LoadInst LI, ArrayList<BasicBlock> IDFBlocks) {
        if(DefiningBlock==null) return;
        Mem2Reg.IDFCalculate(DT, new ArrayList<>(){{add(DefiningBlock);}}, null, IDFBlocks);

        if(BBNumbers.size()==0){
            int ID=0;
            for(BasicBlock basicBlock:DT.Parent.getBbList()){
                BBNumbers.put(basicBlock,ID++);
            }
        }
        //升序排列，便于处理
        IDFBlocks.sort(Comparator.comparingInt(BBNumbers::get));

        //插入phi
        for (BasicBlock BB : IDFBlocks) {
            MemoryPhi memPhi = new MemoryPhi(BB, ID++);
            getOrAddAccessList(BB).addFirst(memPhi);
            getOrAddDefList(BB).addFirst(memPhi);
            memPhi.setPointer(LI);
            PhiToLoad.put(memPhi,LoadLoopUp.get(LI));
        }
    }

    public void placeArrayPHINodes(DominatorTree DT, ArrayList<BasicBlock> DefiningBlocks,
                                   Value pointer,DimInfo dimInfo, ArrayList<BasicBlock> IDFBlocks) {
        if(DefiningBlocks==null) return;
        Mem2Reg.IDFCalculate(DT, DefiningBlocks, null, IDFBlocks);

        if(BBNumbers.size()==0){
            int ID=0;
            for(BasicBlock basicBlock:DT.Parent.getBbList()){
                BBNumbers.put(basicBlock,ID++);
            }
        }
        //升序排列，便于处理
        IDFBlocks.sort(Comparator.comparingInt(BBNumbers::get));

        //插入phi
        for (BasicBlock BB : IDFBlocks) {
            MemoryPhi memPhi = new MemoryPhi(BB, ID++);
            memPhi.setPointer(pointer);
            memPhi.setDimInfo(dimInfo);
            getOrAddAccessList(BB).addFirst(memPhi);
            getOrAddDefList(BB).addFirst(memPhi);

            HashMap<DimInfo,HashMap<BasicBlock,MemoryAccess>> ptrPhis = Mem2Phi.getOrDefault(pointer,new HashMap<>());
            HashMap<BasicBlock,MemoryAccess> dimPhis=ptrPhis.getOrDefault(dimInfo,new HashMap<>());
            dimPhis.put(BB,memPhi);
            ptrPhis.put(dimInfo,dimPhis);
            Mem2Phi.put(pointer,ptrPhis);
        }
    }

    static class RenamePassData {
        public BasicBlock BB;
        public BasicBlock Pred;
        HashMap<Value,MemoryAccess> Val;

        public RenamePassData(BasicBlock BB, BasicBlock pred, HashMap<Value,MemoryAccess> val) {
            this.BB = BB;
            Pred = pred;
            Val = val;
        }
    }

    /**
     * 添加MemPHI的IncomingVal，参考mem2reg的RenamePass
     */
    public void RenamePass(DominatorTree DT, HashMap<Value,MemoryAccess> IncomingVal, Set<BasicBlock> Visited) {
        DominatorTree.TreeNode Root = DT.Root;
        boolean AlreadyVisited = !Visited.add(Root.BB);
        if (AlreadyVisited)
            return;
        renameBlock(Root.BB, IncomingVal);
        renameSuccessorPhis(Root.BB, IncomingVal);
        dfsRename(new RenamePassData(Root.BB,null, IncomingVal),Visited);
    }

    public void dfsRename(RenamePassData RPD, Set<BasicBlock> Visited) {
        BasicBlock Pred = RPD.BB;
        if (Pred.getSuccessorsNum()!=0) {
            for (BasicBlock BB : Pred.getSuccessors()) {
                HashMap<Value,MemoryAccess> IncomingVal = new HashMap<>(RPD.Val);
                boolean AlreadyVisited = !Visited.add(BB);
                if (AlreadyVisited) {
                    LinkedList<MemoryAccess> BlockDefs=BlockToMemDefList.get(BB);
                    if (BlockDefs != null) {
                        for(MemoryAccess MA:BlockDefs){
                            //处理callInst
                            if(MA instanceof MemoryDef){
                                if(((MemoryDef)MA).getMemoryInstruction() instanceof Instructions.CallInst){
                                    Instructions.CallInst CI=(Instructions.CallInst)((MemoryDef)MA).getMemoryInstruction();
                                    for(Value v:CI2Pointers.get(CI)){
                                        IncomingVal.put(v,MA);
                                    }
                                    continue;
                                }
                            }
                            IncomingVal.put(MA.getPointer(),MA);
                        }
                    }
                    continue;
                } else{
                    renameBlock(BB, IncomingVal);
                }
                renameSuccessorPhis(BB, IncomingVal);
                dfsRename(new RenamePassData(BB,Pred,IncomingVal),Visited);
            }
        }
    }

    static class RenameLoadPassData {
        public BasicBlock BB;
        public BasicBlock Pred;
        HashMap<Value,Instruction> Val;

        public RenameLoadPassData(BasicBlock BB, BasicBlock pred, HashMap<Value,Instruction> val) {
            this.BB = BB;
            Pred = pred;
            Val = val;
        }
    }

    public void RenameLoadPass(BasicBlock BB,BasicBlock Pred,HashMap<Value,Instruction> IncomingVals
            ,Stack<RenameLoadPassData> Worklist,Set<BasicBlock> Visited){
        while(true){
            /*
             * 如果块中有 φ 指令，则遍历所有先前添加的 φ（注意程序中原来可能也有 φ，这里要和原来的 φ 区分开来）：
             * 假设某个前驱到当前基本块有 NumEdges 条边，则为 φ 指令添加 NumEdges 个来源，值为 IncomingVals[L]，同时设置 IncomingVals[L] = Phi
             */
            if (BlockToMemDefList.get(BB)!=null && BlockToMemDefList.get(BB).getFirst() instanceof MemoryPhi) {
                MemoryPhi APN = (MemoryPhi) BlockToMemDefList.get(BB).getFirst();
                if(PhiToLoad.containsKey(APN)){
                    int loadIdx=PhiToLoad.get(APN);
                    int NewPHINumOperands = APN.getNumOperands();
                    int NumEdges = 0;
                    for (BasicBlock suc :
                            Pred.getSuccessors()) {
                        if (suc == BB) {
                            NumEdges++;
                        }
                    }
                    assert NumEdges >= 1;
                    Iterator<MemoryAccess> PNI = BlockToMemDefList.get(BB).iterator();
                    Instruction I;
                    do {
                        // 则为 φ 指令添加 NumEdges 个来源
                        for (int i = 0; i != NumEdges; ++i)
                            APN.addIncoming(IncomingVals.get(Loads.get(loadIdx)), Pred);
                        // 设置 IncomingVals[L] = Phi
                        IncomingVals.put(Loads.get(loadIdx), APN);
                        // 处理下一个phi
                        I = PNI.next();
                        if (!(I instanceof MemoryPhi)) {
                            break;
                        }
                        APN = (MemoryPhi) (I);
                    } while (APN.getNumOperands() == NewPHINumOperands);
                }
            }

            if (!Visited.add(BB)) {
                return;
            }
            renameLoadBlock(BB,IncomingVals);

            if (BB.getSuccessorsNum() == 0)
                return;

            BasicBlock I = BB.getSuccessor(0);
            Set<BasicBlock> VisitedSuccs = new HashSet<>();

            VisitedSuccs.add(I);
            Pred = BB;
            BasicBlock oldBB=BB;
            BB = I;

            for (int i = 1; i < oldBB.getSuccessorsNum(); i++) {
                I = oldBB.getSuccessor(i);
                if (VisitedSuccs.add(I))
                    Worklist.add(new RenameLoadPassData(I, Pred, new HashMap<>(IncomingVals)));
            }
        }
    }

    private void renameLoadBlock(BasicBlock BB, HashMap<Value,Instruction> IncomingVal) {
        LinkedList<MemoryAccess> accList = BlockToMemAccList.get(BB);
        if (accList != null && !accList.isEmpty()) {
            for (MemoryAccess MA : accList) {
                if (MA instanceof MemoryDefOrUse) {
                    MemoryDefOrUse MUD = (MemoryDefOrUse) MA;
                    if (MUD instanceof MemoryUse) {
                        MemoryUse MU=(MemoryUse)MUD;
                        IncomingVal.put(MU.getMemoryInstruction(),MU.getMemoryInstruction());
                    }else{
                        //TODO：也许需要标识store和call?
                        //Load遇到了Store或Call，作用范围失效
                        MemoryDef MD=(MemoryDef)MUD;
                        if(MD.getMemoryInstruction() instanceof Instructions.StoreInst){
                            IncomingVal.forEach((key,value)->{
                                if(!value.equals(LiveOnEntry)&&getMemoryAccess((Instruction)(key)).getPointer().equals(MD.getPointer())){
                                    MD.addOperand(value);
                                }
                            });
                        }else{
                            Instructions.CallInst CI= (Instructions.CallInst) MD.getMemoryInstruction();
                            IncomingVal.forEach((key,value)->{
                                if(!value.equals(LiveOnEntry)&&CI2Pointers.get(CI).contains(getMemoryAccess((Instruction)(key)).getPointer())){
                                    MD.addOperand(value);
                                }
                            });
                        }
                    }
                } else if(MA instanceof MemoryPhi){
                    if(PhiToLoad.containsKey(MA)){
                        IncomingVal.put(Loads.get(PhiToLoad.get(MA)),MA);
                    }
                }
            }
        }
    }

    private void renameBlock(BasicBlock BB, HashMap<Value,MemoryAccess> IncomingVal) {
        LinkedList<MemoryAccess> accList = BlockToMemAccList.get(BB);
        if (accList != null && !accList.isEmpty()) {
            for (MemoryAccess MA : accList) {
                if (MA instanceof MemoryDefOrUse) {
                    MemoryDefOrUse MUD = (MemoryDefOrUse) MA;
                    //call单独处理
                    if(MUD.getMemoryInstruction() instanceof Instructions.CallInst) {
                        Instructions.CallInst CI=(Instructions.CallInst)MUD.getMemoryInstruction();
                        MemoryAccess newDef=LiveOnEntry;
                        for(Value v:CI2Pointers.get(CI)){
                            //找到call定义的pointer中版本最后的
                            if(IncomingVal.get(v).getID()>newDef.getID()) newDef=IncomingVal.get(v);
                            IncomingVal.put(v,MA);
                        }
                        if(MUD.getDefiningAccess()==null){
                            MUD.setDefiningAccess(newDef);
                        }
                        continue;
                    }
                    if (MUD.getDefiningAccess() == null) {
                        MUD.setDefiningAccess(IncomingVal.get(MUD.getPointer()));
                    }
                    if (MUD instanceof MemoryDef) {
                        IncomingVal.put(MUD.getPointer(),MA);
                    }
                } else {
                    IncomingVal.put(MA.getPointer(),MA);
                }
            }
        }
    }

    private void renameSuccessorPhis(BasicBlock BB, HashMap<Value,MemoryAccess> IncomingVal) {
        for (BasicBlock Succ : BB.getSuccessors()) {
            LinkedList<MemoryAccess> accList = BlockToMemAccList.get(Succ);
            if (accList == null || accList.isEmpty() || !(accList.getFirst() instanceof MemoryPhi))
                continue;
            for(MemoryAccess MA:accList){
                if(!(MA instanceof MemoryPhi)){
                    break;
                }
                MemoryPhi Phi = (MemoryPhi) MA;
                Phi.addIncoming(IncomingVal.get(Phi.getPointer()), BB);
            }
        }
    }

    private void renameArraySuccessorPhis(BasicBlock BB, HashMap<Value,HashMap<DimInfo,MemoryAccess>> IncomingVal) {
        for (BasicBlock Succ : BB.getSuccessors()) {
            LinkedList<MemoryAccess> accList = BlockToMemAccList.get(Succ);
            if (accList == null || accList.isEmpty() || !(accList.getFirst() instanceof MemoryPhi))
                continue;
            for(MemoryAccess MA:accList){
                if(!(MA instanceof MemoryPhi)){
                    break;
                }
                MemoryPhi Phi = (MemoryPhi) MA;
                Phi.addIncoming(IncomingVal.get(Phi.getPointer()).get(Phi.getDimInfo()), BB);
            }
        }
    }

    static class RenameArrayPassData {
        public BasicBlock BB;
        public BasicBlock Pred;
        HashMap<Value,HashMap<DimInfo,MemoryAccess>> Val;

        public RenameArrayPassData(BasicBlock BB, BasicBlock pred, HashMap<Value,HashMap<DimInfo,MemoryAccess>> val) {
            this.BB = BB;
            Pred = pred;
            Val = val;
        }
    }

    public void RenameArrayPass(DominatorTree DT, HashMap<Value,HashMap<DimInfo,MemoryAccess>> IncomingVal
            , Set<BasicBlock> Visited) {
        DominatorTree.TreeNode Root = DT.Root;
        boolean AlreadyVisited = !Visited.add(Root.BB);
        if (AlreadyVisited)
            return;
        renameArrayBlock(Root.BB, IncomingVal);
        renameArraySuccessorPhis(Root.BB, IncomingVal);
        dfsArrayRename(new RenameArrayPassData(Root.BB,null, IncomingVal),Visited);
    }

    public void dfsArrayRename(RenameArrayPassData RPD, Set<BasicBlock> Visited) {
        BasicBlock Pred = RPD.BB;
        if (Pred.getSuccessorsNum()!=0) {
            for (BasicBlock BB : Pred.getSuccessors()) {
                //注意复杂hash结构的复制问题
                HashMap<Value,HashMap<DimInfo,MemoryAccess>> IncomingVal = new HashMap<>();
                RPD.Val.forEach((pointer,hash)->{
                    IncomingVal.put(pointer,new HashMap<>(hash));
                });
                boolean AlreadyVisited = !Visited.add(BB);
                if (AlreadyVisited) {
                    LinkedList<MemoryAccess> BlockDefs=BlockToMemDefList.get(BB);
                    if (BlockDefs != null) {
                        for(MemoryAccess MA:BlockDefs){
                            //处理callInst
                            if(MA instanceof MemoryDef){
                                if(((MemoryDef)MA).getMemoryInstruction() instanceof Instructions.CallInst){
                                    Instructions.CallInst CI=(Instructions.CallInst)((MemoryDef)MA).getMemoryInstruction();
                                    //call就不考虑dimInfo了
                                    for(Value v:CI2Pointers.get(CI)){
                                        HashMap<DimInfo,MemoryAccess> ArrDimInfo=IncomingVal.get(v);
                                        ArrDimInfo.keySet().forEach(((dimInfo) -> {
                                            IncomingVal.get(v).put(dimInfo,MA);
                                        }));
                                        IncomingVal.put(v,ArrDimInfo);
                                    }
                                    continue;
                                }
                            }
                            IncomingVal.get(MA.getPointer()).put(MA.getDimInfo(),MA);
                        }
                    }
                    continue;
                } else{
                    renameArrayBlock(BB, IncomingVal);
                }
                renameArraySuccessorPhis(BB, IncomingVal);
                dfsArrayRename(new RenameArrayPassData(BB,Pred,IncomingVal),Visited);
            }
        }
    }

    private void renameArrayBlock(BasicBlock BB, HashMap<Value,HashMap<DimInfo,MemoryAccess>> IncomingVal) {
        LinkedList<MemoryAccess> accList = BlockToMemAccList.get(BB);
        if (accList != null && !accList.isEmpty()) {
            for (MemoryAccess MA : accList) {
                if (MA instanceof MemoryDefOrUse) {
                    MemoryDefOrUse MUD = (MemoryDefOrUse) MA;
                    //call单独处理
                    if(MUD.getMemoryInstruction() instanceof Instructions.CallInst) {
                        Instructions.CallInst CI=(Instructions.CallInst)MUD.getMemoryInstruction();
//                        MemoryAccess newDef=LiveOnEntry;
                        for(Value v:CI2Pointers.get(CI)){
                            //找到call定义的pointer中版本最后的
//                            if(IncomingVal.get(v).getID()>newDef.getID()) newDef=IncomingVal.get(v);
                            for(DimInfo dimInfo:Pointer2DimInfo.get(v)){
                                IncomingVal.get(v).put(dimInfo,MA);
                            }
                        }
                        if(MUD.getDefiningAccess()==null){
                            MUD.setDefiningAccess(LiveOnEntry);
                        }
                        continue;
                    }
                    if (MUD.getDefiningAccess() == null) {
                        if(IncomingVal.get(MUD.getPointer())==null){
                            System.out.println("err");
                        }
                        MUD.setDefiningAccess(IncomingVal.get(MUD.getPointer()).get(MUD.getDimInfo()));
                    }
                    //判断哪些dimInfo被当前的覆盖了
                    if (MUD instanceof MemoryDef) {
                        for(DimInfo dimInfo:Pointer2DimInfo.get(MUD.getPointer())){
                            if(dominateDimInfo(MUD.getDimInfo(),dimInfo))
                                IncomingVal.get(MUD.getPointer()).put(dimInfo,MUD);
                        }
                    }
                } else {
                    for(DimInfo dimInfo:Pointer2DimInfo.get(MA.getPointer())){
                        if(dominateDimInfo(MA.getDimInfo(),dimInfo))
                            IncomingVal.get(MA.getPointer()).put(dimInfo,MA);
                    }
                }
            }
        }
    }

    /**
     * a[2][i] dominate a[2][j]
     * a[1][i] !dominate a[2][j]
     * a[1][i][2] !dominate a[1][i][1]
     * 在fuzzyIdx之前都相同的话，就认为dominate
     */
    private boolean dominateDimInfo(DimInfo dim1,DimInfo dim2){
        if(dim1.equals(dim2)) return true;
        if(dim1.getNumOperands()!=dim2.getNumOperands()) return false;//TODO:保险起见这里还是true？
        int fuzzyIdx=dim1.getNumOperands();
        for(int i=0;i<dim1.getNumOperands();i++){
            if(!(dim1.getOperand(i) instanceof Constants.ConstantInt)
                    &&!dim1.getOperand(i).equals(dim2.getOperand(i))){
                fuzzyIdx=i;
            }
        }
        for(int i=0;i<fuzzyIdx;i++){
            if(!dim1.getOperand(i).equals(dim2.getOperand(i))){
                return false;
            }
        }
        return true;
    }

    public LinkedList<MemoryAccess> getOrAddAccessList(BasicBlock BB) {
        if (BlockToMemAccList.containsKey(BB)) return BlockToMemAccList.get(BB);
        LinkedList<MemoryAccess> memoryAccesses = new LinkedList<>();
        BlockToMemAccList.put(BB, memoryAccesses);
        return memoryAccesses;
    }

    public LinkedList<MemoryAccess> getOrAddDefList(BasicBlock BB) {
        if (BlockToMemDefList.containsKey(BB)) return BlockToMemDefList.get(BB);
        LinkedList<MemoryAccess> memoryDefs = new LinkedList<>();
        BlockToMemDefList.put(BB, memoryDefs);
        return memoryDefs;
    }

    /**
     * TODO:新建I的MemoryAccess，可能需要用到Alias Analysis中的信息判断是否改写、读取了内存？
     * 这里暂时只通过load是否为参数以及callee的side effect
     */
    public MemoryDefOrUse createNewAccess(Instruction I) {
        MemoryDefOrUse ret = null;
        switch (I.getOp()) {
            case Store, Call -> {
                if(I.getOp().equals(Instruction.Ops.Call)){
                    if((!((Function)I.getOperand(0)).hasSideEffect())){
                        return null;
                    }
                    Instructions.CallInst CI=(Instructions.CallInst)I;
                    CI2Pointers.put(CI,new ArrayList<>());
                    Pointer2Defs.keySet().forEach((pointer)->{
                        if(AliasAnalysis.callAlias(pointer,CI)){
                            CI2Pointers.get(CI).add(pointer);
                        }
                    });
                    if(CI2Pointers.get(CI).isEmpty()){
                        return null;
                    }
                    ret = new MemoryDef(I, null, ID++);
                    for(Value v:CI2Pointers.get(CI)){
                        ArrayList<BasicBlock> defs=Pointer2Defs.getOrDefault(v,new ArrayList<>());
                        defs.add(I.getParent());
                        Pointer2Defs.put(v,defs);
                        //call就不考虑dimInfo了要重构的有点多
                        HashMap<DimInfo,ArrayList<BasicBlock>> ArrDimInfo=Mem2Defs.getOrDefault(v,new HashMap<>());
                        Pointer2DimInfo.get(v).forEach(((dimInfo) -> {
                            ArrayList<BasicBlock> dimBlocks=ArrDimInfo.getOrDefault(dimInfo,new ArrayList<>());
                            dimBlocks.add(I.getParent());
                            ArrDimInfo.put(dimInfo,dimBlocks);
                        }));
                        Mem2Defs.put(v,ArrDimInfo);
                    }
                    break;
                }
                if(I.getOp().equals(Instruction.Ops.Store)){
                    if(AliasAnalysis.isParam(I.getOperand(1))){
                        return null;
                    }
                    ret = new MemoryDef(I, null, ID++);
                    Value ptr=AliasAnalysis.getPointerOrArgumentValue(I.getOperand(1));
                    if(ptr==null){// ptr形式的phi
                        ptr=I.getOperand(1);
                    }
                    ArrayList<BasicBlock> defs=Pointer2Defs.getOrDefault(ptr,new ArrayList<>());
                    defs.add(I.getParent());
                    Pointer2Defs.put(ptr,defs);
                    ret.setPointer(ptr);
                    //ArraySSA def
                    DimInfo dimInfo;
                    if(I.getOperand(1) instanceof Instructions.GetElementPtrInst){
                        dimInfo=((Instructions.GetElementPtrInst) I.getOperand(1)).getAndUpdateDimInfo();
                    }else{
                        dimInfo=nullDimInfo;
                    }
                    ret.setDimInfo(dimInfo);
                    MemAcc2Dim.put(ret,dimInfo);
                    //TODO:ptr形式的phi是否会破坏dimInfo?
                    HashMap<DimInfo,ArrayList<BasicBlock>> ArrDimInfo=Mem2Defs.getOrDefault(ptr,new HashMap<>());
                    ArrayList<BasicBlock> ArrDefs=ArrDimInfo.getOrDefault(dimInfo,new ArrayList<>());
                    ArrDefs.add(I.getParent());
                    ArrDimInfo.put(dimInfo,ArrDefs);
                    Mem2Defs.put(ptr,ArrDimInfo);
                    ArrayList<DimInfo> dims=Pointer2DimInfo.getOrDefault(ptr,new ArrayList<>());
                    dims.add(dimInfo);
                    Pointer2DimInfo.put(ptr,dims);
                }
            }
            case Load -> {
                if(AliasAnalysis.isParam(I.getOperand(0))){
                    return null;
                }
                ret = new MemoryUse(I, null);
                Value ptr=AliasAnalysis.getPointerOrArgumentValue(I.getOperand(0));
                if(ptr==null){
                    ptr=I.getOperand(0);
                }
                ArrayList<BasicBlock> defs=Pointer2Defs.getOrDefault(ptr,new ArrayList<>());
                Pointer2Defs.put(ptr,defs);
                Loads.add((Instructions.LoadInst) I);
                LoadLoopUp.put((Instructions.LoadInst) I,Loads.size()-1);
                ret.setPointer(ptr);
                //calculate dim info
                DimInfo dimInfo;
                if(I.getOperand(0) instanceof Instructions.GetElementPtrInst){
                    dimInfo=((Instructions.GetElementPtrInst) I.getOperand(0)).getAndUpdateDimInfo();
                }else{
                    dimInfo=nullDimInfo;
                }
                ret.setDimInfo(dimInfo);
                MemAcc2Dim.put(ret,dimInfo);
                ArrayList<DimInfo> dims=Pointer2DimInfo.getOrDefault(ptr,new ArrayList<>());
                dims.add(dimInfo);
                Pointer2DimInfo.put(ptr,dims);
            }
        }
        if (ret != null) {
            ValueToMemAcc.put(I, ret);
        }
        return ret;
    }

    public boolean isLiveOnEntry(MemoryAccess MA) {
        return MA == LiveOnEntry;
    }

    /**
     * 获得某指令对应的MemoryDef或MemoryUse
     */
    public MemoryDefOrUse getMemoryAccess(Instruction I) {
        return (MemoryDefOrUse) ValueToMemAcc.get(I);
    }

    /**
     * 获得某基本块开头的MemoryPhi
     */
    public MemoryPhi getMemoryAccess(BasicBlock BB,Value pointer) {
        if(PointerToPhi.get(pointer)==null) return null;
        return (MemoryPhi) PointerToPhi.get(pointer).get(BB);
    }

    public DominatorTree getDomTree() {
        return DT;
    }

    public Function getF() {
        return F;
    }

    public MemoryAccess getLiveOnEntry() {
        return LiveOnEntry;
    }

    /**
     * 输出MemorySSA
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(F).append("{\n");
        boolean init = true;
        for (BasicBlock BB : F.getBbList()) {
            if (!init) {
                sb.append("\n").append(BB).append("     ").append(BB.getComment() != null ? BB.getComment() : "").append("\n");
            } else {
                sb.append(BB).append("     ").append(BB.getComment() != null ? BB.getComment() : "").append("\n");
                init = false;
            }
            for(MemoryAccess m:BlockToMemDefList.getOrDefault(BB,new LinkedList<>())){
                if(!(m instanceof MemoryPhi)) break;
                sb.append("  ").append(m).append("\n");
            }
            for (Instruction I : BB.getInstList()) {
                if (getMemoryAccess(I) != null) {
                    sb.append("  ").append(getMemoryAccess(I)).append("\n");
                }
                sb.append("  ").append(I).append("     ").append(I.getComment() != null ? I.getComment() : "").append("\n");
            }
        }
        sb.append("}").append("\n");
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        testPass.initModule();
        ir.Module module=ir.Module.getInstance();
//        FileWriter fw=new FileWriter("src/pass/test/output.txt");
//        PrintWriter pw=new PrintWriter(fw);

//        PassManager.initialization();
//        PassManager.run(module);
//        pw.println(module.toLL());
//        pw.flush();
        new Mem2Reg().runOnModule(module);
        new SimplifyCFG(false).runOnModule(module);
        for(Function F:module.getFuncList()){
            if(!F.isDefined()) continue;
            MemorySSA MSSA=new MemorySSA(F,F.getDominatorTree());
            System.out.println(MSSA);
        }
    }
}
