package backend.pass;

import backend.machineCode.Instruction.Arithmetic;
import backend.machineCode.Instruction.LoadImm;
import backend.machineCode.Instruction.LoadOrStore;
import backend.machineCode.Instruction.PushOrPop;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static backend.machineCode.Instruction.Arithmetic.Type.ADD;
import static backend.machineCode.Instruction.Arithmetic.Type.SUB;

public class RegAllocator {
    private ArrayList<MachineFunction> funcList;

    public RegAllocator(ArrayList<MachineFunction> funcList) {
        this.funcList = funcList;
    }

    public void run() {

        for (var func : funcList) {
            if (!func.isDefined()) continue;
            HashMap<VirtualRegister, Integer> vRegHash = new HashMap<>();
            int numOnStack = 0;
            for (var bb : func.getBbList()) {
                for (var inst : bb.getInstList()) {
                    var dest = inst.getDest();
                    var op1 = inst.getOp1();
                    var op2 = inst.getOp2();

                    if (dest instanceof VirtualRegister && !vRegHash.containsKey(dest)) {
                        vRegHash.put((VirtualRegister) dest, numOnStack);
                        numOnStack++;
                    }
                    if (op1 instanceof VirtualRegister && !vRegHash.containsKey(op1)) {
                        vRegHash.put((VirtualRegister) op1, numOnStack);
                        numOnStack++;
                    }
                    if (op2 instanceof VirtualRegister && !vRegHash.containsKey(op2)) {
                        vRegHash.put((VirtualRegister) op2, numOnStack);
                        numOnStack++;
                    }
                }
            }

            if (numOnStack > 0) {
                MachineBasicBlock firstBb = func.getBbList().getFirst().getVal();
                MachineInstruction newInst;

                // reserve space for temp variable on stack
                for (var inst : firstBb.getInstList()) {
                    if (!inst.isPrologue()) {
                        // Push FP
                        newInst = new PushOrPop(firstBb, PushOrPop.Type.Push, new MCRegister(MCRegister.RegName.r11));
                        newInst.setPrologue(true);
                        newInst.getInstNode().insertBefore(inst.getInstNode());

                        // set Frame Pointer -> Fp = sp + 4
                        newInst = new Arithmetic(firstBb, ADD, new MCRegister(MCRegister.RegName.r11), new MCRegister(MCRegister.RegName.SP), new ImmediateNumber(4));
                        newInst.setPrologue(true);
                        newInst.getInstNode().insertBefore(inst.getInstNode());

                        MCOperand c;
                        if(ImmediateNumber.isLegalImm(4 * numOnStack))
                            c = new ImmediateNumber(4 * numOnStack);
                        else{
                            c = new MCRegister(Register.Content.Int, 9);
                            new LoadImm(firstBb, (Register) c, 4 * numOnStack).getInstNode().insertBefore(inst.getInstNode());;

                        }
                        new Arithmetic(firstBb, SUB, new MCRegister(MCRegister.RegName.SP), c).getInstNode().insertBefore(inst.getInstNode());
                        break;
                    }
                }

                // release stack
                for (var bb : func.getBbList()) {
                    for (var inst : bb.getInstList()) {
                        if (inst.isEpilogue()) {

                            new Arithmetic(firstBb, SUB, new MCRegister(MCRegister.RegName.SP), new MCRegister(MCRegister.RegName.r11),
                                    new ImmediateNumber(4)).getInstNode().insertBefore(inst.getInstNode());
                            newInst = new PushOrPop(bb, PushOrPop.Type.Pop, new MCRegister(MCRegister.RegName.r11));
                            newInst.setEpilogue(true);
                            newInst.getInstNode().insertBefore(inst.getInstNode());
                        }
                    }
                }
            }

            for (var bb : func.getBbList()) {
                for (var inst : bb.getInstList()) {
                    var dest = inst.getDest();
                    var op1 = inst.getOp1();
                    var op2 = inst.getOp2();

                    if (dest instanceof VirtualRegister) {
                        Register reg;
                        if (dest.isFloat()) {
                            reg = new MCRegister(Register.Content.Float, 15);
                        } else {
                            reg = new MCRegister(Register.Content.Int, 4);
                        }
//                        System.out.println(dest instanceof  ImmediateNumber);
                        new LoadOrStore(bb, LoadOrStore.Type.STORE, reg,
                                new Address(new MCRegister(MCRegister.RegName.r11), -4 * vRegHash.get((VirtualRegister) dest) - func.getStackTop()))
                                .setForFloat(dest.isFloat(), new ArrayList<>(List.of("32")))
                                .getInstNode().insertAfter(inst.getInstNode());
                        inst.setDest(reg);
                    }
                    if (op1 instanceof VirtualRegister) {
                        Register reg;
                        if (((VirtualRegister) op1).isFloat()) {
                            reg = new MCRegister(Register.Content.Float, 16);
                        } else {
                            reg = new MCRegister(Register.Content.Int, 5);
                        }

                        new LoadOrStore(bb, LoadOrStore.Type.LOAD, reg,
                                new Address(new MCRegister(MCRegister.RegName.r11), -4 * vRegHash.get((VirtualRegister) op1) - func.getStackTop()))
                                .setForFloat(((VirtualRegister) op1).isFloat(), new ArrayList<>(List.of("32")))
                                .getInstNode().insertBefore(inst.getInstNode());
                        inst.setOp1(reg);
                    }
                    if (op2 instanceof VirtualRegister) {
                        Register reg;
                        if (((VirtualRegister) op2).isFloat()) {
                            reg = new MCRegister(Register.Content.Float, 17);
                        } else {
                            reg = new MCRegister(Register.Content.Int, 6);
                        }
                        new LoadOrStore(bb, LoadOrStore.Type.LOAD, reg,
                                new Address(new MCRegister(MCRegister.RegName.r11), -4 * vRegHash.get((VirtualRegister) op2) - func.getStackTop()))
                                .setForFloat(((VirtualRegister) op2).isFloat(), new ArrayList<>(List.of("32")))
                                .getInstNode().insertBefore(inst.getInstNode());

                        inst.setOp2(reg);

                    } else if (op2 instanceof Address && ((Address) op2).getReg() instanceof VirtualRegister) {
                        new LoadOrStore(bb, LoadOrStore.Type.LOAD, new MCRegister(MCRegister.RegName.r7),
                                new Address(new MCRegister(MCRegister.RegName.r11), -4 * vRegHash.get(((Address) op2).getReg()) - func.getStackTop()))
                                .getInstNode().insertBefore(inst.getInstNode());
                        ((Address) op2).setReg(new MCRegister(MCRegister.RegName.r7));
                    }
                    //  地址的第二个参数可能也是reg, 肯定还得是int
                    else if (op2 instanceof Address && ((Address) op2).getOffset() instanceof VirtualRegister) {
                        new LoadOrStore(bb, LoadOrStore.Type.LOAD, new MCRegister(MCRegister.RegName.r8),
                                new Address(new MCRegister(MCRegister.RegName.r11), -4 * vRegHash.get(((Address) op2).getOffset()) - func.getStackTop()))
                                .getInstNode().insertBefore(inst.getInstNode());
                        ((Address) op2).setOffset(new MCRegister(MCRegister.RegName.r8));
                    }
                }
            }
        }
    }
}
