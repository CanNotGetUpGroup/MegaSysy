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

    public Function(FunctionType type, String name, Module module) {
        super(type, name);
        Parent = module;
        funcNode = new IListNode<>(this);
        bbList = new IList<>(this);
        //添加到module
        funcNode.insertIntoListEnd(Parent.getFuncList());
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
}
