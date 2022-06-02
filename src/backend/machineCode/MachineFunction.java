package backend.machineCode;

import ir.*;
import ir.Module;
import util.IList;
import util.IListNode;

import java.util.ArrayList;

public class MachineFunction {
    private ArrayList<Argument> Arguments;
    private String name;
    private IList<MachineBasicBlock, MachineFunction> bbList;
    private boolean isDefined = true;

    /**
     * 生成一个MachineFunction对象
     *
     * @param name
     * @return
     */
    public MachineFunction(String name) {
        bbList = new IList<>(this);
        Arguments = new ArrayList<>();
        this.name = name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // TODO: print directives for functions

        sb.append(this.name).append(":");
        return sb.toString();
    }

    public ArrayList<Argument> getArguments() {
        return Arguments;
    }

    public void setArguments(ArrayList<Argument> arguments) {
        Arguments = arguments;
    }

    public IList<MachineBasicBlock, MachineFunction> getBbList() {
        return bbList;
    }

    public void setBbList(IList<MachineBasicBlock, MachineFunction> bbList) {
        this.bbList = bbList;
    }

    public String getName(){
        return name;
    }
}
