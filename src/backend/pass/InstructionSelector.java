package backend.pass;

import backend.machineCode.Instruction.Arithmetic;
import backend.machineCode.Instruction.Branch;
import backend.machineCode.Instruction.Move;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.*;
import ir.*;
import ir.Module;

import java.net.PortUnreachableException;
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

        HashMap<BasicBlock, MachineBasicBlock> bbMap = mf.getBBMap();
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
        var mf = funcMap.get(bb.getParent());
        var mbb = mf.getBBMap().get(bb);
        var valueMap = mf.getValueMap();
        switch (ir.getOp()) {
            case Ret -> {
                if (ir.getNumOperands() == 1) {
                    var op1 = ir.getOperand(0);
                    new Move(mbb, new MCRegister(MCRegister.RegName.r0), valueToMCOperand(valueMap, op1)).pushBacktoInstList();
                }
                new Branch(mbb, new MCRegister(MCRegister.RegName.LR), false);
            }
            // 可能可以优化， 但中端也可以做吧？MemtoReg
            case Alloca -> {
                var type = ir.getType();
                assert type.isPointerTy();
                DerivedTypes.PointerType pType = (DerivedTypes.PointerType) type;
                if (pType.getElementType().isInt32Ty()) {
                    // 为int32分配空间，分配到寄存器上
                    valueMap.put(ir, new VirtualRegister());
                } else {
                    // TODO: 数组?
                    throw new RuntimeException("Unfinish: array allocation");
                }
            }
            case Store -> {
                assert ir.getNumOperands() == 2;
                MCOperand op1 = valueToMCOperand(valueMap, ir.getOperand(0)),
                        op2 = valueToMCOperand(valueMap, ir.getOperand(1));
                if (op2 instanceof Register) {
                    // 存到寄存器中
                    new Move(mbb, (Register) op2, op1).pushBacktoInstList();
                } else {
                    // TODO: 存到堆栈中（数组。。。）
                }
            }
            case Load -> {
                assert ir.getNumOperands() == 2;
                MCOperand src = valueToMCOperand(valueMap, ir.getOperand(0));
                Register dest = new VirtualRegister();
                valueMap.put(ir, dest);
                if (src instanceof Register) {
                    // 寄存器中取
                    new Move(mbb, dest, src).pushBacktoInstList();
                } else {
                    // TODO: 存到堆栈中（数组。。。）
                }
            }

            case Sub, Add, Mul, SDiv -> {
                assert ir.getNumOperands() == 2;
                MCOperand op1 = valueToMCOperand(valueMap, ir.getOperand(0)),
                        op2 = valueToMCOperand(valueMap, ir.getOperand(1));

                Register dest = new VirtualRegister();
                valueMap.put(ir, dest);

                assert op1 instanceof ImmediateNumber || op1 instanceof Register;
                assert op2 instanceof ImmediateNumber || op2 instanceof Register;

                if (op1 instanceof ImmediateNumber) {
                    if (Instruction.isCommutative(ir.getOp())) {
                        MCOperand tmp = op1;
                        op1 = op2;
                        op2 = tmp;
                    } else {
                        MCOperand n = new VirtualRegister();
                        new Move(mbb, n, op1).pushBacktoInstList();
                        op1 = n;
                    }
                }
                if (op2 instanceof ImmediateNumber
                        && (ir.getOp() == Instruction.Ops.Mul || ir.getOp() == Instruction.Ops.SDiv)) {
                    MCOperand n = new VirtualRegister();
                    new Move(mbb, n, op2).pushBacktoInstList();
                    op2 = n;
                }
                Arithmetic.Type mcType = switch (ir.getOp()) {
                    case Sub -> Arithmetic.Type.SUB;
                    case Add -> Arithmetic.Type.ADD;
                    case Mul -> Arithmetic.Type.MUL;
                    case SDiv -> Arithmetic.Type.SDIV;
                    default -> null;
                };
                assert mcType != null;
                new Arithmetic(mbb, mcType, dest, (Register) op1, op2).pushBacktoInstList();

            }
        }
    }

    private MCOperand valueToMCOperand(HashMap<Value, Register> valueMap, Value val) {

        var type = val.getType();
        if (val instanceof Constant) {
            if (type.isInt32Ty()) {
                Constants.ConstantInt v = (Constants.ConstantInt) val;
                return new ImmediateNumber(v.getVal());
            }
        } else if (val instanceof Instruction) {
            var ans = valueMap.get(val);
            if (ans == null) throw new RuntimeException("Not defined instruction");
            return ans;
        }
        throw new RuntimeException("Unreachable point");
    }

}
