package ir;

import ir.DerivedTypes.FunctionType;
import util.IList;
import util.IListNode;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class Function extends Constant {
    private ArrayList<Argument> Arguments;
    private Module Parent;
    private IListNode<Function, Module> funcNode;
    private IList<BasicBlock, Function> bbList;
    private BasicBlock entryBB;
    private boolean isDefined=true;

    /**
     * 生成一个Function对象
     * @param type
     * @param name
     * @param module
     * @return
     */
    public static Function create(FunctionType type,String name,Module module){
        return new Function(type, name, module);
    }

    public Function(FunctionType type, String name, Module module) {
        super(type, name);
        Parent = module;
        funcNode = new IListNode<>(this,module.getFuncList());
        bbList = new IList<>(this);
        //添加到module
        funcNode.insertIntoListEnd(Parent.getFuncList());
        Arguments=new ArrayList<>();
    }

    @Override
    public FunctionType getType() {
        return (FunctionType) super.getType();
    }

    public Type getRetType() {
        return getType().getReturnType();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(isDefined){
            sb.append("define dso_local ")
                    .append(this.getType().getReturnType())
                    .append(" @")
                    .append(this.getName())
                    .append("(");
            for(Argument arg:Arguments){
                sb.append(arg).append(",");
            }
            if (Arguments.size() != 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append(")");
        }else{
            sb.append("declare ")
                    .append(this.getType().getReturnType())
                    .append(" @")
                    .append(this.getName())
                    .append("(");
            for(int i=0;i<getType().getParamNum();i++){
                sb.append(getType().getParamType(i)).append(",");
            }
            if (getType().getParamNum() != 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append(")");
        }
        return sb.toString();
    }

    public ArrayList<Argument> getArguments() {
        return Arguments;
    }

    public void setArguments(ArrayList<Argument> arguments) {
        Arguments = arguments;
    }

    public void setParent(Module parent) {
        Parent = parent;
    }

    public Module getParent() {
        return Parent;
    }

    public IList<BasicBlock, Function> getBbList() {
        return bbList;
    }

    public void setBbList(IList<BasicBlock, Function> bbList) {
        this.bbList = bbList;
    }

    public IListNode<Function, Module> getFuncNode() {
        return funcNode;
    }

    public void setFuncNode(IListNode<Function, Module> funcNode) {
        this.funcNode = funcNode;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public void setDefined(boolean defined) {
        isDefined = defined;
    }

    public BasicBlock getEntryBB() {
        if(entryBB==null){
            entryBB=getBbList().getHead().getVal();
            entryBB.setEntryBlock(true);
        }
        return entryBB;
    }

    public void setEntryBB(BasicBlock entryBB) {
        this.entryBB = entryBB;
        entryBB.setEntryBlock(true);
    }

    //从module中删除
    public void remove(){

    }

    /**
     * @return 首个基本块
     */
    public BasicBlock front(){
        return getBbList().getFirst().getVal();
    }

    /**
     *
     * @return 最后一个基本块
     */
    public BasicBlock back(){
        return getBbList().getLast().getVal();
    }
}
