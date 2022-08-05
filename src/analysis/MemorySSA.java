package analysis;

import ir.BasicBlock;
import ir.Function;
import ir.Instruction;
import ir.Value;
import analysis.MemoryAccess.*;
import pass.PassManager;
import pass.passes.Mem2Reg;
import pass.test.testPass;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class MemorySSA {
    private final HashMap<Value, MemoryAccess> ValueToMemAcc;
    private final HashMap<BasicBlock, LinkedList<MemoryAccess>> BlockToMemAccList;//基本块中储存的MemoryAccess
    private final HashMap<BasicBlock, LinkedList<MemoryAccess>> BlockToMemDefList;//基本块中储存的MemoryDef和MemoryPhi
    private final MemoryAccess LiveOnEntry;
    private int ID;
    private final Function F;
    private final DominatorTree DT;

    public MemorySSA(Function F, DominatorTree DT) {
        this.F = F;
        this.DT = DT;
        ValueToMemAcc = new HashMap<>();
        BlockToMemAccList = new HashMap<>();
        BlockToMemDefList = new HashMap<>();
        LiveOnEntry = new MemoryAccess(Instruction.Ops.MemDef, F.getEntryBB());
        ID=1;
        buildMemorySSA();
    }

    /**
     * 判断dominator是否支配dominated，若在不同基本块，则通过Dominator Tree判断，
     * 若在同一基本块，则调用localDominates()
     */
    public boolean dominates(MemoryAccess dominator, MemoryAccess dominated) {
        if (dominator == dominated) return true;
        if (isLiveOnEntry(dominated)) return false;
        if (dominator.getBB() != dominated.getBB()) {
            return DT.dominates(dominator.getBB(), dominated.getBB());
        }
        return localDominates(dominator, dominated);
    }

    //TODO:同一基本块支配判断
    public boolean localDominates(MemoryAccess dominator, MemoryAccess dominated) {
        if(dominator==dominated) return true;
        if(isLiveOnEntry(dominated)) return false;
        if(isLiveOnEntry(dominator)) return true;
        //否则比较二者顺序，在前面的支配后面的
        if(dominator instanceof MemoryPhi) return true;

        return true;
    }

    public void buildMemorySSA() {
        ArrayList<BasicBlock> DefiningBlocks = new ArrayList<>();
        //先生成MemoryDef和MemoryUse，但不为他们指定definingAccess
        for (BasicBlock BB : F.getBbList()) {
            boolean InsertIntoDef = false;
            LinkedList<MemoryAccess> Accesses = null;
            LinkedList<MemoryAccess> Defs = null;
            for (Instruction I : BB.getInstList()) {
                MemoryDefOrUse MUD = createNewAccess(I);
                if (MUD == null)
                    continue;
                if (Accesses == null)
                    Accesses = getOrAddAccessList(BB);
                Accesses.add(MUD);
                if ((MUD instanceof MemoryDef)) {
                    InsertIntoDef = true;
                    if (Defs == null)
                        Defs = getOrAddDefList(BB);
                    Defs.add(MUD);
                }
            }
            if (InsertIntoDef)
                DefiningBlocks.add(BB);
        }
        ArrayList<BasicBlock> PHIBasicBlocks = new ArrayList<>();
        placePHINodes(DT, DefiningBlocks, PHIBasicBlocks);

        //插入phi
        for (BasicBlock BB : PHIBasicBlocks) {
            MemoryPhi memPhi = new MemoryPhi(BB, ID++);
            getOrAddAccessList(BB).addFirst(memPhi);
            getOrAddDefList(BB).addFirst(memPhi);
            ValueToMemAcc.put(BB, memPhi);
        }

        Set<BasicBlock> Visited = new HashSet<>();
        RenamePass(DT, LiveOnEntry, Visited);

        //TODO：将无法到达的基本块设为LiveOnEntry
    }

    /**
     * 计算支配树边界，找到插入MemPhi指令的位置，参考mem2reg的IDFCalculate方法
     */
    public void placePHINodes(DominatorTree DT, ArrayList<BasicBlock> DefiningBlocks, ArrayList<BasicBlock> IDFBlocks) {
        Mem2Reg.IDFCalculate(DT, DefiningBlocks, null, IDFBlocks);
    }

    static class RenamePassData {
        public DominatorTree.TreeNode treeNode;
        MemoryAccess Val;

        public RenamePassData(DominatorTree.TreeNode BB, MemoryAccess val) {
            this.treeNode = BB;
            Val = val;
        }
    }

    /**
     * 添加MemPHI的IncomingVal，参考mem2reg的RenamePass
     */
    public void RenamePass(DominatorTree DT, MemoryAccess IncomingVal, Set<BasicBlock> Visited) {
        DominatorTree.TreeNode Root = DT.Root;
        boolean AlreadyVisited = !Visited.add(Root.BB);
        if (AlreadyVisited)
            return;
        IncomingVal = renameBlock(Root.BB, IncomingVal);
        renameSuccessorPhis(Root.BB, IncomingVal);
        dfsRename(new RenamePassData(Root, IncomingVal),Visited);
    }

    public void dfsRename(RenamePassData RPD, Set<BasicBlock> Visited) {
        DominatorTree.TreeNode Node = RPD.treeNode;
        MemoryAccess IncomingVal = RPD.Val;
        if (!Node.Children.isEmpty()) {
            for (DominatorTree.TreeNode Child : Node.Children) {
                BasicBlock BB = Child.BB;
                boolean AlreadyVisited = !Visited.add(BB);
                if (AlreadyVisited) {
                    LinkedList<MemoryAccess> BlockDefs=BlockToMemDefList.get(BB);
                    if (BlockDefs != null) {
                        IncomingVal =BlockDefs.getLast();
                    }
                } else{
                    IncomingVal = renameBlock(BB, IncomingVal);
                }
                renameSuccessorPhis(BB, IncomingVal);
                dfsRename(new RenamePassData(Child,IncomingVal),Visited);
            }
        }
    }

    private MemoryAccess renameBlock(BasicBlock BB, MemoryAccess IncomingVal) {
        LinkedList<MemoryAccess> accList = BlockToMemAccList.get(BB);
        if (accList != null && !accList.isEmpty()) {
            for (MemoryAccess MA : accList) {
                if (MA instanceof MemoryDefOrUse) {
                    MemoryDefOrUse MUD = (MemoryDefOrUse) MA;
                    if (MUD.getDefiningAccess() == null) {
                        MUD.setDefiningAccess(IncomingVal);
                    }
                    if (MUD instanceof MemoryDef) {
                        IncomingVal = MA;
                    }
                } else {
                    IncomingVal = MA;
                }
            }
        }
        return IncomingVal;
    }

    private void renameSuccessorPhis(BasicBlock BB, MemoryAccess IncomingVal) {
        for (BasicBlock Succ : BB.getSuccessors()) {
            LinkedList<MemoryAccess> accList = BlockToMemAccList.get(Succ);
            if (accList == null || accList.isEmpty() || !(accList.getFirst() instanceof MemoryPhi))
                continue;
            MemoryPhi Phi = (MemoryPhi) accList.getFirst();
            Phi.addIncoming(IncomingVal, BB);
        }
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
     * 这里暂时只通过指令类型判断
     */
    public MemoryDefOrUse createNewAccess(Instruction I) {
        MemoryDefOrUse ret = null;
        switch (I.getOp()) {
            case Store, Call -> {
                ret = new MemoryDef(I, null, ID++);
            }
            case Load -> {
                ret = new MemoryUse(I, null);
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
    public MemoryPhi getMemoryAccess(BasicBlock BB) {
        return (MemoryPhi) ValueToMemAcc.get(BB);
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
            if (getMemoryAccess(BB) != null) {
                sb.append("  ").append(getMemoryAccess(BB)).append("\n");
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
        for(Function F:module.getFuncList()){
            if(!F.isDefined()) continue;
            MemorySSA MSSA=new MemorySSA(F,F.getDominatorTree());
            System.out.println(MSSA);
        }
    }
}
