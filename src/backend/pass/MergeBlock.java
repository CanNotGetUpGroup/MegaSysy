package backend.pass;

import backend.CodeGenManager;
import backend.machineCode.Instruction.*;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import ir.Module;
import pass.MCPass;

import java.util.ArrayList;

import static backend.machineCode.Instruction.Branch.Type.Block;

public class MergeBlock extends MCPass {
    private ArrayList<MachineFunction> funcList;


    void run() {
        for (var f : funcList) {
            for (var bb : f.getBbList()) {
                // get basic block info
                int instruction_num = 0;
                boolean hasCond = false;
                boolean setState = false;
                for (var i : bb.getInstList()) {
                    if (i instanceof Comment) continue;
                    instruction_num++;
                    if (i.getCond() != null)
                        hasCond = true;
                    // TODO: check
                    if (instructionSetState(i))
                        setState = true;
                }

                if (instruction_num > 5) continue;

                for (var srcInst : bb.getPredInst()) {
                    var srcBB = srcInst.getParent();
                    // case: loop
                    if (srcBB == bb) continue;

                    // case : need state later
                    boolean needState = false;
                    for (var inst = srcInst.getInstNode().getNext().getVal();
                         inst != null; inst = inst.getInstNode().getNext().getVal()) {
                        if (instructionSetState(inst))
                            break;
                        if (inst.getCond() != null) {
                            needState = true;
                            break;
                        }
                    }
                    if (needState && setState)
                        continue;

                    if (srcInst.getCond() != null) {
                        if (hasCond)
                            continue;

                    } else { // srcInst doesn't have condition
                        for (var i : bb.getInstList()) {
                            var newInst = MachineInstruction.copyMCInstruction(srcBB, i);
                            newInst.insertBefore(srcInst);
                            if (newInst instanceof Branch && ((Branch) newInst).getType() == Block) {
                                var dest = ((Branch) newInst).getDestBB();
//                                dest.addPredInst(new );
                            }
                        }
                        bb.getPredInst().remove(srcInst);
                        srcInst.delete();
                    }
                }
                if (bb.getPredInst().isEmpty())
                    bb.getBbNode().remove();
            }
        }
    }

    boolean instructionSetState(MachineInstruction i) {
        return i instanceof LoadOrStore || i.isSetState() || i instanceof Cmp || i instanceof VMRS;
    }

    @Override
    public void runOnCodeGen(CodeGenManager CGM) {
        this.funcList = CGM.getFuncList();
        run();
    }

    @Override
    public void runOnModule(Module M) {
        throw new RuntimeException("MergeBB : shouldn't call");
    }
}
