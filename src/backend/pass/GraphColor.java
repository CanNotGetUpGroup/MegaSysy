package backend.pass;

import backend.machineCode.Instruction.Move;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.MCRegister;
import backend.machineCode.Operand.Register;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

// ref "Iterated Register Coalescing" by LAL GEORGE and ANDREW W. APPEL
public class GraphColor {
    private ArrayList<MachineFunction> funcList;

    public GraphColor(ArrayList<MachineFunction> funcList) {
        this.funcList = funcList;
    }

    HashSet<Register> simplifyWorklist; // list of low-degree non-move-related nodes.
    HashSet<Register> freezeWorklist; // low-degree move-related nodes.
    HashSet<Register> spillWorklist; // high-degree nodes.
    HashSet<Register> spilledNodes; // nodes marked for spilling during this round; initially empty.
    HashSet<Register> coalescedNodes; // registers that have been coalesced; when the move u:=v is coalesced, one of u or v is added to this set, and the other is put back on some worklist.
    HashSet<Register> coloredNodes; // nodes successfully colored.
    Deque<Register> selectStack; // stack containing temporaries removed from the graph.

    HashSet<Move> coalescedMoves; // moves that have been coalesced.
    HashSet<Move> constrainedMoves;// moves whose source and target interfere.
    HashSet<Move> frozenMoves;// moves that will no longer be considered for coalescing.
    HashSet<Move> worklistMoves; // moves enabled for possible coalescing.
    HashSet<Move> activeMoves; //moves not yet ready for coalescing.

    HashSet<Map.Entry<Register, Register>> adjSet; // the set of interference edges (u, v) in the graph.
    HashMap<Register, Set<Register>> adjList; // the set of interference edges (u, v) in the graph.
    Map<Register, Integer> degree; // an array containing the current degree of each node. Precolored nodes are initialized with a degree of ∞,

    Map<Register, Set<Move>> moveList; // a mapping from node to the list of moves it is associated with.
    Map<Register, Register> alias; // when a move (u, v) has been coalesced, and v put in coalescedNodes, then alias(v) = u.

    Map<MachineBasicBlock, LiveInfo> liveInfoMap;

    class LiveInfo {
        Set<Register> liveIn;
        Set<Register> liveOut;
        Set<Register> liveDef;
        Set<Register> liveUse;

        public LiveInfo() {
            liveIn = new HashSet<>();
            liveOut = new HashSet<>();
            liveDef = new HashSet<>();
            liveUse = new HashSet<>();
        }
    }

    // 活跃变量分析
    void LivenessAnalysis(MachineFunction func) {
        // find all use and def in basic blocks
        liveInfoMap = new HashMap<>();
        for (var bb : func.getBbList()) {
            var curLiveInfo = new LiveInfo();
            liveInfoMap.put(bb, curLiveInfo);
            for (var inst : bb.getInstList()) {
                var def = inst.getDef();
                var uses = inst.getUse();
                if (!curLiveInfo.liveUse.contains(def)) {
                    curLiveInfo.liveDef.add(def);
                }
                if (!curLiveInfo.liveDef.contains(uses)) {
                    curLiveInfo.liveUse.addAll(uses);
                }
            }
            curLiveInfo.liveIn.addAll(curLiveInfo.liveUse);
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (var bb : func.getBbList()) {
                var curLiveout = new HashSet<Register>();
                var curLiveInfo = liveInfoMap.get(bb);
                // out[B] = ∪ B的所有后继S in[S]
                for (var succ : bb.getSuccessors()) {
                    curLiveout.addAll(liveInfoMap.get(succ).liveIn);
                }


                // in[B] = use[B] ∪(out[B] – def[B])
                var curLivein = new HashSet<>(curLiveInfo.liveOut);
                curLivein.removeAll(curLiveInfo.liveDef);
                curLivein.addAll(curLiveInfo.liveUse);

                if (!curLiveInfo.liveIn.equals(curLivein)) {
                    curLiveInfo.liveIn = curLivein;
                    changed = true;
                }
                if (!curLiveout.equals(curLiveInfo.liveOut)) {
                    curLiveInfo.liveOut = curLiveout;
                    changed = true;
                }
            }
        }
    }

    void addEdge(Register u, Register v) {
        if (u.equals(v)) return;
        if (adjSet.contains(new AbstractMap.SimpleEntry<>(u, v)))
            return;
        adjSet.add(new AbstractMap.SimpleEntry<>(u, v));
        adjSet.add(new AbstractMap.SimpleEntry<>(v, u));

        if (!(u instanceof MCRegister)) {
            adjList.putIfAbsent(u, new HashSet<>());
            var set = adjList.get(u);
            set.add(v);
            int deg = degree.getOrDefault(u, 0) + 1;
            degree.put(u, deg);
        }
        if (!(v instanceof MCRegister)) {
            adjList.putIfAbsent(u, new HashSet<>());
            var set = adjList.get(v);
            set.add(u);
            int deg = degree.getOrDefault(v, 0) + 1;
            degree.put(v, deg);
        }
    }

    void build(MachineFunction func) {
        for (var bb : func.getBbList()) {
            var liveInfo = liveInfoMap.get(bb);
            var live = liveInfo.liveOut;
            var instList = bb.getInstList();
            for (var instNode = instList.getLast();
                 instNode != instList.getHead(); instNode = instNode.getPrev()) {
                var inst = instNode.getVal();
                if (inst instanceof Move) {
                    inst.getUse().forEach(live::remove);
                    for (var i : inst.getUse()) {
                        moveList.putIfAbsent(i, new HashSet<>());
                        moveList.get(i).add((Move) inst);
                    }
                    moveList.putIfAbsent(inst.getDef(), new HashSet<>());
                    moveList.get(inst.getDef()).add((Move) inst);

                    worklistMoves.add((Move) inst);
                }
                var def = inst.getDef();
                live.add(def);
                for (var l : live) {
                    addEdge(l, def);
                }
                // live := use(I) ∪ (live\def(I))
                live.remove(def);
                live.addAll(inst.getUse());
            }
        }
    }

    //function Adjacent(n)
    //adjList[n] \ (selectStack ∪ coalescedNodes)
    Set<Register> getAdjacent(Register reg) {
        return adjList.get(reg).stream()
                .filter(n -> !selectStack.contains(n))
                .filter(n -> !coalescedNodes.contains(n))
                .collect(Collectors.toSet());
    }

    // function NodeMoves (n)
    //moveList[n] ∩ (activeMoves ∪ worklistMoves)
    Set<Move> getNodeMoves(Register reg) {
        return moveList.get(reg).stream()
                .filter(n -> activeMoves.contains(n) || worklistMoves.contains(n))
                .collect(Collectors.toSet());
    }

    //function MoveRelated(n) NodeMoves(n) ̸= {}
    boolean isMoveRelated(Register reg) {
        return !getNodeMoves(reg).isEmpty();
    }

    // procedure MkWorklist()
    // forall n ∈ initial
    //initial := initial \ {n} if degree[n] ≥ K then
    //spillWorklist := spillWorklist ∪ {n} else if MoveRelated(n) then
    //freezeWorklist := freezeWorklist ∪ {n} else
    //simplifyWorklist := simplifyWorklist ∪ {n}

    void makeWorkList(Set<Register> initial) {
        for (var reg : initial) {
            if (degree.getOrDefault(reg, 0) >= MCRegister.maxRegNum(Register.Content.Int)) {
                // spillWorklist := spillWorklist ∪ {n}
                spillWorklist.add(reg);
            } else if (isMoveRelated(reg)) {
                freezeWorklist.add(reg);
            } else {
                simplifyWorklist.add(reg);
            }
        }
    }

    // procedure Simplify()
    //let n ∈ simplifyWorklist
    //simplifyWorklist := simplifyWorklist \ {n} push(n, selectStack)
    //forall m ∈ Adjacent(n)
    //DecrementDegree(m)
    void Simplify() {
        var reg = simplifyWorklist.iterator().next();
        simplifyWorklist.remove(reg);
        selectStack.push(reg);
        for (var m : getAdjacent(reg)) {
            DecrementDegree(m);
        }
    }


    //    procedure DecrementDegree(m)
    //    let d = degree[m]
//    degree[m] := d-1 if d=K then
//    EnableMoves({m} ∪ Adjacent(m)) spillWorklist := spillWorklist \ {m} if MoveRelated(m) then
//    freezeWorklist := freezeWorklist ∪ {m} else
//    simplifyWorklist := simplifyWorklist ∪ {m}
    void DecrementDegree(Register reg) {
        var d = degree.get(reg);
        degree.put(reg, d - 1);
        if (d == MCRegister.maxRegNum(Register.Content.Int)) {
            var set = new HashSet<Register>();
            set.add(reg);
            set.addAll(getAdjacent(reg));
            enableMoves(set);
            spillWorklist.remove(reg);
            if (isMoveRelated(reg))
                freezeWorklist.add(reg);
            else
                simplifyWorklist.add(reg);
        }
    }

    // procedure EnableMoves(nodes) forall n ∈ nodes
    //forall m ∈ NodeMoves(n)
    //if m ∈ activeMoves then
    //activeMoves := activeMoves \ {m}
    // worklistMoves := worklistMoves ∪ {m}
    void enableMoves(Collection<Register> regs) {
        for (var reg : regs) {
            for (var m : getNodeMoves(reg)) {
                if (activeMoves.contains(m)) {
                    activeMoves.remove(m);
                    worklistMoves.add(m);
                }
            }
        }
    }

    void Coalesce() {
        var m = worklistMoves.iterator().next();
        var x = getAlias(m.getDest());
        var y = getAlias((Register) m.getOp2());
        Register u, v;
        if (y.isPrecolored()) {
            u = y;
            v = x;
        } else {
            u = x;
            v = y;
        }
        worklistMoves.remove(m);
        if (u == v) {
            coalescedMoves.add(m);
            addWorkList(u);
            return;
        } else if (v.isPrecolored() || adjSet.contains(new AbstractMap.SimpleEntry<>(u, v))) {
            coalescedMoves.add(m);
            addWorkList(u);
            addWorkList(v);
            return;
        }
        var set = new HashSet<Register>();
        set.addAll(getAdjacent(u));
        set.addAll(getAdjacent(v));
        if (u.isPrecolored() && getAdjacent(v).stream().map(t -> OK(t, u)).reduce(true, (a, b) -> a && b)
                || !u.isPrecolored() && conservative(set)) {
            coalescedMoves.add(m);
            combine(u, v);
            addWorkList(u);
        } else
            activeMoves.add(m);
    }

    void addWorkList(Register reg) {
        if (!reg.isPrecolored() && !isMoveRelated(reg) && degree.get(reg) < MCRegister.maxRegNum(Register.Content.Int)) {
            freezeWorklist.remove(reg);
            simplifyWorklist.add(reg);
        }
    }

    // function OK(t,r)
    //degree[t] < K ∨ t ∈ precolored ∨ (t, r) ∈ adjSet
    boolean OK(Register t, Register r) {
        return degree.get(t) < MCRegister.maxRegNum(Register.Content.Int)
                || t.isPrecolored()
                || adjSet.contains(new AbstractMap.SimpleEntry<>(t, r));
    }

    boolean conservative(Collection<Register> nodes) {
        int k = 0;
        for (var n : nodes) {
            if (degree.get(n) > MCRegister.maxRegNum(Register.Content.Int))
                k++;
        }
        return k < MCRegister.maxRegNum(Register.Content.Int);
    }

    Register getAlias(Register reg) {
        if (coalescedNodes.contains(reg)) {
            return getAlias(alias.get(reg));
        }
        return reg;
    }

    void combine(Register u, Register v) {
        if(freezeWorklist.contains(v)){
            freezeWorklist.remove(v);
        } else {
            spillWorklist.remove(v);
        }
        coloredNodes.add(v);
        alias.put(v, u);
        moveList.get(u).addAll(moveList.get(v));
        for(var t : getAdjacent(v)){
            addEdge(t, u);
            DecrementDegree(t);
        }
        if(degree.get(u) >= MCRegister.maxRegNum(Register.Content.Int) && freezeWorklist.contains(u)){
            freezeWorklist.remove(u);
            spillWorklist.add(u);
        }
    }

    void freeze() {
        var u = freezeWorklist.iterator().next();
        freezeWorklist.remove(u);
        simplifyWorklist.add(u);
        FreezeMoves(u);
    }

    void FreezeMoves(Register u){

    }


    public void run() {
        for (var f : funcList) {

        }
    }

    public static void main(String[] args) {
        HashSet<Map.Entry<String, Integer>> adjSet = new HashSet<>();
        adjSet.add(new AbstractMap.SimpleEntry<>("aaa", 89));
        System.out.println(adjSet.contains(new AbstractMap.SimpleEntry<>("aaa", 89)));
        ;

        ArrayList<String> a = new ArrayList<>();
        a.add("aa");
        a.add("bb");
        var c = Arrays.asList(a., "cc");

    }

}
