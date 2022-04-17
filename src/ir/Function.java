package ir;

import util.IList;
import util.IListNode;

import java.util.ArrayList;

public class Function extends Value {
    private ArrayList<Argument> Arguments;
    private Module Parent;
    private IList<BasicBlock, Function> bbList;
    private IListNode<Function, Module> funcNode;

    public Function(Type type, String name, Module module) {
        super(type, name);
        //TODO: 添加到module

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
