package backend.pass;

import backend.machineCode.Instruction.*;
import backend.machineCode.MachineFunction;
import backend.machineCode.Operand.Address;
import backend.machineCode.Operand.ImmediateNumber;
import backend.machineCode.Operand.Register;

import java.util.ArrayList;

public class PeepHole {
    private ArrayList<MachineFunction> funcList;

    public PeepHole(ArrayList<MachineFunction> funcList) {
        this.funcList = funcList;
    }

    public void run() {
        for (var f : funcList) {
            if (!f.isDefined()) continue;
            // 消除跳转
            for (var bb : f.getBbList()) {
                var inst = bb.getInstList().getLast().getVal();
                if (inst instanceof Branch && inst.getCond() == null) {
                    if (((Branch) inst).getDestBB() == bb.getBbNode().getNext().getVal()) {
                        inst.delete();
                    }
                }
            }
            //
            for (var bb : f.getBbList()) {
                // 	mov	r0, 4
                //	MUL	r0, r4, r0
                for (var next : bb.getInstList()) {
                    var iNode = next.getInstNode().getPrev();
                    if (iNode == null) continue;
                    var i = iNode.getVal();
                    if ((i instanceof Move || i instanceof LoadImm) && i.getOp2() instanceof ImmediateNumber
                            && isPowerOfTwo(((ImmediateNumber) i.getOp2()).getValue())
                            && next instanceof Arithmetic && ((Arithmetic) next).getType() == Arithmetic.Type.MUL) {
                        var r4 = i.getDest();
                        var num = ((ImmediateNumber) i.getOp2()).getValue();
                        var other = (Register) next.getOp1();
                        if (other.equals(r4))
                            other = (Register) next.getOp2();
                        if (next.getOp2().equals(r4) || next.getOp1().equals(r4)) {
                            new Arithmetic(bb, Arithmetic.Type.LSL, next.getDest(), other, new ImmediateNumber(log2(num)))
                                    .insertBefore(i);
                            i.delete();
                            next.delete();
                        }
                    }
                }
            }
            for (var bb : f.getBbList()) {
//                ADD r0, r6, #4
//                LDR r0, [ r0 ]

                }

            }

        }
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


}
