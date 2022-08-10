package backend.pass;

import backend.machineCode.Instruction.Arithmetic;
import backend.machineCode.Instruction.LoadImm;
import backend.machineCode.Instruction.PushOrPop;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.ImmediateNumber;
import backend.machineCode.Operand.MCRegister;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

// 我感觉会有bug， 换个写法
public class MergePush {
    private ArrayList<MachineFunction> funcList;

    public MergePush(ArrayList<MachineFunction> funcList) {
        this.funcList = funcList;
    }

    public void run() {
        for (var f : funcList) {
            if(! f.isDefined()) continue;
            HashSet<MachineInstruction> mayDelete = new HashSet<>();
            int subStackSize = 0;
            for (var bb : f.getBbList()) {
                for (var i : bb.getInstList()) {
                    if (!i.isPrologue()
                            && i instanceof Arithmetic && ((Arithmetic) i).getType() == Arithmetic.Type.SUB) {
                        // sub sp, sp, #imm
                        if (Objects.equals(i.getDest(), new MCRegister(MCRegister.RegName.SP))
                                && Objects.equals(i.getOp1(), new MCRegister(MCRegister.RegName.SP))
                                && i.getOp2() instanceof ImmediateNumber
                                && !i.isSetState() && i.getCond() == null && i.getShifter() == null) {
                            subStackSize += ((ImmediateNumber) i.getOp2()).getValue();
//                            i.getInstNode().remove();
                            mayDelete.add(i);
                        }
                    }
                }
            }
            var firstBb = f.getBbList().getFirst().getVal();
            MachineInstruction firstSubStack = null;
            // get fist sub stack
            for (var i : firstBb.getInstList()) {
                if (i instanceof Arithmetic && ((Arithmetic) i).getType() == Arithmetic.Type.SUB
                        && Objects.equals(i.getDest(), new MCRegister(MCRegister.RegName.SP))
                        && Objects.equals(i.getOp1(), new MCRegister(MCRegister.RegName.SP))
                        && i.getOp2() instanceof ImmediateNumber
                        && !i.isSetState() && i.getCond() == null && i.getShifter() == null){
                    firstSubStack = i;
                    break;
                }
            }
            if(firstSubStack != null){
                int subSize = ((ImmediateNumber)firstSubStack.getOp2()).getValue() + subStackSize;
                if(ImmediateNumber.isLegalImm(subSize)){
                    firstSubStack.setOp2(new ImmediateNumber(subSize));
                    mayDelete.stream().forEach(i -> i.getInstNode().remove());
                } else {
                    MCRegister canUse = null;
                    for(var i : firstBb.getInstList()){
                        if(! i.isPrologue()) break;
                        if(i instanceof PushOrPop
                                && ((PushOrPop) i).getType() == PushOrPop.Type.Push
                                && ((PushOrPop) i).getOp2() instanceof MCRegister
                                && ((MCRegister) ((PushOrPop) i).getOp2()).getId() <= 10
                        )
                            canUse = ((MCRegister) ((PushOrPop) i).getOp2());
                    }
                    if(canUse != null){
                        var inst = new LoadImm(firstBb, canUse, subSize);
                        inst.insertBefore(firstSubStack.getInstNode());
                        inst.setPrologue(true);
                        firstSubStack.setOp2(canUse);
                        mayDelete.stream().forEach(i -> i.getInstNode().remove());
                    }

                    var size = (int) Math.ceil(Math.log(subStackSize) / Math.log(2));
                    size = (int) Math.pow(2, size);

                    // TODO : could be source of bugs

//                    throw new RuntimeException("Can't do" + size);
//                    if(firstSubStack)
//                    firstSubStack.setOp2();
                }
            }
        }
    }

}

