package ir;

import util.IList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Module {
    private ArrayList<GlobalValue> globalValues;
    private IList<Function, Module> funcList;

    public Module() {
        this.globalValues = new ArrayList<>();
        this.funcList = new IList<>(this);

    }

    public ArrayList<GlobalValue> getGlobalValues() {
        return globalValues;
    }

    public IList<Function, Module> getFuncList() {
        return funcList;
    }
}
