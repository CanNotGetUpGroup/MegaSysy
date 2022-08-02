package analysis;

import ir.BasicBlock;
import ir.Function;
import ir.Instruction;
import ir.Value;
import analysis.MemoryAccess;
import analysis.MemoryAccess.*;
import pass.passes.Mem2Reg;

import java.util.*;

public class MemorySSA {
    private final HashMap<Value, MemoryAccess> ValueToMemAcc;
    private final HashMap<BasicBlock, ArrayList<MemoryAccess>> BlockToMemAccList;//基本块中储存的MemoryAccess
    private final HashMap<BasicBlock, ArrayList<MemoryDef>> BlockToMemDefList;//基本块中储存的MemoryDef
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
        return true;
    }

    public void buildMemorySSA() {
        BasicBlock StartingPoint = F.getEntryBB();
        Set<BasicBlock> DefiningBlocks = new HashSet<>();
        for (BasicBlock BB : F.getBbList()) {
            boolean InsertIntoDef = false;
            ArrayList<MemoryAccess> Accesses = null;
            ArrayList<MemoryDef> Defs = null;
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
                    Defs.add((MemoryDef) MUD);
                }
            }
            if (InsertIntoDef)
                DefiningBlocks.add(BB);
        }
        placePHINodes(DefiningBlocks);

        Set<BasicBlock> Visited=new HashSet<>();
        RenamePass(DT, LiveOnEntry, Visited);

        //TODO：将无法到达的基本块设为LiveOnEntry
//        for (BasicBlock BB :F.getBbList()){
//            if (!Visited.contains(BB))
//                markUnreachableAsLiveOnEntry(BB);
//        }
    }

    /**
     * TODO:计算支配树边界，找到插入MemPhi指令的位置，参考mem2reg的IDFCalculate方法
     */
    public void placePHINodes(Set<BasicBlock> DefiningBlocks){

    }

    /**
     * TODO：添加MemPHI的IncomingVal，参考mem2reg的RenamePass
     */
    public void RenamePass(DominatorTree DT,MemoryAccess IncomingVal, Set<BasicBlock> Visited){
        DominatorTree.TreeNode Root=DT.Root;

    }

    public ArrayList<MemoryAccess> getOrAddAccessList(BasicBlock BB) {
        if (BlockToMemAccList.containsKey(BB)) return BlockToMemAccList.get(BB);
        ArrayList<MemoryAccess> memoryAccesses = new ArrayList<>();
        BlockToMemAccList.put(BB, memoryAccesses);
        return memoryAccesses;
    }

    public ArrayList<MemoryDef> getOrAddDefList(BasicBlock BB) {
        if (BlockToMemDefList.containsKey(BB)) return BlockToMemDefList.get(BB);
        ArrayList<MemoryDef> memoryDefs = new ArrayList<>();
        BlockToMemDefList.put(BB, memoryDefs);
        return memoryDefs;
    }

    /**
     * TODO:新建I的MemoryAccess，可能需要用到Alias Analysis中的信息判断是否改写、读取了内存？
     * 这里暂时只通过指令类型判断
     */
    public MemoryDefOrUse createNewAccess(Instruction I) {
        MemoryDefOrUse ret=null;
        switch (I.getOp()){
            case Store,Call->{
                ret=new MemoryDef(I,null,ID++);
            }
            case Load -> {
                ret=new MemoryUse(I,null,ID++);
            }
        }
        if(ret!=null){
            ValueToMemAcc.put(I,ret);
        }
        return ret;
    }

    public boolean isLiveOnEntry(MemoryAccess MA) {
        return MA == LiveOnEntry;
    }

    public MemoryDefOrUse getMemoryAccess(Instruction I) {
        return (MemoryDefOrUse) ValueToMemAcc.get(I);
    }

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
}
