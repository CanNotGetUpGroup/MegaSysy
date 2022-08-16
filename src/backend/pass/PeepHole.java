package backend.pass;

import backend.machineCode.Instruction.*;
import backend.CodeGenManager;
import backend.machineCode.MachineFunction;
import backend.machineCode.Operand.Address;
import backend.machineCode.Operand.ImmediateNumber;
import backend.machineCode.Operand.Register;
import ir.Module;
import pass.MCPass;

import java.util.ArrayList;

public class PeepHole extends MCPass{

    public PeepHole() {
        super();
    }

    private boolean peepHoleWithoutDataflow(CodeGenManager CGM) {
        boolean done = true;
        for(var f : CGM.getFuncList()) {
            if (!f.isDefined()) continue;
            for(var bb : f.getBbList()) {
                // 消除bb尾无效跳转
                var lastInst = bb.getInstList().getLast().getVal();
                if (lastInst instanceof Branch && lastInst.getCond() == null) {
                    if (((Branch) lastInst).getDestBB() == bb.getBbNode().getNext().getVal()) {
                        lastInst.delete();
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
                            new Move(bb, next.getOp1(), i.getDest()).insertBefore(next);
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
            
            for (var bb : f.getBbList()) {
                for (var next : bb.getInstList()) {
                    var iNode = next.getInstNode().getPrev();
                    if (iNode == null) continue;
                    var i = iNode.getVal();

                    // add/sub ldr/str
                    // ADD r1, r2, 4
                    // LDR r0, [ r1, 8 ] -> LDR r0, [ r2, 4+8 ]
                    if( !i.hasShift()
                        && i instanceof Arithmetic 
                        && (((Arithmetic) i).getType() == Arithmetic.Type.ADD || ((Arithmetic) i).getType() == Arithmetic.Type.SUB)
                        && i.getOp2() instanceof ImmediateNumber
                        // TODO 需要做数据流分析确定LDR是r1的最后一个user
                            ) {

                    }
                }
            }
        }

        return done;
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
