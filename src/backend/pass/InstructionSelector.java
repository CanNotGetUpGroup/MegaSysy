package backend.pass;

import backend.machineCode.Instruction.*;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.*;
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

        // 稍微处理了一下参数。。。这个不太行。目前是用Virtual Regitser又转存了一次。TODO: 在完成Phi指令的时候应该顺便修改这里
        var firstbb = mf.getBbList().getFirst().getVal();
        var paras = irFunction.getArguments();
        for (var para : paras) {
            var dest = new VirtualRegister();
            mf.getValueMap().put(para, dest);
            if (para.getArgNo() < 4) {
                new Move(firstbb, dest, switch (para.getArgNo()) {
                    case 0 -> new MCRegister(MCRegister.RegName.r0);
                    case 1 -> new MCRegister(MCRegister.RegName.r1);
                    case 2 -> new MCRegister(MCRegister.RegName.r2);
                    case 3 -> new MCRegister(MCRegister.RegName.r3);
                    default -> null;
                }).pushBacktoInstList();
            } else {
                new LoadOrStore(firstbb, LoadOrStore.Type.LOAD, dest, new Adress(new MCRegister(MCRegister.RegName.r11), 4 * (para.getArgNo() - 3))).pushBacktoInstList();
            }
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
                    new Move(mbb, new MCRegister(MCRegister.RegName.r0), valueToMCOperand(mbb, valueMap, op1)).pushBacktoInstList();
                }

                // 不管是不是叶子节点都push了，为了解决栈上参数的问题 TODO：未来修改
                MachineInstruction newInst = new PushOrPop(mbb, PushOrPop.Type.Push, new MCRegister(MCRegister.RegName.LR));
                newInst.setPrologue(true);
                newInst.pushtofront();

                if (mf.isLeaf()) {
                    newInst = new Branch(mbb, new MCRegister(MCRegister.RegName.LR), false, Branch.Type.Ret);
                    newInst.setEpilogue(true);
                    newInst.pushBacktoInstList();

                } else {
                    newInst = new PushOrPop(mbb, PushOrPop.Type.Pop, new MCRegister(MCRegister.RegName.PC));
                    newInst.setEpilogue(true);
                    newInst.pushBacktoInstList();
                }
            }
            case Call -> {
                mf.setLeaf(false);
                // TODO : change after phi
                int paraNum = ir.getNumOperands();

                for (--paraNum; paraNum > 4; paraNum--) {
                    new PushOrPop(mbb, PushOrPop.Type.Push, valueToReg(mbb, valueMap, ir.getOperand(paraNum))).pushBacktoInstList();
                }
                for (; paraNum > 0; paraNum--) {
                    new Move(mbb, new MCRegister(MCRegister.idTORegName(paraNum - 1)), valueToMCOperand(mbb, valueMap, ir.getOperand(paraNum))).pushBacktoInstList();
                }

                new Branch(mbb, funcMap.get(ir.getOperand(0)), true, Branch.Type.Call).pushBacktoInstList();
                // release stack
                if (paraNum > 4) {
                    new Arithmetic(mbb, Arithmetic.Type.ADD, new MCRegister(MCRegister.RegName.SP), new ImmediateNumber((paraNum - 5) * 4)).pushBacktoInstList();
                }
                var dest = new VirtualRegister();
                new Move(mbb, dest, new MCRegister(MCRegister.RegName.r0)).pushBacktoInstList();
                valueMap.put(ir, dest);
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
                MCOperand op1 = valueToMCOperand(mbb, valueMap, ir.getOperand(0)),
                        op2 = valueToMCOperand(mbb, valueMap, ir.getOperand(1));
                if (op2 instanceof Register) {
                    // 存到寄存器中
                    new Move(mbb, (Register) op2, op1).pushBacktoInstList();
                } else {
                    // TODO: 存到堆栈中（数组。。。）
                }
            }
            case Load -> {
                assert ir.getNumOperands() == 2;
                MCOperand src = valueToMCOperand(mbb, valueMap, ir.getOperand(0));
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
                MCOperand op1 = valueToMCOperand(mbb, valueMap, ir.getOperand(0)),
                        op2 = valueToMCOperand(mbb, valueMap, ir.getOperand(1));

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

    private MCOperand valueToMCOperand(MachineBasicBlock parent, HashMap<Value, Register> valueMap, Value val) {
        var type = val.getType();
        if (val instanceof Constant) {
            if (type.isInt32Ty()) {
                Constants.ConstantInt v = (Constants.ConstantInt) val;
                int value = v.getVal();
                if (ImmediateNumber.isLegalImm(value))
                    return new ImmediateNumber(value);
                else {
                    // not a legal immediate number, has to load a literal value
                    Register dest = new VirtualRegister();
                    new LoadOrStore(parent, LoadOrStore.Type.LOAD, dest, new ImmediateNumber(value)).pushBacktoInstList();
                    return dest;
                }
            }
        } else if (val instanceof Instruction) {
            var ans = valueMap.get(val);
            if (ans == null) throw new RuntimeException("Not defined instruction");
            return ans;
        } else if (val instanceof Argument) {
            return valueMap.get(val);
        }

        throw new RuntimeException("Unreachable point");
    }

    private Register valueToReg(MachineBasicBlock parent, HashMap<Value, Register> valueMap, Value val) {
        MCOperand res = valueToMCOperand(parent, valueMap, val);
        if (res instanceof Register)
            return (Register) res;
        if (res instanceof ImmediateNumber) {
            var dest = new VirtualRegister();
            ImmediateNumber.loadNum(parent,dest, ((ImmediateNumber) res).getValue()).pushBacktoInstList();
            return dest;
        }
        throw new RuntimeException("can't convert to Register, or maybe haven't finished this part");
    }


}
