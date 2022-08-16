package backend.pass;

import backend.machineCode.Instruction.*;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.*;
import ir.Constant;
import ir.Function;

import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static backend.machineCode.Instruction.Arithmetic.Type.*;

// ref "Iterated Register Coalescing" by LAL GEORGE and ANDREW W. APPEL
public class GraphColor {
    private final ArrayList<MachineFunction> funcList;

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

    Register.Content curPassType;

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

                for (var d : def) {
                    if (!curLiveInfo.liveUse.contains(d) && d.getContent() == curPassType) {
                        curLiveInfo.liveDef.add(d);
                    }
                }
                for (var use : uses)
                    if (!curLiveInfo.liveDef.contains(use) && use.getContent() == curPassType) {
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
                if (inst instanceof Move && inst.getOp2() instanceof Register
                        && inst.getUse().get(0).getContent() == curPassType &&
                        inst.getDef().get(0).getContent() == curPassType) {
                    inst.getUse().forEach(live::remove);
                    for (var i : inst.getUse()) {
                        if (i.getContent() != curPassType)
                            continue;
                        moveList.putIfAbsent(i, new HashSet<>());
                        moveList.get(i).add((Move) inst);
                    }

                    for (var i : inst.getDef()) {
                        if (i.getContent() != curPassType)
                            continue;
                        moveList.putIfAbsent(i, new HashSet<>());
                        moveList.get(i).add((Move) inst);
                    }

                    worklistMoves.add((Move) inst);
                }
                var def = inst.getDef()
                        .stream().filter(x -> x.getContent() == curPassType).collect(Collectors.toSet());
                live.addAll(def);
                for (var d : def) {
                    for (var l : live) {
                        addEdge(l, d);
                    }
                    // live := use(I) ∪ (live\def(I))
                    live.remove(d);
                }

                live.addAll(inst.getUse().stream().filter(x -> x.getContent() == curPassType).collect(Collectors.toSet()));
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
            if (degree.getOrDefault(reg, 0) >= MCRegister.maxRegNum(curPassType)) {
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
        if (d == MCRegister.maxRegNum(Register.Content.Float)) {
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
                && degree.getOrDefault(reg, 0) < MCRegister.maxRegNum(curPassType)) {
            freezeWorklist.remove(reg);
            simplifyWorklist.add(reg);
        }
    }

    // function OK(t,r)
    //degree[t] < K ∨ t ∈ precolored ∨ (t, r) ∈ adjSet
    boolean OK(Register t, Register r) {
        return degree.get(t) < MCRegister.maxRegNum(curPassType) || t.isPrecolored() || adjSet.contains(new AbstractMap.SimpleEntry<>(t, r));
    }

    boolean conservative(Collection<Register> nodes) {
        int k = 0;
        for (var n : nodes) {
            if (degree.get(n) > MCRegister.maxRegNum(curPassType)) k++;
        }
        return k < MCRegister.maxRegNum(curPassType);
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
        if (degree.getOrDefault(u, 0) >= MCRegister.maxRegNum(curPassType)
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
            if (v != null && !isMoveRelated((Register) v) && degree.getOrDefault(v, 0) < MCRegister.maxRegNum(curPassType)) {
                freezeWorklist.remove(v);
                simplifyWorklist.add((Register) v);
            }
        }
    }

    void SelectSpill() {
        Double maxScore = 0.0;
        Register m = spillWorklist.iterator().next();
        for (var i : spillWorklist) {
            double curScore;
            curScore = ((double) degree.getOrDefault(i, 0)) / Math.pow(2, loopDepth.getOrDefault(i, 0));

            if (substitutions.contains(i)) {
                curScore = -1;
            }
            if (curScore >= maxScore) {
                maxScore = curScore;
                m = i;
            }
        }

        //Note: avoid choosing nodes that are the tiny live ranges
        //resulting from the fetches of previously spilled registers

        spillWorklist.remove(m);
        simplifyWorklist.add(m);
        FreezeMoves(m);
    }

    void AssignColors() {

        for (int i = 0; i < MCRegister.maxRegNum(curPassType); i++) {
            colorMap.put(new MCRegister(curPassType, i), i);

        }

        // select stack filter
        while (!selectStack.isEmpty()) {
            var n = selectStack.pop();
            var okColors = IntStream.range(0, MCRegister.maxRegNum(curPassType)).boxed()
                    .collect(Collectors.toCollection(HashSet::new));

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
            colorMap.put(n, colorMap.get(getAlias(n)));
        }
    }


    void substituteAllRegister(MachineFunction func) {
        for (var bb : func.getBbList()) {
            for (var i : bb.getInstList()) {
                var dest = i.getDest();
                var op1 = i.getOp1();
                var op2 = i.getOp2();
                if (dest instanceof VirtualRegister && dest.getContent() == curPassType) {
                    int color = colorMap.get(getAlias(dest));
                    ((VirtualRegister) dest).setColorId(color);
                    registerUsed.add(new MCRegister(dest.getContent(), color));
                }
                if (op1 instanceof VirtualRegister && ((VirtualRegister) op1).getContent() == curPassType) {
                    int color = colorMap.get(getAlias((Register) op1));
                    ((VirtualRegister) op1).setColorId(color);
                    registerUsed.add(new MCRegister(((Register) op1).getContent(), color));
                }
                if (op2 instanceof VirtualRegister && ((VirtualRegister) op2).getContent() == curPassType) {
                    int color = colorMap.get(getAlias((Register) op2));
                    ((VirtualRegister) op2).setColorId(color);
                    registerUsed.add(new MCRegister(((Register) op2).getContent(), color));
                } else if (op2 instanceof Address) {
                    var add = (Address) op2;
                    var reg = add.getReg();
                    var off = add.getOffset();
                    if (reg instanceof VirtualRegister && reg.getContent() == curPassType) {
                        int color = colorMap.get(getAlias(reg));
                        ((VirtualRegister) reg).setColorId(colorMap.get(getAlias(reg)));
                        registerUsed.add(new MCRegister(reg.getContent(), color));
                    }
                    if (off instanceof VirtualRegister && ((VirtualRegister) off).getContent() == curPassType) {
                        int color = colorMap.get(getAlias((Register) off));
                        ((VirtualRegister) off).setColorId(colorMap.get(getAlias((Register) off)));
                        registerUsed.add(new MCRegister(((Register) off).getContent(), color));
                    }
                }
            }
        }
    }

    boolean isLegalOffset(Register reg, int offset) {
        if (reg.isFloat()) {
            return offset < 1024 && offset > -1024;
        } else
            return offset < 4096 && offset > -4096;
    }

    void getSubstitutionAfter(Register ori, Register substitution, int offset, MachineInstruction prevNode) {
        Address addr;
        if (isLegalOffset(ori, offset))
            addr = new Address(new MCRegister(MCRegister.RegName.SP), offset);
        else {
            var addrReg = new VirtualRegister();
            var iii = new LoadImm(prevNode.getParent(), addrReg, offset);
            iii.getInstNode().insertAfter(prevNode.getInstNode());
            prevNode = iii;
            if (ori.isFloat()) {
                // TODO: 这段没经过测试
                var node = new Arithmetic(prevNode.getParent(), ADD, addrReg, new MCRegister(MCRegister.RegName.SP))
                        .setForFloat(true);
                node.insertAfter(prevNode);
                prevNode = node;
                addr = new Address(addrReg);
            } else
                addr = new Address(new MCRegister(MCRegister.RegName.SP), addrReg);
        }
        new LoadOrStore(prevNode.getParent(), LoadOrStore.Type.STORE, substitution, addr)
                .setForFloat(ori.isFloat(), new ArrayList<>(List.of("32")))
                .getInstNode().insertAfter(prevNode.getInstNode());
    }

    void getSubstitutionBefore(Register ori, Register substitution, int offset, MachineInstruction nextNode) {
        Address addr;
        if (isLegalOffset((Register) ori, offset))
            addr = new Address(new MCRegister(MCRegister.RegName.SP), offset);
        else {
            var addrReg = new VirtualRegister();
            var iii = new LoadImm(nextNode.getParent(), addrReg, offset);
            iii.getInstNode().insertBefore(nextNode.getInstNode());
            if (ori.isFloat()) {
                new Arithmetic(nextNode.getParent(), ADD, addrReg, new MCRegister(MCRegister.RegName.SP))
                        .setForFloat(true)
                        .insertBefore(nextNode);
                addr = new Address(addrReg);
            } else
                addr = new Address(new MCRegister(MCRegister.RegName.SP), addrReg);
        }

        new LoadOrStore(nextNode.getParent(), LoadOrStore.Type.LOAD, substitution, addr)
                .setForFloat(ori.isFloat(), new ArrayList<>(List.of("32")))
                .insertBefore(nextNode);
    }


    void RewriteProgram(MachineFunction func) {
        int spilledNum = spilledNodes.size();
        func.addSpliteNumOnStack(spilledNum);
        var spillMap = new HashMap<Register, Integer>(); // store the spilled register and its according store space
        int i = 0;
        for (var reg : spilledNodes) {
            spillMap.put(reg, i++);
        }
        // TODO: 现在vreg溢出之后就一直在内存里，但可以考虑溢出之后还有机会回来
        for (var bb : func.getBbList()) {
            // lots of load and store for spilt register
            for (var inst : bb.getInstList()) {
                var dest = inst.getDest();
                var op1 = inst.getOp1();
                var op2 = inst.getOp2();
                if (dest instanceof VirtualRegister && spillMap.containsKey(dest)) {
                    var substitution = new VirtualRegister((dest).getContent());
                    substitutions.add(substitution);
                    var prevNode = inst;
                    int offset = 4 * spillMap.get(dest) + func.getStackSize();
                    getSubstitutionAfter(dest, substitution, offset, prevNode);
                    inst.setDest(substitution);
                }
                if (op1 instanceof VirtualRegister && spillMap.containsKey(op1)) {
                    var substitution = new VirtualRegister(((VirtualRegister) op1).getContent());
                    substitutions.add(substitution);
                    int offset = 4 * spillMap.get((VirtualRegister) op1) + func.getStackSize();
                    getSubstitutionBefore((Register) op1, substitution, offset, inst);

                    inst.setOp1(substitution);
                }
                if (op2 instanceof VirtualRegister && spillMap.containsKey(op2)) {
                    var substitution = new VirtualRegister(((VirtualRegister) op2).getContent());
                    substitutions.add(substitution);
                    int offset = 4 * spillMap.get((VirtualRegister) op2) + func.getStackSize();
                    getSubstitutionBefore((Register) op2, substitution, offset, inst);

                    inst.setOp2(substitution);
                } else if (op2 instanceof Address) {
                    var reg = ((Address) op2).getReg();

                    if (reg instanceof VirtualRegister && spillMap.containsKey(reg)) {
                        var substitution = new VirtualRegister(reg.getContent());
                        substitutions.add(substitution);
                        int offset = 4 * spillMap.get(reg) + func.getStackSize();
                        getSubstitutionBefore(reg, substitution, offset, inst);

                        ((Address) op2).setReg(substitution);
                    }
                    //  地址的第二个参数可能也是reg, 肯定还得是int
                    var offsetReg = ((Address) op2).getOffset();
                    if (offsetReg instanceof VirtualRegister && spillMap.containsKey(offsetReg)) {
                        var substitution = new VirtualRegister(((VirtualRegister) offsetReg).getContent());
                        substitutions.add(substitution);
                        int offset = 4 * spillMap.get(offsetReg) + func.getStackSize();

                        getSubstitutionBefore((Register) offsetReg, substitution, offset, inst);

                        ((Address) op2).setOffset(substitution);
                    }
                } // end of if op2 is address
            }
        }

        func.addStackSize(4 * spilledNum);
        spilledNodes = new HashSet<>();
        coalescedNodes = new HashSet<>();
    }

    void setStack(MachineFunction func) {
        if (!func.isDefined()) return;

        MachineBasicBlock firstBb = func.getBbList().getFirst().getVal();
        MachineInstruction newInst;
        // TODO:
        int saveOnStack = 9 + 16;

        // reserve space for temp variable on stack
        int paraOnStack = func.getMaxParaNumOnStack();

        // push callee save register
        // TODO: only save those used
        newInst = new PushOrPop(firstBb, PushOrPop.Type.Push, new MCRegister(MCRegister.RegName.LR));
        newInst.setPrologue(true);
        newInst.insertBefore(firstBb.getInstList().getFirst());
        var insertPoint = newInst;
        for (int i = 11; i >= 4; i--) {
            if (!registerUsed.contains(new MCRegister(Register.Content.Int, i))) {
                saveOnStack--;
                continue;
            }
            newInst = new PushOrPop(firstBb, PushOrPop.Type.Push, new MCRegister(Register.Content.Int, i));
            newInst.setPrologue(true);
            newInst.insertAfter(insertPoint);
            insertPoint = newInst;
        }
        for (int i = 31; i >= 16; i--) {
            if (!registerUsed.contains(new MCRegister(Register.Content.Float, i))) {
                saveOnStack--;
                continue;
            }
            newInst = new PushOrPop(firstBb, PushOrPop.Type.Push, new MCRegister(Register.Content.Float, i)).setForFloat(true);
            newInst.setPrologue(true);
            newInst.insertAfter(insertPoint);
            insertPoint = newInst;
        }


        // reserve for spilled
        if ((func.getStackSize() + saveOnStack * 4) % 8 != 0)
            func.addStackSize(4);
        func.addStoredRegisterNum(saveOnStack);

        // TODO: release stack
        for (var bb : func.getBbList()) {
            for (var inst : bb.getInstList()) {
                if (inst.isEpilogue()) {

                    new LoadImm(inst.getParent(),
                            new MCRegister(Register.Content.Int, 3),
                            func.getStackSize()).insertBefore(inst);

                    // TODO: modify
                    new Arithmetic(firstBb, ADD,
                            new MCRegister(MCRegister.RegName.SP),
                            new MCRegister(MCRegister.RegName.SP),
                            new MCRegister(Register.Content.Int, 3))
                            .insertBefore(inst);

                    for (int i = 16; i <= 31; i++) {
                        var reg = new MCRegister(Register.Content.Float, i);
                        if (!registerUsed.contains(reg)) {
                            continue;
                        }
                        // TODO: leaf function
                        newInst = new PushOrPop(bb, PushOrPop.Type.Pop, reg).setForFloat(true);
                        newInst.setEpilogue(true);
                        newInst.getInstNode().insertBefore(inst.getInstNode());
                    }
                    for (int i = 4; i <= 11; i++) {
                        if (!registerUsed.contains(new MCRegister(Register.Content.Int, i))) {
                            continue;
                        }
                        // TODO: leaf function
                        newInst = new PushOrPop(bb, PushOrPop.Type.Pop, new MCRegister(Register.Content.Int, i));
                        newInst.setEpilogue(true);
                        newInst.getInstNode().insertBefore(inst.getInstNode());
                    }
                }
            }
        }
    }

    HashSet<Register> substitutions = new HashSet<>();
    HashSet<Register> registerUsed = new HashSet<>();
    HashMap<Register, Integer> loopDepth;

    public void run() {
        var MCdegree = new HashMap<Register, Integer>();
        var MCUsed = new HashSet<Register>();
        for (int i = 0; i < 20; i++) {
            MCdegree.put(new MCRegister(Register.Content.Int, i), Integer.MAX_VALUE);
        }
        for (int i = 0; i < 33; i++) {
            MCdegree.put(new MCRegister(Register.Content.Float, i), Integer.MAX_VALUE);
        }
        for (int i = 0; i < 4; i++) {
            MCUsed.add(new MCRegister(Register.Content.Int, i));
        }
        for (int i = 12; i < 20; i++) {
            MCUsed.add(new MCRegister(Register.Content.Int, i));
        }
        for (int i = 0; i < 16; i++) {
            MCUsed.add(new MCRegister(Register.Content.Float, i));
        }

        for (var f : funcList) {
            if (!f.isDefined()) continue;
            registerUsed = new HashSet<>(MCUsed);

            for (int pass = 0; pass < 2; pass++) {
                if (pass == 0)
                    curPassType = Register.Content.Float;
                else
                    curPassType = Register.Content.Int;

                int time = 0;
                substitutions = new HashSet<>();
                while (true) {
//                    if (++time > 10) throw new RuntimeException("to many rewrite");
                    // initial all data structure

                    {
                        loopDepth = new HashMap<>();
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
                        // id of MC Register have been used
                    }
                    LivenessAnalysis(f);
                    build(f);

                    Set<Register> init = new HashSet<>();
                    for (var bb : f.getBbList()) {
                        int bbDepth = bb.getLoopDepth();
                        for (var i : bb.getInstList()) {
                            init.addAll(i.getDef().stream().filter(x -> x instanceof VirtualRegister && x.getContent() == curPassType).collect(Collectors.toSet()));
                            init.addAll(i.getUse().stream().filter(x -> x instanceof VirtualRegister && x.getContent() == curPassType).collect(Collectors.toSet()));
                            for (var u : i.getUse()) {
                                var depth = loopDepth.getOrDefault(u, 0);
                                if (bbDepth > depth) {
                                    depth = bbDepth;
                                }
                                loopDepth.put(u, depth);
                            }
                            for (var u : i.getDef()) {
                                var depth = loopDepth.getOrDefault(u, 0);
                                if (bbDepth > depth) {
                                    depth = bbDepth;
                                }
                                loopDepth.put(u, depth);
                            }
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
                            SelectSpill();
                        }
                    } while (!simplifyWorklist.isEmpty() || !worklistMoves.isEmpty() || !freezeWorklist.isEmpty() || !spillWorklist.isEmpty());
                    AssignColors();
                    if (spilledNodes.isEmpty()) {
                        substituteAllRegister(f);

                        break;
                    }
                    System.out.println("Rewrite, spill : " + spilledNodes.size());
                    RewriteProgram(f);
                }
            }
            setStack(f);
            setUnknownStackSize(f);
        }

    }

    void setUnknownStackSize(MachineFunction f) {
        for (var bb : f.getBbList()) {
            for (var i : bb.getInstList()) {

                if (i instanceof LoadImm && i.getOp2() instanceof StackOffsetNumber) {
                    i.setOp2(new ImmediateNumber(((StackOffsetNumber) i.getOp2()).getValue()));
                }
            }
        }
    }

    public static void main(String[] args) {
    }

}
