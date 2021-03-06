package ir;

import ir.instructions.Instructions;
import util.IList;

import java.util.ArrayList;
import java.util.Iterator;

public class Module {
    private final ArrayList<GlobalVariable> globalVariables; //全局变量
    private final IList<Function, Module> funcList; //函数
    private static final Module module = new Module();

    private Module() {
        this.globalVariables = new ArrayList<>();
        this.funcList = new IList<>(this);
    }

    public static Module getInstance() { return module; };

    public ArrayList<GlobalVariable> getGlobalVariables() {
        return globalVariables;
    }

    public IList<Function, Module> getFuncList() {
        return funcList;
    }

    public void rename(){
        for (Function F : funcList) {
            int namePtr = 0;
            if (F.isDefined()) {
                for (Argument argument : F.getArguments()) {
                    argument.setName("%" + namePtr++);
                }
                for (BasicBlock BB : F.getBbList()) {
                    BB.setName(String.valueOf(namePtr++));
                    Iterator<Instruction> instItr=BB.getInstList().iterator();
                    while (instItr.hasNext()) {
                        Instruction I=instItr.next();
                        if (!I.getType().isVoidTy()) {
                            I.setName("%" + namePtr++);
                        } else if (I instanceof Instructions.BranchInst || I instanceof Instructions.ReturnInst) {
                            I.getInstNode().cutFollow(instItr);
                        }
                    }
                }
            }
        }
    }

    public String toLL(){
        StringBuilder sb=new StringBuilder();
        ArrayList<GlobalVariable> globalVariables=getGlobalVariables();
        IList<Function, Module> funcList = getFuncList();
        for(GlobalVariable g:globalVariables){
            sb.append(g).append("\n");
        }
        if(!globalVariables.isEmpty()) sb.append("\n");

        for (Function F : funcList){
            if(F.isDefined()){
                sb.append("\n").append(F).append("{\n");
                boolean init=true;
                for (BasicBlock BB : F.getBbList()){
                    if(!init){
                        sb.append("\n").append(BB).append("     ").append(BB.getComment()!=null?BB.getComment():"").append("\n");
                    }else{
                        init=false;
                    }
                    for (Instruction I: BB.getInstList()){
                        sb.append("  ").append(I).append("     ").append(I.getComment()!=null?I.getComment():"").append("\n");
                    }
                }
                sb.append("}").append("\n");
            }else{
                sb.append(F).append("\n");
            }
        }
        return sb.toString();
    }
}
