package backend.pass;

import backend.machineCode.Instruction.Arithmetic;
import backend.machineCode.Instruction.Comment;
import backend.machineCode.Instruction.Move;
import backend.machineCode.MachineFunction;
import backend.machineCode.Operand.ImmediateNumber;

import java.util.ArrayList;

public class Clean {
    private ArrayList<MachineFunction> funcList;

    public Clean(ArrayList<MachineFunction> funcList) {
        this.funcList = funcList;
    }

    public void run() {
        for (var f : funcList) {
            for (var bb : f.getBbList()) {
                for (var i : bb.getInstList()) {
                    if (i instanceof Comment){
                        // case : comment
//                        i.getInstNode().remove();
                    }
                    else if (i instanceof Move && i.getDest().toString().equals(i.getOp2().toString())){
                        // case : mov r0, r0
                        i.getInstNode().remove();
                    }

                }
            }
        }
    }
}
