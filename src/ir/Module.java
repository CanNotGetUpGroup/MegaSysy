package ir;

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
        var last=funcList.getLast();
        while(last!=null&&first!=last){
            first=first.getNext();
            var F=first.getVal();
            int namePtr=0;
            if(F.isDefined()){
                for(Argument argument:F.getArguments()){
                    argument.setName("%"+namePtr++);
                }
                var first_block=F.getBbList().getHead();
                var last_block=F.getBbList().getLast();
                while ((last_block!=null&&first_block!=last_block)){
                    first_block=first_block.getNext();
                    var BB=first_block.getVal();
                    BB.setName(String.valueOf(namePtr++));
                    var first_inst=BB.getInstList().getHead();
                    var last_inst=BB.getInstList().getLast();
                    while(last_inst!=null&&first_inst!=last_inst){
                        first_inst=first_inst.getNext();
                        var I=first_inst.getVal();
                        if(!I.getType().isVoidTy())
                            I.setName("%"+namePtr++);
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
        sb.append("\n");

        var first=funcList.getHead();
        var last=funcList.getLast();
        while(last!=null&&first!=last){
            first=first.getNext();
            var F=first.getVal();
            if(F.isDefined()){
                sb.append("\n").append(F).append("{\n");
                var first_block=F.getBbList().getHead();
                var last_block=F.getBbList().getLast();
                boolean init=true;
                while ((last_block!=null&&first_block!=last_block)){
                    first_block=first_block.getNext();
                    var BB=first_block.getVal();
                    if(!init){
                        sb.append("\n").append(BB).append("\n");
                    }else{
                        init=false;
                    }
                    var first_inst=BB.getInstList().getHead();
                    var last_inst=BB.getInstList().getLast();
                    while(last_inst!=null&&first_inst!=last_inst){
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
