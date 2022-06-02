package backend.pass;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import ir.BasicBlock;
import ir.Function;
import util.IList;

import java.util.ArrayList;
import java.util.HashMap;

import ir.Module;

public class InstructionSelector {
    private Module module;
    private ArrayList<MachineFunction> funcList;

    public InstructionSelector(Module module) {
        this.module = module;
        funcList = new ArrayList<>();
    }

    public ArrayList<MachineFunction> getFuncList() {
        return funcList;
    }

    public void run() {
        // get ir function to machine function map
        var irFuncList = module.getFuncList();
        var head = irFuncList.getHead();
        while (irFuncList.getLast() != null && head != irFuncList.getLast()) {
            head = head.getNext();
            var f = head.getVal();
            MachineFunction mf = new MachineFunction(f.getName());
            funcList.add(mf);
            funcMap.put(f, mf);
        }


    }

    private HashMap<Function, MachineFunction> funcMap = new HashMap<>();

    private void translateFunction(Function irFunction) {
        MachineFunction mf = funcMap.get(irFunction);

        HashMap<BasicBlock, MachineBasicBlock> bbMap = new HashMap<>();
        var bbList = irFunction.getBbList();
        var head = bbList.getHead();

        while (bbList.getLast() != null && head != bbList.getLast()) {
            head = head.getNext();
            var bb = head.getVal();
            MachineBasicBlock mbb = new MachineBasicBlock(mf);
            mbb.pushBacktoBBList();
            bbMap.put(bb, mbb);
        }
    }

    private void translateBB(BasicBlock bb, HashMap<BasicBlock, MachineBasicBlock> bbMap){
        MachineBasicBlock mbb = bbMap.get(bb);

        var instList = bb.getInstList();
        var head = instList.getHead();

        while (instList.getLast() != null && head != instList.getLast()) {
            head = head.getNext();
            var inst = head.getVal();


        }
    }

}
