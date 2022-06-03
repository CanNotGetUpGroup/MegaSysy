package backend.pass;

import backend.machineCode.Instruction.Move;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.ImmediateNumber;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.MCRegister;
import ir.*;
import ir.Module;

import java.util.ArrayList;
import java.util.HashMap;

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

            if (f.isDefined())
                mf.setDefined(true);

            funcList.add(mf);
            funcMap.put(f, mf);
        }

        head = irFuncList.getHead();
        while (irFuncList.getLast() != null && head != irFuncList.getLast()) {
            head = head.getNext();
            var f = head.getVal();

            if (!f.isDefined()) continue;
            translateFunction(f);
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
        head = bbList.getHead();
        while (bbList.getLast() != null && head != bbList.getLast()) {
            head = head.getNext();
            var bb = head.getVal();

            translateBB(bb);
        }
    }

    private void translateBB(BasicBlock bb) {

        var bbMap = funcMap.get(bb.getParent()).getBBMap();
        MachineBasicBlock mbb = bbMap.get(bb);

        var instList = bb.getInstList();
        var head = instList.getHead();

        while (instList.getLast() != null && head != instList.getLast()) {
            head = head.getNext();
            var ir = head.getVal();
            translateInstruction(ir);
        }
    }

    private void translateInstruction(Instruction ir) {
        var bb = ir.getParent();
        System.out.println(ir.toString());
        System.out.println(bb);
        var mbb = funcMap.get(bb.getParent()).getBBMap().get(bb);

        System.out.println(ir.getOperand(0).toString());
        switch (ir.getOp()) {
            case Ret -> {
                if (ir.getNumOperands() == 1) {
                    var op1 = ir.getOperand(0);
                    MachineInstruction inst = new Move(mbb, new MCRegister(MCRegister.RegName.r0), valueToMCOperand(op1));
                    inst.pushBacktoInstList();
                }
            }

        }
    }

    private MCOperand valueToMCOperand(Value val) {
        var type = val.getType();
        if (type.isInt32Ty()) {
            if (val instanceof Constant) {
                Constants.ConstantInt v = (Constants.ConstantInt) val;
                return new ImmediateNumber(v.getVal());
            }

        }
        return null;
    }

}
