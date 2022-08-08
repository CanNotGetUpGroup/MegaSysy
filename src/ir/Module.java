package ir;

import ir.instructions.Instructions;
import util.IList;

import java.util.ArrayList;
import java.util.Iterator;

public class Module {
    private final ArrayList<GlobalVariable> globalVariables; //全局变量
    private final IList<Function, Module> funcList; //函数
    private static final Module module = new Module();
    private static final boolean openComment=false;

    private Module() {
        this.globalVariables = new ArrayList<>();
        this.funcList = new IList<>(this);
    }

    public static Module getInstance() {
        return module;
    }

    ;

    public ArrayList<GlobalVariable> getGlobalVariables() {
        return globalVariables;
    }

    public IList<Function, Module> getFuncList() {
        return funcList;
    }

    /**
     * 将ir按照数字顺序重新命名
     */
    public void rename() {
        for (Function F : funcList) {
            rename(F);
        }
    }

    public void rename(Function F) {
        int namePtr = 0;
        if (F.isDefined()) {
            for (Argument argument : F.getArguments()) {
                if (argument.getName().isEmpty() || argument.getName().substring(1).matches("[0-9]+"))
                    argument.setName("%" + namePtr++);
            }
            for (BasicBlock BB : F.getBbList()) {
                if (BB.getName().isEmpty() || BB.getName().matches("[0-9]+"))
                    BB.setName(String.valueOf(namePtr++));
                Iterator<Instruction> instItr = BB.getInstList().iterator();
                while (instItr.hasNext()) {
                    Instruction I = instItr.next();
                    //指令有返回值并且没有名字或是有数字名字，需要更新时，进行重命名
                    if (!I.getType().isVoidTy() && (I.getName().isEmpty() || I.getName().substring(1).matches("[0-9]+"))) {
                        I.setName("%" + namePtr++);
                    } else if (I instanceof Instructions.BranchInst || I instanceof Instructions.ReturnInst) {
                        while (instItr.hasNext()){
                            instItr.next().remove();
                        }
                    }
                }
            }
        }
    }

    public String toLL() {
        StringBuilder sb = new StringBuilder();
        ArrayList<GlobalVariable> globalVariables = getGlobalVariables();
        IList<Function, Module> funcList = getFuncList();
        for (GlobalVariable g : globalVariables) {
            sb.append(g).append("\n");
        }
        if (!globalVariables.isEmpty()) sb.append("\n");

        for (Function F : funcList) {
            sb.append(toLL(F));
        }
        return sb.toString();
    }

    public String toLL(Function F) {
        StringBuilder sb = new StringBuilder();

        if (F.isDefined()) {
            sb.append("\n").append(F).append("{\n");
            boolean init = true;
            for (BasicBlock BB : F.getBbList()) {
                if (!init) {
                    sb.append("\n").append(BB).append("     ").append(openComment && BB.getComment() != null ? BB.getComment() : "").append("\n");
                } else {
                    sb.append(BB).append("     ").append(openComment && BB.getComment() != null ? BB.getComment() : "").append("\n");
                    init = false;
                }
                for (Instruction I : BB.getInstList()) {
                    sb.append("  ").append(I).append("     ").append(openComment && I.getComment() != null ? I.getComment() : "").append("\n");
                }
            }
            sb.append("}").append("\n");
        } else {
            sb.append(F).append("\n");
        }
        return sb.toString();
    }
}
