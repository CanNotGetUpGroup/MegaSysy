package pass.passes;

import ir.*;
import ir.instructions.Instructions;
import org.antlr.v4.runtime.misc.Pair;
import pass.FunctionPass;
import pass.Pass;
import ir.instructions.Instructions.*;

import java.util.*;

public class Mem2Reg extends FunctionPass {
    public Mem2Reg() {
        super();
    }

    @Override
    public String getName() {
        return "Mem2Reg";
    }

    @Override
    public void runOnFunction(Function F) {
        DominatorTree DT=new DominatorTree(F);
        promoteMem2Reg(F,DT);
    }

    public static FunctionPass createMem2Reg(){
        return new Mem2Reg();
    }

    public static void promoteMem2Reg(Function F,DominatorTree DT){
        ArrayList<AllocaInst> allocaInsts=new ArrayList<>();
        BasicBlock BB=F.getEntryBB();

        while(true){
            allocaInsts.clear();
            for(Instruction I:BB.getInstList()){
                if(I instanceof AllocaInst){
                    AllocaInst AI=(AllocaInst)I;
                    if(isAllocaPromotable(AI)){
                        allocaInsts.add(AI);
                    }
                }
            }
            if(allocaInsts.isEmpty()){
                break;
            }
            promoteMemoryToRegister(allocaInsts,DT);
        }
    }

    public static void promoteMemoryToRegister(ArrayList<AllocaInst> allocaInsts,DominatorTree DT){
        if(allocaInsts.isEmpty()){
            return;
        }
        for(int i=0;i<allocaInsts.size();i++){
            AllocaInst AI=allocaInsts.get(i);

            //删除未使用的ALLOC
            if(AI.getUseList().isEmpty()){
                AI.remove();
                allocaInsts.remove(AI);
                continue;
            }

            analyzeAlloca(AI);

            //如果只有一个定义基本块（只有一个STORE语句，且只存在一个基本块中），
            //那么被这个定义基本块所支配的所有LOAD都要被替换为STORE语句中的那个右值。
            if(AI.definingBlocks.size()==1&&rewriteAlloca(AI,DT)){
                allocaInsts.remove(AI);
                continue;
            }

            //如果某个ALLOC出来的局部变量的读或者写都只存在一个基本块中，那么我们就
            //没必要去遍历所有的CFG的，因为这个store语句支配了这些LOAD，
            //所以可以用STORE的右值直接替换使用LOAD指令的那些值。
            if(AI.onlyUsedInOne&&promoteAlloca(AI,DT)){
                allocaInsts.remove(AI);
                continue;
            }

            Set<BasicBlock> LiveBB=new HashSet<>();
            computeLiveBB(AI,LiveBB);

        }
    }

    public static void computeLiveBB(AllocaInst AI,Set<BasicBlock> LiveBB){
        ArrayList<BasicBlock> copyUsing = new ArrayList<>(AI.usingBlocks);

        for(int i=0,e=copyUsing.size();i!=e;i++){
            BasicBlock basicBlock=copyUsing.get(i);
            if(!AI.definingBlocks.contains(basicBlock)){
                continue;
            }
            for(Instruction I:basicBlock.getInstList()){
                if(I instanceof StoreInst){
                    StoreInst SI=(StoreInst)I;
                    if(SI.getOperand(1)!=AI){
                        continue;
                    }
                    copyUsing.set(i,copyUsing.get(e-1));
                    copyUsing.remove(e-1);
                    --i;--e;
                    break;
                }
                if(I instanceof LoadInst){
                    LoadInst LI=(LoadInst)I;
                    if(LI.getOperand(0)==AI){
                        break;
                    }
                }
            }
        }

        while(!copyUsing.isEmpty()){
            BasicBlock basicBlock=copyUsing.get(copyUsing.size()-1);
            copyUsing.remove(copyUsing.size()-1);
            if(LiveBB.contains(basicBlock)){
                continue;
            }
            LiveBB.add(basicBlock);

            for(BasicBlock pre:basicBlock.predecessors()){
                if(AI.definingBlocks.contains(pre)){
                    continue;
                }
                copyUsing.add(pre);
            }
        }
    }

    public static boolean promoteAlloca(AllocaInst AI, DominatorTree DT){
        ArrayList<Pair<Integer,StoreInst>> storeIndex=new ArrayList<>();

        for(Use use:AI.getUseList()){
            User U=use.getU();
            if(U instanceof StoreInst){
                StoreInst SI=(StoreInst)U;
                storeIndex.add(new Pair<>(SI.getInstNode().index(),SI));
            }
        }
        storeIndex.sort(Comparator.comparingInt(o -> o.a));

        for(Use use:AI.getUseList()){
            User U=use.getU();
            if(!(U instanceof LoadInst)) {
                continue;
            }
            LoadInst LI=(LoadInst)U;
            int Idx=LI.getInstNode().index();
            //二分查找最接近Idx的storeInst
            int l=0,r=storeIndex.size()-1,mid;
            while(l<=r){
                mid=(l+r)/2;
                if(storeIndex.get(mid).a<=Idx){
                    l=mid+1;
                }else{
                    r=mid-1;
                }
            }
            if(r==-1){
                if(storeIndex.isEmpty()){//不存在store，load得到undef值
                    LI.replaceAllUsesWith(Constants.UndefValue.get(LI.getType()));
                }else{//load前面没有store但是后面有
                    return false;
                }
            }else{
                Value val=storeIndex.get(r).b.getOperand(0);
                if(LI.equals(val)){
                    val = Constants.UndefValue.get(LI.getType());
                }
                LI.replaceAllUsesWith(val);
            }
            LI.remove();

        }
        //移除storeInst
        while(!AI.getUseList().isEmpty()){
            StoreInst SI=(StoreInst)AI.userBack();
            SI.remove();
        }
        AI.remove();
        return true;
    }

    public static boolean rewriteAlloca(AllocaInst AI, DominatorTree DT){
        StoreInst SI=AI.onlyStore;
        BasicBlock SB=SI.getParent();
        int storeIdx=-1;

        AI.usingBlocks.clear();
        for(Use use:AI.getUseList()){
            User U=use.getU();
            Instruction I=(Instruction)U;
            if(I.equals(SI)){
                continue;
            }
            LoadInst LI=(LoadInst)I;
            //store的是instruction，需要判断支配关系；若不是instruction，则不用检查
            if(SI.getOperand(0) instanceof Instruction){
                //如果load和store在同一基本块，则需要比较二者先后顺序
                if(LI.getParent().equals(SB)){
                    if(storeIdx==-1){
                        storeIdx=SB.getInstList().indexOf(SI);
                    }
                    //store在load之后，load不受store支配
                    if(storeIdx>SB.getInstList().indexOf(LI)){
                        AI.usingBlocks.add(SB);
                        continue;
                    }
                }
                //TODO：在不同基本块中，需要检查store的基本块是否支配load基本块
                else if(!DT.dominates(SB,LI.getParent())){
                    AI.usingBlocks.add(LI.getParent());
                    continue;
                }
            }
            Value val=SI.getOperand(0);
            // TODO:未定义情况（SI.getOperand(0)==LI）
            if(LI.equals(val)){
                val = Constants.UndefValue.get(LI.getType());
            }
            LI.replaceAllUsesWith(val);
            LI.remove();
        }
        if(!AI.usingBlocks.isEmpty()){
            return false;
        }
        AI.onlyStore.remove();
        AI.remove();
        return true;
    }

    /**
     * 对于步骤2中选出来的可promotable的那些局部变量，扫描一次当前函数，
     * 看下哪些基本块在使用这些ALLOC，哪些基本块定义了这个ALLOC
     * （这里的使用是指LOAD，定义是通过STORE语句）
     */
    public static void analyzeAlloca(AllocaInst AI){
        AI.resetAnalyzeInfo();
        for(Use use:AI.getUseList()){
            Instruction I=(Instruction) use.getU();
            if(I instanceof StoreInst){
                StoreInst SI=(StoreInst)I;
                AI.definingBlocks.add(SI.getParent());
                AI.onlyStore=SI;
            }else {
                LoadInst LI=(LoadInst)I;
                AI.usingBlocks.add(LI.getParent());
            }
            if(AI.onlyUsedInOne){
                if(AI.onlyBlock==null){
                    AI.onlyBlock=I.getParent();
                }else if(AI.onlyBlock!=I.getParent()){
                    AI.onlyUsedInOne=false;
                }
            }
        }
    }

    /**
     * 如果满足下面的2个条件，就是可promotable：
     * 1.这个ALLOC出来的临时变量不在任何volatile指定中使用
     * 2.这个变量是直接通过LOAD或者STORE进行访问的，比如，不会被取址。
     */
    public static boolean isAllocaPromotable(AllocaInst AI){
        for(Use use:AI.getUseList()){
            User U=use.getU();
            if(U instanceof LoadInst){

            }else if(U instanceof StoreInst){
                StoreInst SI=(StoreInst)U;
                if(SI.getOperand(0).equals(AI)){
                    return false;
                }
            }else if(U instanceof GetElementPtrInst){
                GetElementPtrInst GEP=(GetElementPtrInst)U;
                if(!GEP.allIndicesZero()){
                    return false;
                }
            }else{
                return false;
            }
        }
        return true;
    }
}
