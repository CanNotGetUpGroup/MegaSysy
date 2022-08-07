package backend.pass;

import backend.machineCode.Instruction.*;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static backend.machineCode.Instruction.Arithmetic.Type.*;

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

    Map<Register, Integer> colorMap;

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
                if (def != null && !curLiveInfo.liveUse.contains(def)) {
                    curLiveInfo.liveDef.add(def);
                }
                for (var use : uses)
                    if (!curLiveInfo.liveDef.contains(use)) {
                        curLiveInfo.liveUse.add(use);
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
        if (adjSet.contains(new AbstractMap.SimpleEntry<>(u, v))) return;
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
            adjList.putIfAbsent(v, new HashSet<>());
            var set = adjList.get(v);
            set.add(u);
            int deg = degree.getOrDefault(v, 0) + 1;
            degree.put(v, deg);
        }
    }

    void build(MachineFunction func) {
        for (var bb : func.getBbList()) {
            var liveInfo = liveInfoMap.get(bb);
            var live = new HashSet<>(liveInfo.liveOut);

            var instList = bb.getInstList();
            for (var instNode = instList.getLast();
                 instNode != instList.getHead(); instNode = instNode.getPrev()) {
                if (instNode == null) break;
                var inst = instNode.getVal();
                if (inst instanceof Move && inst.getOp2() instanceof Register) {
                    inst.getUse().forEach(live::remove);
                    for (var i : inst.getUse()) {
                        moveList.putIfAbsent(i, new HashSet<>());
                        moveList.get(i).add((Move) inst);
                    }
                    if (inst.getDef() != null) {
                        moveList.putIfAbsent(inst.getDef(), new HashSet<>());
                        moveList.get(inst.getDef()).add((Move) inst);
                    }

                    worklistMoves.add((Move) inst);
                }
                var def = inst.getDef();
                if (def != null) {
                    live.add(def);
                    for (var l : live) {
                        addEdge(l, def);
                    }
                    // live := use(I) ∪ (live\def(I))
                    live.remove(def);
                }
                live.addAll(inst.getUse());
            }
        }
    }

    Set<Register> getAdjacent(Register reg) {
        return adjList.getOrDefault(reg, new HashSet<>())
                .stream()
                .filter(n -> !selectStack.contains(n))
                .filter(n -> !coalescedNodes.contains(n))
                .collect(Collectors.toSet());
    }

    // function NodeMoves (n)
    //moveList[n] ∩ (activeMoves ∪ worklistMoves)
    Set<Move> getNodeMoves(Register reg) {
        return moveList
                .getOrDefault(reg, new HashSet<>())
                .stream()
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
        if (reg == null)
            throw new RuntimeException("null register");
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
        if (m.getOp2() instanceof ImmediateNumber) {
            worklistMoves.remove(m);
            return;
        }

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
        if (reg == null) throw new RuntimeException("null reg");
        if (!reg.isPrecolored() && !isMoveRelated(reg)
                && degree.getOrDefault(reg, 0) < MCRegister.maxRegNum(Register.Content.Int)) {
            freezeWorklist.remove(reg);
            simplifyWorklist.add(reg);
        }
    }

    // function OK(t,r)
    //degree[t] < K ∨ t ∈ precolored ∨ (t, r) ∈ adjSet
    boolean OK(Register t, Register r) {
        return degree.get(t) < MCRegister.maxRegNum(Register.Content.Int) || t.isPrecolored() || adjSet.contains(new AbstractMap.SimpleEntry<>(t, r));
    }

    boolean conservative(Collection<Register> nodes) {
        int k = 0;
        for (var n : nodes) {
            if (degree.get(n) > MCRegister.maxRegNum(Register.Content.Int)) k++;
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
        if (freezeWorklist.contains(v)) {
            freezeWorklist.remove(v);
        } else {
            spillWorklist.remove(v);
        }
        coalescedNodes.add(v);
        alias.put(v, u);
        moveList.get(u).addAll(moveList.get(v));
        for (var t : getAdjacent(v)) {
            addEdge(t, u);
            DecrementDegree(t);
        }
        if (degree.getOrDefault(u, 0) >= MCRegister.maxRegNum(Register.Content.Int)
                && freezeWorklist.contains(u)) {
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

    void FreezeMoves(Register u) {
        for (var m : getNodeMoves(u)) {
            // m(= copy(u,v) or copy(v,u)) ∈ NodeMoves(u)

            if (activeMoves.contains(m)) {
                activeMoves.remove(m);
            } else {
                worklistMoves.remove(m);
            }
            frozenMoves.add(m);

            var v = m.getDest().equals(u) ? m.getOp2() : m.getDest();
            if (v != null && !isMoveRelated((Register) v) && degree.getOrDefault(v, 0) < MCRegister.maxRegNum(Register.Content.Int)) {
                freezeWorklist.remove(v);
                simplifyWorklist.add((Register) v);
            }
        }
    }
    private Random rand = new Random();

    void SelectSpill() {
//        var m = spillWorklist.iterator().next();
        var n = new ArrayList<>(spillWorklist);
        var i = rand.nextInt(n.size());
        var m = n.get(i);

        // TODO: elected using favorite heuristic
        //Note: avoid choosing nodes that are the tiny live ranges
        //resulting from the fetches of previously spilled registers

        spillWorklist.remove(m);
        simplifyWorklist.add(m);
        FreezeMoves(m);
    }

    void AssignColors() {
//        System.out.println("select stack" + selectStack);
        for (int i = 0; i < MCRegister.maxRegNum(Register.Content.Int); i++) {
            colorMap.put(new MCRegister(Register.Content.Int, i), i);
        }


        while (!selectStack.isEmpty()) {
            var n = selectStack.pop();
            var okColors = IntStream.range(0, MCRegister.maxRegNum(Register.Content.Int)).boxed().collect(Collectors.toCollection(HashSet::new));
            for (var w : adjList.getOrDefault(n, new HashSet<>())) {
                var alias = getAlias(w);
                if (coloredNodes.contains(alias) || alias.isPrecolored()) {
                    okColors.remove(colorMap.get(alias));
                }
            }
            if (okColors.isEmpty()) {
                spilledNodes.add(n);
            } else {
                coloredNodes.add(n);
                var c = okColors.iterator().next();
                colorMap.put(n, c);
            }
        }
        for (var n : coloredNodes) {
            // TODO: MC register not in colorMap
//            System.out.println("color:" + getAlias(n) + " " + colorMap.get(getAlias(n)));
            colorMap.put(n, colorMap.get(getAlias(n)));
        }
    }


    void substituteAllRegister(MachineFunction func) {
        for (var bb : func.getBbList()) {
            for (var i : bb.getInstList()) {
                var dest = i.getDest();
                var op1 = i.getOp1();
                var op2 = i.getOp2();
                if (dest instanceof VirtualRegister) {
//                    System.out.println(dest);
                    ((VirtualRegister) dest).setColorId(colorMap.get(getAlias(dest)));
                }
                if (op1 instanceof VirtualRegister) {
                    ((VirtualRegister) op1).setColorId(colorMap.get(getAlias((Register) op1)));
                }
                if (op2 instanceof VirtualRegister) {
                    ((VirtualRegister) op2).setColorId(colorMap.get(getAlias((Register) op2)));
                } else if (op2 instanceof Address) {
                    var add = (Address) op2;
                    var reg = add.getReg();
                    var off = add.getReg();
                    if (reg instanceof VirtualRegister) {
                        ((VirtualRegister) reg).setColorId(colorMap.get(getAlias(reg)));
                    }
                    if (off instanceof VirtualRegister) {
                        ((VirtualRegister) off).setColorId(colorMap.get(getAlias(off)));
                    }
                }
            }
        }
    }

    void RewriteProgram(MachineFunction func) {
        int spilledNum = spilledNodes.size();
        func.addSpliteNumOnStack(spilledNum);
        var spillMap = new HashMap<Register, Integer>(); // store the spilled register and its according store space
        int i = 0;
        for (var reg : spilledNodes) {
            spillMap.put(reg, i++);
        }
        for (var bb : func.getBbList()) {
            // lots of load and store for spilt register
            for (var inst : bb.getInstList()) {
                var dest = inst.getDest();
                var op1 = inst.getOp1();
                var op2 = inst.getOp2();
                if (dest instanceof VirtualRegister && spillMap.containsKey(dest)) {
                    var prevNode = inst;
                    int offset = 4 * spillMap.get(dest) + func.getStackTop();
                    Address addr;
                    if (offset < 1024) addr = new Address(new MCRegister(MCRegister.RegName.r11), -offset);
                    else {
                        MachineInstruction temp;
                        var addrReg = new VirtualRegister();
                        if (ImmediateNumber.isLegalImm(offset))
                            temp = new Arithmetic(inst.getParent(), SUB, addrReg, new MCRegister(Register.Content.Int, 11), offset);
                        else {
                            var iii = new LoadImm(inst.getParent(), addrReg, offset);
                            iii.getInstNode().insertAfter(prevNode.getInstNode());
                            prevNode = iii;
                            temp = new Arithmetic(inst.getParent(), RSB, addrReg, addrReg, new MCRegister(MCRegister.RegName.r11));
                        }
                        temp.getInstNode().insertAfter(prevNode.getInstNode());
                        prevNode = temp;
                        addr = new Address(addrReg);
                    }
                    new LoadOrStore(bb, LoadOrStore.Type.STORE, dest, addr).setForFloat(dest.isFloat(), new ArrayList<>(List.of("32"))).getInstNode().insertAfter(prevNode.getInstNode());
                }
                if (op1 instanceof VirtualRegister && spillMap.containsKey(op1)) {
                    int offset = 4 * spillMap.get((VirtualRegister) op1) + func.getStackTop();
                    Address addr;
                    if (offset < 1024) addr = new Address(new MCRegister(MCRegister.RegName.r11), -offset);
                    else {
                        var addrReg = new VirtualRegister();
                        MachineInstruction temp;
                        if (ImmediateNumber.isLegalImm(offset))
                            temp = new Arithmetic(inst.getParent(), SUB, addrReg, new MCRegister(Register.Content.Int, 11), offset);
                        else {
                            var iii = new LoadImm(inst.getParent(), addrReg, offset);
                            iii.getInstNode().insertBefore(inst.getInstNode());
                            temp = new Arithmetic(inst.getParent(), RSB, addrReg, addrReg, new MCRegister(MCRegister.RegName.r11));
                        }
                        temp.getInstNode().insertBefore(inst.getInstNode());
                        addr = new Address(addrReg);
                    }

                    new LoadOrStore(bb, LoadOrStore.Type.LOAD, (VirtualRegister) op1, addr).setForFloat(((VirtualRegister) op1).isFloat(), new ArrayList<>(List.of("32"))).getInstNode().insertBefore(inst.getInstNode());
                }
                if (op2 instanceof VirtualRegister && spillMap.containsKey(op2)) {
                    int offset = 4 * spillMap.get((VirtualRegister) op2) + func.getStackTop();
                    Address addr;
                    if (offset < 1024) addr = new Address(new MCRegister(MCRegister.RegName.r11), -offset);
                    else {
                        var addrReg = new VirtualRegister();
                        MachineInstruction temp;
                        if (ImmediateNumber.isLegalImm(offset))
                            temp = new Arithmetic(inst.getParent(), SUB, addrReg, new MCRegister(Register.Content.Int, 11), offset);
                        else {
                            var iii = new LoadImm(inst.getParent(), addrReg, offset);
                            iii.getInstNode().insertBefore(inst.getInstNode());
                            temp = new Arithmetic(inst.getParent(), RSB, addrReg, addrReg, new MCRegister(MCRegister.RegName.r11));
                        }
                        temp.getInstNode().insertBefore(inst.getInstNode());
                        addr = new Address(addrReg);
                    }
                    new LoadOrStore(bb, LoadOrStore.Type.LOAD, (VirtualRegister) op2, addr).setForFloat(((VirtualRegister) op2).isFloat(), new ArrayList<>(List.of("32"))).getInstNode().insertBefore(inst.getInstNode());
                } else if (op2 instanceof Address) {
                    var reg = ((Address) op2).getReg();

                    if (reg instanceof VirtualRegister && spillMap.containsKey(reg)) {

                        int offset = 4 * spillMap.get(((Address) op2).getReg()) + func.getStackTop();
                        Address addr;
                        if (offset < 1024) addr = new Address(new MCRegister(MCRegister.RegName.r11), -offset);
                        else {
                            var addrReg = new VirtualRegister();
                            MachineInstruction temp;
                            if (ImmediateNumber.isLegalImm(offset))
                                temp = new Arithmetic(inst.getParent(), SUB, addrReg, new MCRegister(Register.Content.Int, 11), offset);
                            else {
                                var iii = new LoadImm(inst.getParent(), addrReg, offset);
                                iii.getInstNode().insertBefore(inst.getInstNode());
                                temp = new Arithmetic(inst.getParent(), RSB, addrReg, addrReg, new MCRegister(MCRegister.RegName.r11));
                            }
                            temp.getInstNode().insertBefore(inst.getInstNode());
                            addr = new Address(addrReg);
                        }

                        new LoadOrStore(bb, LoadOrStore.Type.LOAD, reg, addr).getInstNode().insertBefore(inst.getInstNode());
                    }
                    //  地址的第二个参数可能也是reg, 肯定还得是int
                    var offsetReg = ((Address) op2).getOffset();
                    if (offsetReg instanceof VirtualRegister && spillMap.containsKey(offsetReg)) {
                        int offset = 4 * spillMap.get(offsetReg) + func.getStackTop();
                        Address addr;
                        // TODO: 1024 for coprocessor and 4096 for arm processor
                        if (offset < 1024) addr = new Address(new MCRegister(MCRegister.RegName.r11), -offset);
                        else {
                            MachineInstruction temp;
                            var addrReg = new VirtualRegister();
                            if (ImmediateNumber.isLegalImm(offset))
                                temp = new Arithmetic(inst.getParent(), SUB, addrReg, new MCRegister(Register.Content.Int, 11), offset);
                            else {
                                var iii = new LoadImm(inst.getParent(), addrReg, offset);
                                iii.getInstNode().insertBefore(inst.getInstNode());
                                temp = new Arithmetic(inst.getParent(), RSB, addrReg, addrReg, new MCRegister(MCRegister.RegName.r11));
                            }
                            temp.getInstNode().insertBefore(inst.getInstNode());
                            addr = new Address(addrReg);
                        }
                        new LoadOrStore(bb, LoadOrStore.Type.LOAD, (VirtualRegister) offsetReg, addr).getInstNode().insertBefore(inst.getInstNode());
                    }
                } // end of if op2 is address
            }
        }

        func.addStackTop(4 * spilledNum);
        spilledNodes = new HashSet<>();
//        initial := coloredNodes ∪ coalescedNodes ∪ newTemps coloredNodes := {}
        coalescedNodes = new HashSet<>();
    }

    void setStack(MachineFunction func) {
        if (!func.isDefined()) return;

        MachineBasicBlock firstBb = func.getBbList().getFirst().getVal();
        MachineInstruction newInst;
        int saveOnStack = 7;

        // reserve space for temp variable on stack
        for (var inst : firstBb.getInstList()) {
            if (!inst.isPrologue()) {
                int paraOnStack = func.getMaxParaNumOnStack();
                // Push FP
                newInst = new PushOrPop(firstBb, PushOrPop.Type.Push, new MCRegister(MCRegister.RegName.r11));
                newInst.setPrologue(true);
                newInst.getInstNode().insertBefore(inst.getInstNode());


                // set Frame Pointer -> Fp = sp + 4
                newInst = new Arithmetic(firstBb, ADD, new MCRegister(MCRegister.RegName.r11), new MCRegister(MCRegister.RegName.SP), new ImmediateNumber(4));
                newInst.setPrologue(true);
                newInst.getInstNode().insertBefore(inst.getInstNode());

                // push callee save register
                // TODO: only save those used

                for(int i = 10; i >= 4; i--){
                    newInst = new PushOrPop(firstBb, PushOrPop.Type.Push, new MCRegister(Register.Content.Int, i));
                    newInst.setPrologue(true);
                    newInst.getInstNode().insertBefore(inst.getInstNode());
                }
                func.addStackTop(4 * saveOnStack);

                // reserve for spilled
                int offset = 4 * func.getSpiltNumOnStack() + 4 * paraOnStack;
                if ((offset + func.getStackTop()) % 8 != 0) offset += 4;
                MCOperand c;
                if (ImmediateNumber.isLegalImm(offset)) c = new ImmediateNumber(offset);
                else {
                    c = new MCRegister(Register.Content.Int, 9);
                    new LoadImm(firstBb, (Register) c, offset).getInstNode().insertBefore(inst.getInstNode());
                }
                new Arithmetic(firstBb, SUB, new MCRegister(MCRegister.RegName.SP), c).getInstNode().insertBefore(inst.getInstNode());
                break;
            }
        }

        // release stack
        for (var bb : func.getBbList()) {
            for (var inst : bb.getInstList()) {
                if (inst.isEpilogue()) {

                    new Arithmetic(firstBb, SUB,
                            new MCRegister(MCRegister.RegName.SP),
                            new MCRegister(MCRegister.RegName.r11),
                            new ImmediateNumber(4 * (saveOnStack + 1)))
                            .getInstNode().insertBefore(inst.getInstNode());

                    for(int i = 4; i <= 11; i++) {
                        newInst = new PushOrPop(bb, PushOrPop.Type.Pop, new MCRegister(Register.Content.Int, i));
                        newInst.setEpilogue(true);
                        newInst.getInstNode().insertBefore(inst.getInstNode());
                    }
                }
            }
        }
    }

    HashSet<Register> spiltRegs = new HashSet<>();

    public void run() {
        var MCdegree = new HashMap<Register, Integer>();
        for (int i = 0; i < 20; i++) {
            MCdegree.put(new MCRegister(Register.Content.Int, i), Integer.MAX_VALUE);
        }
        for (var f : funcList) {
            if (!f.isDefined()) continue;

            while (true) {
                simplifyWorklist = new HashSet<>(); // list of low-degree non-move-related nodes.
                freezeWorklist = new HashSet<>(); // low-degree move-related nodes.
                spillWorklist = new HashSet<>(); // high-degree nodes.
                spilledNodes = new HashSet<>(); // nodes marked for spilling during this round; initially empty.
                coalescedNodes = new HashSet<>(); // registers that have been coalesced; when the move u:=v is coalesced, one of u or v is added to this set, and the other is put back on some worklist.
                coloredNodes = new HashSet<>(); // nodes successfully colored.
                selectStack = new ArrayDeque<>(); // stack containing temporaries removed from the graph.

                coalescedMoves = new HashSet<>(); // moves that have been coalesced.
                constrainedMoves = new HashSet<>();// moves whose source and target interfere.
                frozenMoves = new HashSet<>();// moves that will no longer be considered for coalescing.
                worklistMoves = new HashSet<>(); // moves enabled for possible coalescing.
                activeMoves = new HashSet<>(); //moves not yet ready for coalescing.

                adjSet = new HashSet<>(); // the set of interference edges (u, v) in the graph.
                adjList = new HashMap<>(); // the set of interference edges (u, v) in the graph.
                degree = new HashMap<>(MCdegree); // an array containing the current degree of each node. Precolored nodes are initialized with a degree of ∞,

                moveList = new HashMap<>(); // a mapping from node to the list of moves it is associated with.
                alias = new HashMap<>(); // when a move (u, v) has been coalesced, and v put in coalescedNodes, then alias(v) = u.

                colorMap = new HashMap<>();

                liveInfoMap = new HashMap<>();


                LivenessAnalysis(f);
                build(f);
                Set<Register> init = new HashSet<>();
                for (var bb : f.getBbList()) {
                    for (var i : bb.getInstList()) {
                        if (i.getDef() != null && i.getDef() instanceof VirtualRegister)
                            init.add(i.getDef());
                        init.addAll(i.getUse().stream().filter(x -> x instanceof VirtualRegister).toList());
                    }
                }
                makeWorkList(init);
                do {
                    if (!simplifyWorklist.isEmpty())
                        Simplify();
                    else if (!worklistMoves.isEmpty())
                        Coalesce();
                    else if (!freezeWorklist.isEmpty())
                        freeze();
                    else if (!spillWorklist.isEmpty()) {

//                        System.out.println("spill");
                        SelectSpill();
                    }
                } while (!simplifyWorklist.isEmpty() || !worklistMoves.isEmpty() || !freezeWorklist.isEmpty() || !spillWorklist.isEmpty());
                AssignColors();
                if (spilledNodes.isEmpty()) {

                    substituteAllRegister(f);
                    setStack(f);
                    break;
                }
                System.out.println("Rewrite");
                RewriteProgram(f);
            }
        }
    }

    public static void main(String[] args) {
        HashMap<Register, Integer> map = new HashMap<>();
        map.put(new MCRegister(Register.Content.Int, 0), 1);
        System.out.println(map.containsKey(new MCRegister(Register.Content.Int, 0)));

    }

}
