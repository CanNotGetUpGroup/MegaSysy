package backend.pass;

import backend.machineCode.MachineFunction;
import ir.Function;


import java.util.ArrayList;

public class RegAllocator {
    private ArrayList<MachineFunction> funcList;

    public RegAllocator(ArrayList<MachineFunction> funcList){
        this.funcList = funcList;
    }

    public void run(){
        for(var func: funcList){
            for(var block : func.getBbList())
        }
    }
}
