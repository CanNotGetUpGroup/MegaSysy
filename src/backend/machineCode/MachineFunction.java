package backend.machineCode;

import analysis.LoopInfo;
import backend.machineCode.Operand.Register;
import ir.*;
import util.IList;

import java.util.ArrayList;
import java.util.HashMap;

public class MachineFunction implements Addressable{
    private ArrayList<Argument> Arguments;
    private String name;
    private IList<MachineBasicBlock, MachineFunction> bbList;
    private boolean isDefined = false;
    private boolean isLeaf = true;

    public int getMaxParaNumOnStack() {
        return maxParaNumOnStack;
    }

    public void setMaxParaNumOnStack(int maxParaNumOnStack) {
        this.maxParaNumOnStack = maxParaNumOnStack;
    }

    private int maxParaNumOnStack = 0;

    public int getSpiltNumOnStack() {
        return spiltNumOnStack;
    }

    public void setSpiltNumOnStack(int spiltNumOnStack) {
        this.spiltNumOnStack = spiltNumOnStack;
    }

    public void addSpliteNumOnStack(int inc){
        this.spiltNumOnStack += inc;
    }

    private int spiltNumOnStack = 0;

    public int getStackSize() {
        return stackSize;
    }
    public void addStackSize(int inc){
        this.stackSize += inc;
    }
    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    private int stackSize = 0; // ir的store指令存的东西


    public HashMap<Register, Integer> getStoredValueMap() {
        return storedValueMap;
    }

    private HashMap<Register, Integer> storedValueMap = new HashMap<>();


    public int getStoredRegisterNum() {
        return storedRegisterNum;
    }

    public void setStoredRegisterNum(int storedRegisterNum) {
        this.storedRegisterNum = storedRegisterNum;
    }
    public void addStoredRegisterNum(int storedRegisterNum) {
        this.storedRegisterNum += storedRegisterNum;
    }

    int storedRegisterNum = 0;



    public void setDefined(boolean defined) {
        isDefined = defined;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public LoopInfo getLoopInfo() {
        return loopInfo;
    }

    public void setLoopInfo(LoopInfo loopInfo) {
        this.loopInfo = loopInfo;
    }

    private LoopInfo loopInfo;

    public void setLeaf(boolean isLeaf) {
        this.isLeaf = isLeaf;
    }


    /**
     * 生成一个MachineFunction对象
     *
     * @param name
     * @return MachineFunction对象
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
        if (!isDefined) return "";
        StringBuilder sb = new StringBuilder();

        sb.append("\t.align\t2\n")
                .append("\t.arch armv7ve\n")
                .append("\t.syntax unified\n")
                .append("\t.arm\n")
                .append("\t.fpu vfpv4\n")
                .append("\t.global\t").append(this.name).append("\n")
                .append("\t.type\t").append(this.name).append(", %function\n")
                .append(this.name).append(":").append("\n");

        var head = bbList.getHead();

        while (bbList.getLast() != null && head != bbList.getLast()) {
            head = head.getNext();
            var bb = head.getVal();
            sb.append(bb.toString()).append("\n");
        }
        // size
        sb.append("\t.size\t").append(name).append(", .-").append(name).append("\n");
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

    public String getLabel() {
        return name;
    }

    private HashMap<BasicBlock, MachineBasicBlock> bbMap;

    private HashMap<Value, Register> valueMap = new HashMap<>();

    public HashMap<Register, Integer> getAllocaMap() {
        return allocaMap;
    }


    private HashMap<Register, Integer> allocaMap = new HashMap<>();// could simplify some address , some immediate number

    public HashMap<Value, Integer> getStackMap() {
        return stackMap;
    }

    public void setStackMap(HashMap<Value, Integer> stackMap) {
        this.stackMap = stackMap;
    }

    private HashMap<Value, Integer> stackMap;

    /**
     * 得到ir中的基本块和汇编中基本块的映射HashMap
     *
     * @return HashMap<BasicBlock, MachineBasicBlock>
     */
    public HashMap<BasicBlock, MachineBasicBlock> getBBMap() {
        return bbMap;
    }

    /**
     * 得到ir中的Value对寄存器的HashMap
     */
    public HashMap<Value, Register> getValueMap() {
        return valueMap;
    }
}
