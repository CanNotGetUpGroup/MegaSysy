package backend.pass;

import backend.machineCode.Instruction.*;
import backend.CodeGenManager;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.Address;
import backend.machineCode.Operand.ImmediateNumber;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;
import ir.Module;
import pass.MCPass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import org.antlr.v4.runtime.misc.Pair;

public class PeepHole extends MCPass{

    public PeepHole() {
        super();
    }

    private boolean peepHoleWithoutDataflow(CodeGenManager CGM) {
        boolean done = true;
        for(var f : CGM.getFuncList()) {
            if (!f.isDefined()) continue;
            for(var bb : f.getBbList()) {
                if(bb.getInstList().isEmpty()) continue;

                // 消除bb尾无效跳转
                var lastInst = bb.getInstList().getLast().getVal();
                if (lastInst instanceof Branch && lastInst.getCond() == null) {
                    if ( bb.getBbNode().getNext() != null && ((Branch) lastInst).getDestBB() == bb.getBbNode().getNext().getVal()) {
                        lastInst.delete();
                        done = false;
                    }
                }


                for (var next : bb.getInstList()) {
                    var iNode = next.getInstNode().getPrev();
                    if (iNode == null) continue;
                    var i = iNode.getVal();
                    if(i instanceof Arithmetic) {
                        if((((Arithmetic) i).getType() == Arithmetic.Type.ADD || ((Arithmetic) i).getType() == Arithmetic.Type.SUB)
                                && i.getOp2() instanceof ImmediateNumber
                                && ((ImmediateNumber)i.getOp2()).getValue() == 0) {
                            if(i.getDest().equals(i.getOp1())) {
                                // add/sub a a 0 -> del
                                i.delete();
                                done = false;
                            }else {
                                // add/sub a b 0 -> mov a b
                                new Move(bb, i.getDest(), i.getOp1()).insertBefore(i);
                                i.delete();
                                done = false;
                            }
                        }
                    }

                    if(i instanceof LoadOrStore) {
                        // str a [b x]    str a [b x]
                        // ldr c [b x] -> mov c a
                        if(next instanceof LoadOrStore
                            && ((LoadOrStore)i).getType() == LoadOrStore.Type.STORE
                            && ((LoadOrStore)next).getType() == LoadOrStore.Type.LOAD
                            && ((LoadOrStore)i).getOp2().equals(((LoadOrStore)next).getOp2())
                        ) {
                            new Move(bb, next.getDest(), i.getOp1()).insertBefore(next);
                            next.delete();
                            done = false;
                        }
                    }

                    if(i instanceof Move) {
                        // move a a -> del
                        if(i.getDest().equals(i.getOp2()) && !i.hasShift()) {
                            i.delete();
                            done = false;
                        }else {
                            // move a b
                            // move a c -> move a c    !! move a a 不能删除
                            if(next instanceof Move 
                                    && !i.hasShift() && !next.hasShift()
                                    && i.getDest().equals(next.getDest())
                                    && !next.getDest().equals(next.getOp2())) {
                                i.delete();
                                done = false;
                                }
                            // move a b
                            // move b a
                            if(next instanceof Move
                                    && !i.hasShift() && !next.hasShift()
                                    && i.getDest().equals(next.getOp2())
                                    && i.getOp2().equals(next.getDest())
                                ) {
                                    next.delete();
                                    done = false;
                                }
                        }
                    }

                    // TODO： 这段有bug 过不了tc71
                    // // 	mov	a, 4
                    // //	MUL	a, b, a -> LSL
                    // if ((i instanceof Move || i instanceof LoadImm) && i.getOp2() instanceof ImmediateNumber
                    //         && isPowerOfTwo(((ImmediateNumber) i.getOp2()).getValue())
                    //         && next instanceof Arithmetic && ((Arithmetic) next).getType() == Arithmetic.Type.MUL) {
                    //     var r4 = i.getDest();
                    //     var num = ((ImmediateNumber) i.getOp2()).getValue();
                    //     var other = (Register) next.getOp1();
                    //     if (other.equals(r4))
                    //         other = (Register) next.getOp2();
                    //     if (next.getOp2().equals(r4) || next.getOp1().equals(r4)) {
                    //         new Arithmetic(bb, Arithmetic.Type.LSL, next.getDest(), other, new ImmediateNumber(log2(num)))
                    //                 .insertBefore(i);
                    //         i.delete();
                    //         next.delete();
                    //     }
                    // }
                }

            }
        }

        return done;
    }

    private boolean peepHoleWithDataflow(CodeGenManager CGM) {
        boolean done = true;
        for (var f : CGM.getFuncList()) {
            if (!f.isDefined()) continue;
            var funcLivenessMap = funcLivenessAnalysis(f);
            
            for (var bb : f.getBbList()) {
                var lastDef = new HashMap<MCOperand,MachineInstruction>();
                var lastUse = new HashMap<MachineInstruction, MachineInstruction>();
                blockLiveRange(bb, lastDef, lastUse);

                for (var next : bb.getInstList()) {
                    var iNode = next.getInstNode().getPrev();
                    if (iNode == null) continue;
                    var i = iNode.getVal();

                    var iLastUse = lastUse.get(i);

                    // add/sub ldr/str/move
                    // ADD r1, r2, 4
                    // LDR r0, [ r1, 8 ] -> LDR r0, [ r2, 4+8 ]
                    if( !i.hasShift() && !next.hasShift()
                        && i instanceof Arithmetic 
                        && (((Arithmetic) i).getType() == Arithmetic.Type.ADD || ((Arithmetic) i).getType() == Arithmetic.Type.SUB)
                        && i.getOp2() instanceof ImmediateNumber
                            ) {
                        boolean isAdd = ((Arithmetic) i).getType() == Arithmetic.Type.ADD;
                        int iImm = ((ImmediateNumber)i.getOp2()).getValue();

                        if(next instanceof LoadOrStore && Objects.equals(iLastUse, next)) {
                            boolean isSameDest = ((Address)next.getOp2()).getReg().equals(i.getDest());
                            boolean isImmOffset = ((Address)next.getOp2()).getOffset() instanceof ImmediateNumber;
                            if(isSameDest && isImmOffset) {
                                int nextImm = ((ImmediateNumber)((Address)next.getOp2()).getOffset()).getValue();
                                if((isAdd && nextImm+iImm < 4096) || (!isAdd && nextImm-iImm >= 0)) {
                                    var nImm = isAdd ? new ImmediateNumber(nextImm+iImm) : new ImmediateNumber(nextImm-iImm);
                                    ((Address)next.getOp2()).setReg((Register)i.getOp1());
                                    ((Address)next.getOp2()).setOffset(nImm);
                                    i.delete();
                                    done = false;
                                }
                            }
                        } else if (next instanceof Move) {
                        // add/sub a b 2
                        // move c y          move c y
                        // str c [a, 4] ->   str c [b, 2+4]    !! b != c
                            var nextnextNode = next.getInstNode().getNext();
                            if(nextnextNode != null) {
                                var nextnext = nextnextNode.getVal();
                                if( !nextnext.hasShift() 
                                    && nextnext instanceof LoadOrStore 
                                    && ((LoadOrStore)nextnext).getType() == LoadOrStore.Type.STORE 
                                    && Objects.equals(iLastUse, nextnext)) {
                                    var moveStoreData = next.getDest().equals(nextnext.getOp1());
                                    var isSameDest = ((Address)nextnext.getOp2()).getReg().equals(i.getDest());
                                    var notEditStoreReg = !((Register)i.getOp1()).equals(((Address)nextnext.getOp2()).getReg());
                                    var isImmOffset = ((Address)nextnext.getOp2()).getOffset() instanceof ImmediateNumber;

                                    if(moveStoreData && isSameDest && notEditStoreReg && isImmOffset) {
                                        int nextImm = ((ImmediateNumber)((Address)nextnext.getOp2()).getOffset()).getValue();
                                        if((isAdd && nextImm+iImm < 4096) || (!isAdd && nextImm-iImm >= 0)) {
                                            var nImm = isAdd ? new ImmediateNumber(nextImm+iImm) : new ImmediateNumber(nextImm-iImm);
                                            ((Address)nextnext.getOp2()).setReg((Register)i.getOp1());
                                            ((Address)nextnext.getOp2()).setOffset(nImm);
                                            i.delete();
                                            done = false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return done;
    }

    private static class BlockLiveness {
        private final HashSet<MCOperand> use = new HashSet<>();
        private final HashSet<MCOperand> def = new HashSet<>();
        private HashSet<MCOperand> in = new HashSet<>();
        private HashSet<MCOperand> out = new HashSet<>();

        BlockLiveness(MachineBasicBlock block) {
        }
    }

    private HashMap<MachineBasicBlock, BlockLiveness> funcLivenessAnalysis(MachineFunction func) {
        var funcLivenessMap = new HashMap<MachineBasicBlock, BlockLiveness>();
        for(var bb : func.getBbList()) {
            var bbLiveness = new BlockLiveness(bb);
            funcLivenessMap.put(bb, bbLiveness);

            for(var i : bb.getInstList()) {
                for(var use : i.getUse()) {
                    if(!bbLiveness.def.contains(use)) bbLiveness.use.add(use);
                }
                for(var def : i.getDef()) {
                    if(!bbLiveness.use.contains(def)) bbLiveness.def.add(def);
                }
            }
            bbLiveness.in.addAll(bbLiveness.use);
        }

        boolean done = false;
        while(!done) {
            done = true;
            for(var bb : func.getBbList()) {
                var bbLiveness = funcLivenessMap.get(bb);
                var newOut = new HashSet<MCOperand>();

                for(var s : bb.getSuccessors()) {
                    newOut.addAll(funcLivenessMap.get(s).in);
                }

                if(!newOut.equals(bbLiveness.out)) {
                    done = false;
                    bbLiveness.out = newOut;
                    bbLiveness.in = new HashSet<>(bbLiveness.use);
                    for(var out : bbLiveness.out) {
                        if(!bbLiveness.def.contains(out)) bbLiveness.in.add(out);
                    }
                }
            }
        }

        return funcLivenessMap;
    }

    private void blockLiveRange(MachineBasicBlock bb, HashMap<MCOperand,MachineInstruction> lastDef, HashMap<MachineInstruction, MachineInstruction> lastUse) {

        for(var i : bb.getInstList()) {
            for(var use : i.getUse()) {
                if(lastDef.containsKey(use)) lastUse.put(lastDef.get(use), i);
            }
            for(var def : i.getDef()) {
                lastDef.put(def, i);
            }
            if(i instanceof Branch ||
                    (i instanceof LoadOrStore && ((LoadOrStore)i).getType() == LoadOrStore.Type.STORE) ||
                    i instanceof PushOrPop ||
                    i instanceof Comment
                    ) {  // 这里的SideEffect比较激进 可能有问题？
                lastUse.put(i, i);
            }
        }
    }

    @Override
    public void runOnCodeGen(CodeGenManager CGM) {
        boolean done = false;
        while(!done) {
            System.out.println("PeepHole");
            done = peepHoleWithoutDataflow(CGM) & peepHoleWithDataflow(CGM);
        }
    }

    @Override
    public void runOnModule(Module M) {
        throw new RuntimeException("runOnModule for a MCPass");
    }

    public static boolean isPowerOfTwo(int n) {
        if (n < 0) n = -n;
        return n > 0 && (n & (n - 1)) == 0;
    }

    public static int log2(int N) {

        // calculate log2 N indirectly
        // using log() method
        int result = (int) (Math.log(N) / Math.log(2));

        return result;
    }

    @Override
    public String getName() {
        return "PeepHole";
    }

}
