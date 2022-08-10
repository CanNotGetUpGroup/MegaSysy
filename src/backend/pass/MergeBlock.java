package backend.pass;

import backend.machineCode.MachineFunction;

import java.util.ArrayList;

public class MergeBlock{
    private ArrayList<MachineFunction> funcList;

    public MergeBlock(ArrayList<MachineFunction> funcList) {
        this.funcList = funcList;
    }

    void run(){
        for(var f : funcList){
            for (var bb : f.getBbList()){
                // get basic block info
                int instruction_num  = 0;
                boolean hasCond = false;
//                for(var )

            }
        }
    }

}
