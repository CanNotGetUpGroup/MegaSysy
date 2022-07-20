package backend.pass;

import backend.machineCode.Addressable;
import backend.machineCode.Instruction.Arithmetic;
import backend.machineCode.Instruction.LoadOrStore;
import backend.machineCode.Instruction.PushOrPop;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.Address;
import backend.machineCode.Operand.ImmediateNumber;
import backend.machineCode.Operand.MCRegister;
import backend.machineCode.Operand.VirtualRegister;


import java.util.ArrayList;
import java.util.HashMap;

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

                // reserve space for temp varible on stack
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

                        new Arithmetic(firstBb, SUB, new MCRegister(MCRegister.RegName.SP),
                                new ImmediateNumber(4 * numOnStack)).getInstNode().insertBefore(inst.getInstNode());
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
                        new LoadOrStore(bb, LoadOrStore.Type.STORE, new MCRegister(MCRegister.RegName.r4),
                                new Address(new MCRegister(MCRegister.RegName.r11), -4 * vRegHash.get((VirtualRegister) dest) - func.getStackTop()))
                                .getInstNode().insertAfter(inst.getInstNode());
                        inst.setDest(new MCRegister(MCRegister.RegName.r4));
                    }
                    if (op1 instanceof VirtualRegister ) {
                        new LoadOrStore(bb, LoadOrStore.Type.LOAD, new MCRegister(MCRegister.RegName.r5),
                                new Address(new MCRegister(MCRegister.RegName.r11), -4 * vRegHash.get((VirtualRegister) op1) - func.getStackTop()))
                                .getInstNode().insertBefore(inst.getInstNode());
                        inst.setOp1(new MCRegister(MCRegister.RegName.r5));
                    }
                    if (op2 instanceof VirtualRegister) {
                        new LoadOrStore(bb, LoadOrStore.Type.LOAD, new MCRegister(MCRegister.RegName.r6),
                                new Address(new MCRegister(MCRegister.RegName.r11), -4 * vRegHash.get((VirtualRegister) op2) - func.getStackTop()))
                                .getInstNode().insertBefore(inst.getInstNode());
                        inst.setOp2(new MCRegister(MCRegister.RegName.r6));
                    } else if (op2 instanceof Address && ((Address) op2).getReg() instanceof VirtualRegister){
                        new LoadOrStore(bb, LoadOrStore.Type.LOAD, new MCRegister(MCRegister.RegName.r7),
                                new Address(new MCRegister(MCRegister.RegName.r11), -4 * vRegHash.get(((Address) op2).getReg()) - func.getStackTop()))
                                .getInstNode().insertBefore(inst.getInstNode());
                        ((Address) op2).setReg(new MCRegister(MCRegister.RegName.r7));
                    }
                    // TODO: 地址的第二个参数可能也是reg
                    else if (op2 instanceof Address && ((Address) op2).getOffset() instanceof VirtualRegister){
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
