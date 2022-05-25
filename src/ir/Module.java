package ir;

import ir.instructions.Instructions;
import util.IList;

import java.util.ArrayList;

public class Module {
    private final ArrayList<GlobalVariable> globalVariables;
    private final IList<Function, Module> funcList;
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
        var first=funcList.getHead();
        while(funcList.getLast()!=null&&first!=funcList.getLast()){
            first=first.getNext();
            var F=first.getVal();
            int namePtr=0;
            if(F.isDefined()){
                for(Argument argument:F.getArguments()){
                    argument.setName("%"+namePtr++);
                }
                var first_block=F.getBbList().getHead();
                while ((F.getBbList().getLast()!=null&&first_block!=F.getBbList().getLast())){
                    first_block=first_block.getNext();
                    var BB=first_block.getVal();
                    BB.setName(String.valueOf(namePtr++));
                    var first_inst=BB.getInstList().getHead();
                    while(BB.getInstList().getLast()!=null&&first_inst!=BB.getInstList().getLast()){
                        first_inst=first_inst.getNext();
                        var I=first_inst.getVal();
                        if(!I.getType().isVoidTy()){
                            I.setName("%"+namePtr++);
                        }
                        else if(I instanceof Instructions.BranchInst||I instanceof Instructions.ReturnInst){
                            first_inst.cutFollow();
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

        var first=funcList.getHead();
        while(funcList.getLast()!=null&&first!=funcList.getLast()){
            first=first.getNext();
            var F=first.getVal();
            if(F.isDefined()){
                sb.append("\n").append(F).append("{\n");
                var first_block=F.getBbList().getHead();
                boolean init=true;
                while ((F.getBbList().getLast()!=null&&first_block!=F.getBbList().getLast())){
                    first_block=first_block.getNext();
                    var BB=first_block.getVal();
                    if(!init){
                        sb.append("\n").append(BB).append("\n");
                    }else{
                        init=false;
                    }
                    var first_inst=BB.getInstList().getHead();
                    while(BB.getInstList().getLast()!=null&&first_inst!=BB.getInstList().getLast()){
                        first_inst=first_inst.getNext();
                        var I=first_inst.getVal();
                        sb.append("  ").append(I).append("\n");
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
