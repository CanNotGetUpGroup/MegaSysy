package backend.pass;

import backend.CodeGenManager;
import backend.machineCode.Instruction.*;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import ir.Module;
import pass.MCPass;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

import static backend.machineCode.Instruction.Branch.Type.Block;
import static backend.machineCode.Instruction.Branch.Type.Call;

public class MergeBlock extends MCPass {
    private ArrayList<MachineFunction> funcList;


    void mergeSingleBranch() {
        for (var f : funcList) {
            for (var bb : f.getBbList()) {
                // get basic block info
                int instruction_num = 0;
                boolean hasCond = false;
                boolean setState = false;
                for (var i : bb.getInstList()) {
                    if (i instanceof Comment) continue;
                    instruction_num++;
                    if (i.getCond() != null) hasCond = true;
                    if(i instanceof Branch && ((Branch) i).getType() == Call)
                        hasCond = true;
                    // TODO: check
                    if (instructionSetState(i)) setState = true;
                }

                if (instruction_num > 5) continue;

                // case : .1: br .2, .3 ;; .2: br .3
                if (bb.getSuccessors().size() == 1 && bb.getPredInst().size() == 1 && !hasCond && !setState) {
                    var predInst = bb.getPredInst().iterator().next();
                    var pred = bb.getPredInst().iterator().next().getParent();
                    var brothers = new HashSet<>(pred.getSuccessors());
                    var brother = brothers.stream().filter(x -> x != bb).findFirst().get();
                    var brotherInst = brother.getPredInst().stream().filter(x -> x.getParent() == pred).findFirst().get();
                    if (bb.getSuccessors().iterator().next() == brother) {
                        MachineInstruction.Condition cond;
                        if (predInst.getCond() != null) {
                            cond = predInst.getCond();
                        } else {
                            cond = MachineInstruction.Condition.getOpposite(brotherInst.getCond());
                        }
                        pred.getSuccessors().remove(bb);
                        brother.getPredInst().remove(brotherInst);
                        brother.getPredInst().removeIf(x -> x.getParent() == bb);

                        predInst.delete();
                        brotherInst.delete();

                        for (var i : bb.getInstList()) {
                            if (i.getInstNode().getNext() == null) break;
                            var newInst = MachineInstruction.copyMCInstruction(pred, i);
                            newInst.pushBacktoInstList();
                            newInst.setCond(cond);
                        }
                        var newInst = MachineInstruction.copyMCInstruction(pred, bb.getInstList().getLast().getVal());
                        newInst.pushBacktoInstList();

                        brother.addPredInst(newInst);
                        bb.getBbNode().remove();
                    }
                }
            }
        }
    }

    void changBranchOrder() {
        for (var f : funcList) {
            for (var bb : f.getBbList()) {
                if(bb.getBbNode().getNext() == null)
                    continue;
                // get basic block info
                var succ = bb.getSuccessors();
                if (succ.size() != 2) continue;
                var succArr = succ.stream().collect(Collectors.toList());
                var succ1 = succArr.get(0);
                var succ2 = succArr.get(1);
                var nextBB = bb.getBbNode().getNext().getVal();
                var predInst1 = succ1.getPredInst().stream().filter(x -> x.getParent() == bb).findFirst().get();
                var predInst2 = succ2.getPredInst().stream().filter(x -> x.getParent() == bb).findFirst().get();
                if (predInst2 != bb.getInstList().getLast().getVal()) {
                    var i = predInst2;
                    predInst2 = predInst1;
                    predInst1 = i;
                    var s = succ1;
                    succ1 = succ2;
                    succ2 = s;
                }
                if (predInst2 != bb.getInstList().getLast().getVal()) continue;
                if (nextBB != succ1) continue; // 没必要交换

                int instruction_num = 0;
                boolean hasCond = false;
                boolean setState = false;
                for (var i = predInst1.getNext(); i != predInst2; i = i.getNext()) {
                    if (i instanceof Comment) continue;
                    instruction_num++;
                    if (i.getCond() != null) hasCond = true;
                    // TODO: check
                    if (instructionSetState(i)) setState = true;
                }
                if (hasCond || setState || instruction_num >= 5) continue;
                var cond = MachineInstruction.Condition.getOpposite(predInst1.getCond());
                for (var i = predInst1.getNext(); i != predInst2; i = i.getNext()) {
                    i.setCond(cond);
                }
                predInst2.insertAfter(predInst1);
                predInst1.pushBacktoInstList();
                predInst2.setCond(cond);
                predInst1.setCond(null);
            }
        }
    }

    // 可以把基本块直接复制到当前块的末尾
    void addToBack(){

    }

//    void hasSingle

    boolean instructionSetState(MachineInstruction i) {
        return i instanceof LoadOrStore || i.isSetState() || i instanceof Cmp || i instanceof VMRS;
    }

    @Override
    public void runOnCodeGen(CodeGenManager CGM) {
        this.funcList = CGM.getFuncList();
        mergeSingleBranch();
        changBranchOrder();
    }

    @Override
    public void runOnModule(Module M) {
        throw new RuntimeException("MergeBB : shouldn't call");
    }
}
