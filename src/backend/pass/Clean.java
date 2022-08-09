package backend.pass;

import backend.machineCode.Instruction.Comment;
import backend.machineCode.Instruction.Move;
import backend.machineCode.MachineFunction;

import java.util.ArrayList;

public class Clean {
    private ArrayList<MachineFunction> funcList;

    public Clean(ArrayList<MachineFunction> funcList) {
        this.funcList = funcList;
    }

    public void run() {
        for(var f: funcList){
            for(var bb: f.getBbList()){
                for(var i: bb.getInstList()){
                    if(i instanceof Comment)
                        i.getInstNode().remove();
                    else if(i instanceof Move && i.getDest().toString().equals(i.getOp2().toString()))
                        i.getInstNode().remove();
                }
            }
        }
    }
}
