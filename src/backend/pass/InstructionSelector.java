package backend.pass;

import backend.machineCode.Instruction.*;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineDataBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.*;
import ir.*;
import ir.Module;
import ir.instructions.CmpInst;
import ir.instructions.Instructions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class InstructionSelector {
    private Module module;
    private ArrayList<MachineFunction> funcList;
    private ArrayList<MachineDataBlock> globalDataList;

    public InstructionSelector(Module module) {
        this.module = module;
        funcList = new ArrayList<>();
        globalDataList = new ArrayList<>();
    }

    public ArrayList<MachineFunction> getFuncList() {
        return funcList;
    }

    public ArrayList<MachineDataBlock> getGlobalDataList() {
        return globalDataList;
    }


    public void run() {
        // 得到全局变量
        var irGlobalData = module.getGlobalVariables();
        for (var g : irGlobalData) {
            var name = g.getName().substring(1);
            if (g.getType().getContainedTys(0).isArrayTy()) {
                // 数组
                var dataBlock = new MachineDataBlock(name, g);
                globalDataList.add(dataBlock);
                globalDataHash.put(g, dataBlock);
            } else {
                // 数值
                int value;

                if (g.getType().getContainedTys(0).isInt32Ty()) {
                    // global int
                    value = ((Constants.ConstantInt) g.getOperand(0)).getVal();
                } else if (g.getType().getContainedTys(0).isFloatTy()) {
                    value = Float.floatToIntBits(((Constants.ConstantFP) g.getOperand(0)).getVal());
                } else {
                    throw new RuntimeException("Shouldn't be here");
                }
                var dataBlock = new MachineDataBlock(name, value);
                globalDataList.add(dataBlock);
                globalDataHash.put(g, dataBlock);
            }
        }


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
    private HashMap<GlobalVariable, MachineDataBlock> globalDataHash = new HashMap<>();

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
        new Comment(mbb, ir.toString()).pushBacktoInstList();
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

                } else {
                    newInst = new PushOrPop(mbb, PushOrPop.Type.Pop, new MCRegister(MCRegister.RegName.PC));
                }
                newInst.setEpilogue(true);
                newInst.pushBacktoInstList();
            }
            case Br -> {
                // TODO: float num cmp
                if (ir.getNumOperands() == 1) { // unconditional branch
                    var dest = ir.getOperand(0);
                    new Branch(mbb, mf.getBBMap().get((BasicBlock) dest), false, Branch.Type.Block).pushBacktoInstList();
                } else { // conditional branch
                    if (ir.getOperand(0) instanceof Constants.ConstantInt) {
                        int val = ((Constants.ConstantInt) (ir.getOperand(0))).getVal();
                        if (val == 0) {
                            new Branch(mbb, mf.getBBMap().get(ir.getOperand(1)), false, Branch.Type.Block).pushBacktoInstList();
                        } else {  // must be 1
                            new Branch(mbb, mf.getBBMap().get(ir.getOperand(2)), false, Branch.Type.Block).pushBacktoInstList();
                        }
                    } else { // cond is an instruction
                        var cond = (CmpInst) ir.getOperand(0);
                        var op = cond.getPredicate();
                        new Cmp(mbb, valueToReg(mbb, cond.getOperand(0)), valueToMCOperand(mbb, cond.getOperand(1))).pushBacktoInstList();
                        // TODO: change the ir.getOperand(2) after merge
                        // TODO: Float number
                        MachineInstruction inst = new Branch(mbb, mf.getBBMap().get(ir.getOperand(2)), false, Branch.Type.Block);
                        inst.setCond(MachineInstruction.Condition.irToMCCond(op));
                        inst.pushBacktoInstList();

                        new Branch(mbb, mf.getBBMap().get(ir.getOperand(1)), false, Branch.Type.Block).pushBacktoInstList();
                    }
                }
            }
            case Call -> {
                mf.setLeaf(false);
                // TODO : change after phi

                // TODO : Float num

                //  float: from s0 to s15
                // int : from r0 to r3

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
                if (pType.getElementType().isArrayTy()) {
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

                } else {
                    // int or float
                    var dest = new VirtualRegister();
                    // 分配栈空间

                    new Arithmetic(mbb, Arithmetic.Type.SUB, new MCRegister(MCRegister.RegName.SP), 4).pushBacktoInstList();
                    new Arithmetic(mbb, Arithmetic.Type.SUB, dest, new MCRegister(MCRegister.RegName.r11), mf.getStackTop()).pushBacktoInstList();
                    // 保存一下位置
                    valueMap.put(ir, dest);
                    mf.addStackTop(4);
                }
            }
            case Store -> {
                Register op1 = valueToReg(mbb, ir.getOperand(0)),
                        op2 = valueToReg(mbb, ir.getOperand(1));
                new LoadOrStore(mbb, LoadOrStore.Type.STORE, op1, new Address(op2)).pushBacktoInstList();
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

                var irOp = ir.getOperand(0);
                var srcAddr = valueToReg(mbb, irOp);


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

                        Register temp = new VirtualRegister();
                       new LoadImm(mbb, temp, 4 * size).pushBacktoInstList();

                        new Arithmetic(mbb, Arithmetic.Type.MUL, op2, (Register) op, temp).pushBacktoInstList();

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
                if (dest == null) dest = srcAddr;
                valueMap.put(ir, dest);
            }
            case ICmp -> {
                // pass; will do it when needed
            }

            case ZExt -> {
                var cond = (CmpInst) ir.getOperand(0);
                var op = cond.getPredicate();
                Register dest = new VirtualRegister();
                new Cmp(mbb, valueToReg(mbb, cond.getOperand(0)), valueToMCOperand(mbb, cond.getOperand(1))).pushBacktoInstList();

                new Move(mbb, dest, new ImmediateNumber(0)).pushBacktoInstList();

                MachineInstruction inst = new Move(mbb, dest, new ImmediateNumber(1));

                inst.setCond(MachineInstruction.Condition.irToMCCond(op));
                inst.pushBacktoInstList();

                valueMap.put(ir, dest);
            }

            // TODO: Mod
            case Sub, Add -> {
                MCOperand op1 = valueToMCOperand(mbb, ir.getOperand(0)),
                        op2 = valueToMCOperand(mbb, ir.getOperand(1));

                Register dest = new VirtualRegister();
                valueMap.put(ir, dest);

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

                Arithmetic.Type mcType = switch (ir.getOp()) {
                    case Sub -> Arithmetic.Type.SUB;
                    case Add -> Arithmetic.Type.ADD;
                    default -> null;
                };
                new Arithmetic(mbb, mcType, dest, (Register) op1, op2).pushBacktoInstList();
            }
            case Mul, SDiv -> {
                Register op1 = valueToReg(mbb, ir.getOperand(0)),
                        op2 = valueToReg(mbb, ir.getOperand(1));

                Register dest = new VirtualRegister();
                valueMap.put(ir, dest);
                new Arithmetic(mbb, switch (ir.getOp()) {
                    case Mul -> Arithmetic.Type.MUL;
                    case SDiv -> Arithmetic.Type.SDIV;
                    default -> null;
                }, dest, op1, op2).pushBacktoInstList();
            }
            case SRem -> {
                mf.setLeaf(false);
                Register r1 = valueToReg(mbb, ir.getOperand(0)),
                        r2 = valueToReg(mbb, ir.getOperand(1));


                new Move(mbb, new MCRegister(Register.Content.Int, 0), r1).pushBacktoInstList();
                new Move(mbb, new MCRegister(Register.Content.Int, 1), r2).pushBacktoInstList();


                new Branch(mbb, "__aeabi_idivmod", true, Branch.Type.Call).pushBacktoInstList();


                var dest = new VirtualRegister();
                new Move(mbb, dest, new MCRegister(MCRegister.RegName.r1)).pushBacktoInstList();
                valueMap.put(ir, dest);
            }

            case FAdd, FDiv, FMul, FSub -> {
                Register r1 = valueToReg(mbb, ir.getOperand(0)),
                        r2 = valueToReg(mbb, ir.getOperand(1));
                if (!r1.isFloat()) {
                    Register rr1 = new VirtualRegister(Register.Content.Float);
                    ArrayList<String> info = new ArrayList<>();
                    info.add("f32");
                    new Move(mbb, rr1, r1).setForFloat(info).pushBacktoInstList();
                    r1 = rr1;
                }
                if (!r2.isFloat()) {
                    Register rr2 = new VirtualRegister(Register.Content.Float);
                    ArrayList<String> info = new ArrayList<>();
                    info.add("f32");
                    new Move(mbb, rr2, r2).setForFloat(info).pushBacktoInstList();
                    r2 = rr2;
                }
                Register dest = new VirtualRegister(Register.Content.Float);
                valueMap.put(ir, dest);
                new Arithmetic(mbb, switch (ir.getOp()) {
                    case FAdd -> Arithmetic.Type.ADD;
                    case FMul -> Arithmetic.Type.MUL;
                    case FDiv -> Arithmetic.Type.DIV;
                    case FSub -> Arithmetic.Type.SUB;
                    default -> null;
                },
                        dest, r1, r2).setForFloat(new ArrayList<>(
                        List.of("f32"))).pushBacktoInstList();
            }
            case FPToSI -> {
                var ori = valueToReg(mbb, ir.getOperand(0));
                var temp = new VirtualRegister(Register.Content.Float);
                var dest = new VirtualRegister();
                new VCVT(mbb, temp, ori).setForFloat(new ArrayList<>(Arrays.asList("s32", "f32"))).pushBacktoInstList();
                new Move(mbb, dest, temp).setForFloat(new ArrayList<>()).pushBacktoInstList();
                valueMap.put(ir, dest);
            }
            case SIToFP -> {
                var ori = valueToReg(mbb, ir.getOperand(0));
                var temp = new VirtualRegister(Register.Content.Float);
                var dest = new VirtualRegister(Register.Content.Float);
                new Move(mbb, temp, ori).setForFloat(new ArrayList<>()).pushBacktoInstList();
                new VCVT(mbb, dest, temp).setForFloat(new ArrayList<>(Arrays.asList("f32", "s32"))).pushBacktoInstList();
                valueMap.put(ir, dest);
            }


            default -> {
                throw new RuntimeException("Didn't process command: " + ir);

            }
        }
    }

    private MCOperand valueToMCOperand(MachineBasicBlock parent, Value val) {
        var func = parent.getParent();
        var valueMap = func.getValueMap();
        var type = val.getType();
        if (val instanceof GlobalVariable) {
            var dataBlock = globalDataHash.get(val);
            Register dest = new VirtualRegister();
            new LoadImm(parent, dest, dataBlock).pushBacktoInstList();
            return dest;
        } else if (val instanceof Constant) {
            if (type.isInt32Ty()) {

                Constants.ConstantInt v = (Constants.ConstantInt) val;
                int value = v.getVal();
                if (ImmediateNumber.isLegalImm(value))
                    return new ImmediateNumber(value);
                else {
                    // not a legal immediate number, has to load a literal value
                    Register dest = new VirtualRegister();
                    new LoadImm(parent, dest, value).pushBacktoInstList();
                    return dest;
                }
            } else if (type.isFloatTy()) {
                float value = ((Constants.ConstantFP) val).getVal();
                Register dest = new VirtualRegister();
                new LoadImm(parent, dest, value).pushBacktoInstList();
                return dest;
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

        throw new RuntimeException("Unreachable point: " + val);
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
