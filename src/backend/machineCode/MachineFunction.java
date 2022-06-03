package backend.machineCode;

import backend.machineCode.Operand.Register;
import ir.*;
import util.IList;

import java.util.ArrayList;
import java.util.HashMap;

public class MachineFunction {
    private ArrayList<Argument> Arguments;
    private String name;
    private IList<MachineBasicBlock, MachineFunction> bbList;

    public void setDefined(boolean defined) {
        isDefined = defined;
    }

    private boolean isDefined = false;

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
        this.bbMap = new HashMap<>();
        this.valueMap = new HashMap<>();
    }

    @Override
    public String toString() {
        if(!isDefined) return "";
        StringBuilder sb = new StringBuilder();

        sb.append("\t.align\t2\n" +
                        "\t.arch armv7-a\n" +
                        "\t.syntax unified\n" +
                        "\t.arm\n" +
                        "\t.fpu vfp\n")
                .append("\t.global\t").append(this.name).append("\n")
                .append("\t.type\t").append(this.name).append("\t%function\n")
                .append(this.name).append(":").append("\n");

        var head = bbList.getHead();

        while (bbList.getLast() != null && head != bbList.getLast()) {
            head = head.getNext();
            var bb = head.getVal();
            sb.append(bb.toString()).append("\n");
        }
        // size
        sb.append(".size\t").append(name).append(", .-").append(name).append("\n");
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

    HashMap<BasicBlock, MachineBasicBlock> bbMap;
    HashMap<Value, Register> valueMap;

    /**
     * 得到ir中的基本块和汇编中基本块的映射HashMap
     * @return HashMap<BasicBlock, MachineBasicBlock>
     */
    public HashMap<BasicBlock, MachineBasicBlock> getBBMap (){
        return bbMap;
    }

    /**
     * 得到ir中的Value对寄存器的HashMap
     */
    public HashMap<Value, Register> getValueMap(){
        return valueMap;
    }
}
