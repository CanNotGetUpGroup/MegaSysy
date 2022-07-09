package backend.pass;

import backend.machineCode.Instruction.*;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.*;
import ir.*;
import ir.Module;
import ir.instructions.CmpInst;
import ir.instructions.Instructions;

import java.util.ArrayList;
import java.util.HashMap;

import static ir.Instruction.Ops.Load;

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
                new LoadOrStore(firstbb, LoadOrStore.Type.LOAD, dest, new Address(new MCRegister(MCRegister.RegName.r11), 4 * (para.getArgNo() - 3))).pushBacktoInstList();
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
                    new Move(mbb, new MCRegister(MCRegister.RegName.r0), valueToMCOperand(mbb, op1)).pushBacktoInstList();
                }

                // 不管是不是叶子节点都push了，为了解决栈上参数的问题 TODO：未来修改
                MachineInstruction newInst = new PushOrPop(mf.getBbList().getFirst().getVal(), PushOrPop.Type.Push, new MCRegister(MCRegister.RegName.LR));
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
            case Br -> {
                if (ir.getNumOperands() == 1) { // unconditional branch
                    var dest = ir.getOperand(0);
                    new Branch(mbb, mf.getBBMap().get((BasicBlock) dest), false, Branch.Type.Block).pushBacktoInstList();
                } else { // conditional branch
                    var cond = (CmpInst) ir.getOperand(0);
                    var op = cond.getPredicate();
                    new Cmp(mbb, valueToReg(mbb, cond.getOperand(0)), valueToMCOperand(mbb, cond.getOperand(1))).pushBacktoInstList();
                    // TODO: change the ir.getOperand(2) after merge
                    MachineInstruction inst = new Branch(mbb, mf.getBBMap().get(ir.getOperand(2)), false, Branch.Type.Block);
                    inst.setCond(switch (op) {
                        case ICMP_EQ -> MachineInstruction.Condition.EQ;
                        case ICMP_NE -> MachineInstruction.Condition.NE;
                        case ICMP_SGE -> MachineInstruction.Condition.GE;
                        case ICMP_SGT -> MachineInstruction.Condition.GT;
                        case ICMP_SLE -> MachineInstruction.Condition.LE;
                        case ICMP_SLT -> MachineInstruction.Condition.LT;
                        default -> null;
                    });
                    inst.pushBacktoInstList();

                    new Branch(mbb, mf.getBBMap().get(ir.getOperand(1)), false, Branch.Type.Block).pushBacktoInstList();

                }
            }
            case Call -> {
                mf.setLeaf(false);
                // TODO : change after phi
                int paraNum = ir.getNumOperands();

                for (--paraNum; paraNum > 4; paraNum--) {
                    new PushOrPop(mbb, PushOrPop.Type.Push, valueToReg(mbb, ir.getOperand(paraNum))).pushBacktoInstList();
                }
                for (; paraNum > 0; paraNum--) {
                    new Move(mbb, new MCRegister(MCRegister.idTORegName(paraNum - 1)), valueToMCOperand(mbb, ir.getOperand(paraNum))).pushBacktoInstList();
                }

                new Branch(mbb, funcMap.get(ir.getOperand(0)), true, Branch.Type.Call).pushBacktoInstList();

                // release stack
                paraNum = ir.getNumOperands();
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
                    var dest = new VirtualRegister();
                    // 分配栈空间

                    new Arithmetic(mbb, Arithmetic.Type.SUB, new MCRegister(MCRegister.RegName.SP), 4).pushBacktoInstList();
                    new Arithmetic(mbb, Arithmetic.Type.SUB, dest, new MCRegister(MCRegister.RegName.r11), mf.getStackTop()).pushBacktoInstList();
                    // 保存一下位置
                    valueMap.put(ir, dest);
                    mf.addStackTop( 4);
                } else {
                    // TODO: 数组
                    assert pType.getElementType().isArrayTy();

                    var contentType = (DerivedTypes.ArrayType) pType.getElementType();
                    var size = contentType.getEleSize() * contentType.getNumElements();

                    // 分配栈空间
                    mf.addStackTop(size * 4);

                    var dest = new VirtualRegister();
                    new Arithmetic(mbb, Arithmetic.Type.SUB, new MCRegister(MCRegister.RegName.SP), size * 4).pushBacktoInstList();
                    new Arithmetic(mbb, Arithmetic.Type.SUB, dest, new MCRegister(MCRegister.RegName.r11), mf.getStackTop() - 4).pushBacktoInstList();
                    // 保存一下位置
                    valueMap.put(ir, dest);

                }
            }
            case Store -> {
                Register op1 = valueToReg(mbb, ir.getOperand(0)),
                        op2 = valueToReg(mbb, ir.getOperand(1));
                new LoadOrStore(mbb, LoadOrStore.Type.STORE,(Register) op1, new Address(op2)).pushBacktoInstList();
            }
            case Load -> {
                Register src = valueToReg(mbb, ir.getOperand(0));
                Register dest = new VirtualRegister();
                valueMap.put(ir, dest);

                new LoadOrStore(mbb, LoadOrStore.Type.LOAD, dest, new Address(src)).pushBacktoInstList();
            }
            case GetElementPtr -> {
                var curIr = (Instructions.GetElementPtrInst) ir;
                assert ir.getType().isPointerTy();

                var srcAddr = valueMap.get(ir.getOperand(0));

                int constOffset = 0;

                Register dest = null;

                var srcType = curIr.getSourceElementType();
                int i = 1;
                while (true) {
                    MCOperand op = valueToMCOperand(mbb, ir.getOperand(i));
                    int size = srcType.isArrayTy() ? ((DerivedTypes.ArrayType) srcType).size() : 1;
                    if (op instanceof ImmediateNumber) {
                        constOffset += 4 * ((ImmediateNumber) op).getValue() * size;
                    } else {
                        assert op instanceof Register;
                        Register op2 = new VirtualRegister();
                        new Arithmetic(mbb, Arithmetic.Type.MUL, op2, (Register) op, 4 * size).pushBacktoInstList();

                        if (dest == null) {
                            dest = new VirtualRegister();
                            new Arithmetic(mbb, Arithmetic.Type.ADD, dest, srcAddr, op2).pushBacktoInstList();
                        } else // Warning: NOT SSA here
                            new Arithmetic(mbb, Arithmetic.Type.ADD, dest, op2).pushBacktoInstList();
                    }
                    if (srcType.isInt32Ty() || i >= ir.getNumOperands() - 1) break;
                    i++;
                    srcType = ((DerivedTypes.ArrayType) srcType).getKidType();
                }
                if (constOffset != 0) {
                    // deal with const offset together
                    if (dest == null) {
                        dest = new VirtualRegister();
                        new Arithmetic(mbb, Arithmetic.Type.ADD, dest, srcAddr, constOffset).pushBacktoInstList();
                    } else // Warning: NOT SSA here
                        new Arithmetic(mbb, Arithmetic.Type.ADD, dest, constOffset).pushBacktoInstList();
                }
                if(dest == null) dest = srcAddr;
                valueMap.put(ir, dest);
            }

            // TODO: Mod
            case Sub, Add, Mul, SDiv -> {
                assert ir.getNumOperands() == 2;
                MCOperand op1 = valueToMCOperand(mbb, ir.getOperand(0)),
                        op2 = valueToMCOperand(mbb, ir.getOperand(1));

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

            default -> {
//                System.out.println("Didn't process command: " + ir);
            }
        }
    }

    private MCOperand valueToMCOperand(MachineBasicBlock parent, Value val) {
        var func = parent.getParent();
        var valueMap = func.getValueMap();
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
            } else if (type.isPointerTy()) {
                // TODO: ???
                return valueMap.get(val);
            }
        } else if (val instanceof Instruction) {
            var ans = valueMap.get(val);
            if (ans == null) throw new RuntimeException("Not defined instruction: " + val);
            return ans;
        } else if (val instanceof Argument) {
            return valueMap.get(val);
        }

        throw new RuntimeException("Unreachable point");
    }

    private Register valueToReg(MachineBasicBlock parent, Value val) {
        MCOperand res = valueToMCOperand(parent, val);
        if (res instanceof Register)
            return (Register) res;
        if (res instanceof ImmediateNumber) {
            var dest = new VirtualRegister();
            ImmediateNumber.loadNum(parent, dest, ((ImmediateNumber) res).getValue()).pushBacktoInstList();
            return dest;
        }
        if (res instanceof Address) {
            // TODO : need to modify to global array
            throw new RuntimeException("Try to convert an address to register");
        }
        throw new RuntimeException("can't convert to Register, or maybe haven't finished this part");
    }


}
