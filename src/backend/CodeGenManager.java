package backend;

import backend.machineCode.MachineFunction;
import ir.Function;
import ir.GlobalVariable;
import ir.Module;
import util.IList;

import java.util.ArrayList;

public class CodeGenManager {

    private static Module module;
    private static CodeGenManager codeGenManager;

//    private final ArrayList<GlobalVariable> globalVariables;
    private final ArrayList<MachineFunction> funcList;
    private CodeGenManager(){
        funcList = new ArrayList<>();
    }

    public CodeGenManager getInstance(){
        if(codeGenManager == null){
            codeGenManager = new CodeGenManager();
        }
        return codeGenManager;
    }



}
