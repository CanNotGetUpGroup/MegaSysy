package backend.pass;

import backend.machineCode.Instruction.Move;
import backend.machineCode.Instruction.Phi;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;
import backend.machineCode.Operand.VirtualRegister;
import ir.Constant;
import ir.Constants;
import ir.Function;
import util.IList;
import util.IListNode;

import java.util.ArrayList;

public class PhiElimination {
    private ArrayList<MachineFunction> funcList;

    public PhiElimination(ArrayList<MachineFunction> funcList) {
        this.funcList = funcList;
    }

    public void run() {
        for (var f : funcList) {
            for (var mbb : f.getBbList()) {



                for (var inst : mbb.getInstList()) {
                    if (inst instanceof Phi) {
                        var ir = ((Phi) inst).getIr();
                        var tempReg = new VirtualRegister(inst.isForFloat() ? Register.Content.Float : Register.Content.Int);
                        var dest = inst.getDest();
                        var valueMap = f.getValueMap();

                        for (int i = 0; i < ir.getNumOperands(); i++) {
                            var bb = ir.getIncomingBlock(i);
                            var val = ir.getIncomingValue(i);
                            MCOperand op;
                            // find end for the origin block
                            IListNode<MachineInstruction, MachineBasicBlock> node = mbb.getInstList().getTail();
                            for (var inst1 : f.getBBMap().get(bb).getInstList()) {
                                if (inst1.isforBr()) {
                                    node = inst1.getInstNode();
                                    break;
                                }
                            }
                            if (inst.isForFloat()) {
                                op = InstructionSelector.valueToFloatRegInsertBefore(mbb, val, node);
                            } else {
                                op = InstructionSelector.valueToMCOperandInsertBefore(mbb, val, node);
                            }
                            var a = new Move(f.getBBMap().get(bb), tempReg, op).setForFloat(inst.isForFloat());
                            System.out.println(a);
                            a.insertBefore(node);
                        }
                        new Move(mbb, dest, tempReg).setForFloat(inst.isForFloat()).pushtofront();
                        inst.getInstNode().remove();
                    }
                }
            }
        }
    }
}
