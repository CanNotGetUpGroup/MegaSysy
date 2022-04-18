package ir;

import util.IList;

import java.util.ArrayList;

public class Module {
    private ArrayList<GlobalVariable> globalVariables;
    private IList<Function, Module> funcList;
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
}
