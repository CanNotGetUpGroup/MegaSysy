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
import util.IList;
import util.IListNode;

import java.awt.image.renderable.RenderableImage;
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
    private static HashMap<GlobalVariable, MachineDataBlock> globalDataHash = new HashMap<>();

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
        for (var bb : bbList) {
            for (var inst : bb.getInstList()) {
                if (inst.getOp() == Instruction.Ops.Call)
                    mf.setLeaf(false);
            }
        }

        // 稍微处理了一下参数。。。这个不太行。目前是用Virtual Regitser又转存了一次。TODO: 在完成Phi指令的时候应该顺便修改这里
        var firstbb = mf.getBbList().getFirst().getVal();
        var paras = irFunction.getArguments();
        int floatNum = 0, intNum = 0, onStackNum = 0;
        for (var para : paras) {
            Register dest;

            if (para.getType().isFloatTy()) {
                dest = new VirtualRegister(Register.Content.Float);
                if (floatNum < 16) { // is on reg
                    new Move(firstbb, dest, new MCRegister(Register.Content.Float, floatNum))
                            .setForFloat(true)
                            .pushBacktoInstList();
                    floatNum++;
                } else {
                    onStackNum++;
                    new LoadOrStore(firstbb, LoadOrStore.Type.LOAD, dest, new Address(new MCRegister(MCRegister.RegName.r11), 4 * onStackNum)).setForFloat(true).pushBacktoInstList();
                }
            } else { // para is not float, array or int
                dest = new VirtualRegister();
                if (intNum < 4) {
                    new Move(firstbb, dest, new MCRegister(Register.Content.Int, intNum))
                            .pushBacktoInstList();
                    intNum++;
                } else {
                    onStackNum++;
                    new LoadOrStore(firstbb, LoadOrStore.Type.LOAD, dest, new Address(new MCRegister(MCRegister.RegName.r11), 4 * onStackNum)).pushBacktoInstList();
                }
            }
            mf.getValueMap().put(para, dest);
        }

        head = bbList.getHead();
        while (bbList.getLast() != null && head != bbList.getLast()) {
            head = head.getNext();
            var bb = head.getVal();

            translateBB(bb);
        }

        //
        MachineInstruction newInst = new PushOrPop(mf.getBbList().getFirst().getVal(), PushOrPop.Type.Push, new MCRegister(MCRegister.RegName.LR));
        newInst.setPrologue(true);
        newInst.pushtofront();
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
            case PHI -> {
                Register dest = new VirtualRegister(ir.getType().isFloatTy() ? Register.Content.Float : Register.Content.Int);
                new Phi(mbb, dest, (Instructions.PHIInst) ir).setForFloat(ir.getType().isFloatTy()).pushBacktoInstList();
                valueMap.put(ir, dest);
            }
            case Ret -> {
                if (ir.getNumOperands() == 1) {
                    var op1 = ir.getOperand(0);
                    var retType = ir.getParent().getParent().getRetType();
                    if (retType.isVoidTy()) {
                        ;
                    } else if (retType.isFloatTy()) {
                        new Move(mbb, new MCRegister(Register.Content.Float, 0), valueToFloatReg(mbb, op1)).setForFloat(true).pushBacktoInstList();
                    } else
                        new Move(mbb, new MCRegister(MCRegister.RegName.r0), valueToMCOperand(mbb, op1)).pushBacktoInstList();
                }
                MachineInstruction newInst;
                // 不管是不是叶子节点都push了，为了解决栈上参数的问题 TODO：未来修改
//                if (mf.isLeaf()) {
//                    newInst = new Branch(mbb, new MCRegister(MCRegister.RegName.LR), false, Branch.Type.Ret);

//                } else {
                newInst = new PushOrPop(mbb, PushOrPop.Type.Pop, new MCRegister(MCRegister.RegName.PC));
//                }
                newInst.setEpilogue(true);
                newInst.pushBacktoInstList();
            }
            case Br -> {
                var node = mbb.getInstList().getLast();
                // TODO: float num cmp
                if (ir.getNumOperands() == 1) { // unconditional branch
                    var dest = ir.getOperand(0);
                    var mdest =mf.getBBMap().get((BasicBlock) dest);
                    new Branch(mbb, mdest, false, Branch.Type.Block).pushBacktoInstList();
                    mbb.addSuccessor(mdest);
                } else { // conditional branch
                    if (ir.getOperand(0) instanceof Constants.ConstantInt) {
                        int val = ((Constants.ConstantInt) (ir.getOperand(0))).getVal();
                        if (val == 0) {
                            var dest =  mf.getBBMap().get(ir.getOperand(1));
                            new Branch(mbb,dest , false, Branch.Type.Block).pushBacktoInstList();
                            mbb.addSuccessor(dest);
                        } else {  // must be 1
                            var dest = mf.getBBMap().get(ir.getOperand(2));
                            new Branch(mbb, dest, false, Branch.Type.Block).pushBacktoInstList();
                            mbb.addSuccessor(dest);
                        }
                    } else { // cond is an instruction
                        var cond = (CmpInst) ir.getOperand(0);
                        var op = cond.getPredicate();
                        Register r1;
                        MCOperand r2 = valueToMCOperand(mbb, cond.getOperand(1));
                        if (cond.getOp() == Instruction.Ops.FCmp) {
                            r1 = valueToFloatReg(mbb, cond.getOperand(0));
                            if (r2 instanceof Register) r2 = regToFloatReg(mbb, (Register) r2);
                        } else {
                            r1 = valueToReg(mbb, cond.getOperand(0));
                        }

                        new Cmp(mbb, r1, r2).setForFloat(cond.getOp() == Instruction.Ops.FCmp, new ArrayList<>(List.of("f32"))).pushBacktoInstList();
                        if (cond.getOp() == Instruction.Ops.FCmp)
                            new VMRS(mbb, new MCRegister(MCRegister.RegName.APSR_nzcv), new MCRegister(MCRegister.RegName.FPSCR))
                                    .pushBacktoInstList();
                        // TODO: change the ir.getOperand(2) after merge
                        // TODO: Float number
                        var dest = mf.getBBMap().get(ir.getOperand(2));
                        MachineInstruction inst = new Branch(mbb, dest, false, Branch.Type.Block);
                        mbb.addSuccessor(dest);
                        inst.setCond(MachineInstruction.Condition.irToMCCond(op));
                        inst.pushBacktoInstList();

                        dest = mf.getBBMap().get(ir.getOperand(1));
                        new Branch(mbb, dest, false, Branch.Type.Block).pushBacktoInstList();
                        mbb.addSuccessor(dest);
                    }
                }
                node = node.getNext();
                node.getVal().setforBr(true);
            }
            case Call -> {

                // TODO : change after phi

                // TODO : Float num

                //  float: from s0 to s15
                // int : from r0 to r3

                int paraNum = ir.getNumOperands();

                int floatParaNum = 0, intParaNum = 0;
                int firstStackFloatPara = paraNum, firstStackIntPara = paraNum;
                for (int i = 1; i < paraNum; i++) {
                    if (ir.getOperand(i).getType().isFloatTy()) {
                        if (floatParaNum >= 16 && firstStackFloatPara == paraNum) firstStackFloatPara = i;
                        floatParaNum++;
                    } else {
                        if (intParaNum >= 4 && firstStackIntPara == paraNum) firstStackIntPara = i;
                        intParaNum++; // could include pointer/array...
                    }
                }
                // paras store on stack
                int numOnStack = 0;
                if (floatParaNum > 16) numOnStack += floatParaNum - 16;
                if (intParaNum > 4) numOnStack += intParaNum - 4;
                if (numOnStack > mf.getMaxParaNumOnStack()) mf.setMaxParaNumOnStack(numOnStack);
                for (int i = 4, stackPos = 0; i < paraNum; i++) {

                    var op = ir.getOperand(i);
                    if (op.getType().isFloatTy()) {
                        if (i < firstStackFloatPara) continue;
                        new LoadOrStore(mbb, LoadOrStore.Type.STORE, valueToFloatReg(mbb, op), new Address(new MCRegister(MCRegister.RegName.SP), stackPos * 4)).setForFloat(true).pushBacktoInstList();
                        stackPos++;
                    } else {
                        if (i < firstStackIntPara) continue;
                        new LoadOrStore(mbb, LoadOrStore.Type.STORE, valueToReg(mbb, op), new Address(new MCRegister(MCRegister.RegName.SP), stackPos * 4)).pushBacktoInstList();
                        stackPos++;
                    }
                }

                // params store on reg
                int intRegId = 3, floatRegId = 15;
                if (intParaNum < 4) intRegId = intParaNum - 1;
                if (floatParaNum < 16) floatRegId = floatParaNum - 1;
                for (int i = paraNum - 1; i > 0; i--) {
                    var op = ir.getOperand(i);
                    if (op.getType().isFloatTy() && i < firstStackFloatPara) {
                        new Move(mbb, new MCRegister(Register.Content.Float, floatRegId), valueToFloatReg(mbb, op)).setForFloat(true).pushBacktoInstList();
                        floatRegId--;
                    }
                    if (!op.getType().isFloatTy() && i < firstStackIntPara) {
                        new Move(mbb, new MCRegister(Register.Content.Int, intRegId), valueToMCOperand(mbb, op)).pushBacktoInstList();
                        intRegId--;
                    }
                }

                new Branch(mbb, funcMap.get(ir.getOperand(0)), true, Branch.Type.Call).pushBacktoInstList();

                // store return value
                if (ir.getOperand(0).getType().isFunctionTy()) {
                    var retType = ((DerivedTypes.FunctionType) ir.getOperand(0).getType()).getReturnType();
                    if (retType.isFloatTy()) {
                        var dest = new VirtualRegister(Register.Content.Float);
                        new Move(mbb, dest, new MCRegister(Register.Content.Float, 0)).setForFloat(true).pushBacktoInstList();
                        valueMap.put(ir, dest);

                    } else if (retType.isVoidTy()) {

                    } else {
                        var dest = new VirtualRegister();
                        new Move(mbb, dest, new MCRegister(MCRegister.RegName.r0)).pushBacktoInstList();
                        valueMap.put(ir, dest);
                    }
                }

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
                    new Arithmetic(mbb, Arithmetic.Type.SUB, new MCRegister(MCRegister.RegName.SP), ImmediateNumber.getLegalOperand(mbb, size * 4)).pushBacktoInstList();
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
                        } else {
                            var dest1 = new VirtualRegister();
                            new Arithmetic(mbb, Arithmetic.Type.ADD, dest1, dest, op2).pushBacktoInstList();
                            dest = dest1;
                        }
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
                    } else {
                        var dest2 = new VirtualRegister();
                        new Arithmetic(mbb, Arithmetic.Type.ADD, dest2, dest, constOffset).pushBacktoInstList();
                        dest = dest2;
                    }

                }
                if (dest == null) dest = srcAddr;
                valueMap.put(ir, dest);
            }
            case ICmp, FCmp -> {
                // pass; will do it when needed
            }

            case ZExt -> {
                var cond = (CmpInst) ir.getOperand(0);
                var op = cond.getPredicate();
                Register dest = new VirtualRegister();

                Register r1;
                MCOperand r2 = valueToMCOperand(mbb, cond.getOperand(1));
                if (cond.getOp() == Instruction.Ops.FCmp) {
                    r1 = valueToFloatReg(mbb, cond.getOperand(0));
                    if (r2 instanceof Register) r2 = regToFloatReg(mbb, (Register) r2);
                } else {
                    r1 = valueToReg(mbb, cond.getOperand(0));
                }

                new Cmp(mbb, r1, r2).setForFloat(cond.getOp() == Instruction.Ops.FCmp, new ArrayList<>(List.of("f32"))).pushBacktoInstList();
                if (cond.getOp() == Instruction.Ops.FCmp)
                    new VMRS(mbb, new MCRegister(MCRegister.RegName.APSR_nzcv), new MCRegister(MCRegister.RegName.FPSCR))
                            .pushBacktoInstList();

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
                if (op1 instanceof ImmediateNumber) {
                    Register opp1 = new VirtualRegister();
                    new Move(mbb, opp1, op1).pushBacktoInstList();
                    op1 = opp1;
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
                var ori = valueToFloatReg(mbb, ir.getOperand(0));
                var temp = new VirtualRegister(Register.Content.Float);
                var dest = new VirtualRegister();
                new VCVT(mbb, temp, ori, new ArrayList<>(Arrays.asList("s32", "f32"))).pushBacktoInstList();
                new Move(mbb, dest, temp).setForFloat(true).pushBacktoInstList();
                valueMap.put(ir, dest);
            }
            case SIToFP -> {
                var ori = valueToReg(mbb, ir.getOperand(0));
                var temp = new VirtualRegister(Register.Content.Float);
                var dest = new VirtualRegister(Register.Content.Float);
                new Move(mbb, temp, ori).setForFloat(true).pushBacktoInstList();
                new VCVT(mbb, dest, temp, new ArrayList<>(Arrays.asList("f32", "s32"))).pushBacktoInstList();
                valueMap.put(ir, dest);
            }


            default -> {
                throw new RuntimeException("Didn't process command: " + ir);

            }
        }
    }

    public static MCOperand valueToMCOperand(MachineBasicBlock parent, Value val) {
        return valueToMCOperandInsertBefore(parent, val, parent.getInstList().getTail());
    }

    public static MCOperand valueToMCOperandInsertBefore(MachineBasicBlock parent, Value val, IListNode<MachineInstruction, MachineBasicBlock> node) {
        var func = parent.getParent();
        var valueMap = func.getValueMap();
        var type = val.getType();

        if (val instanceof GlobalVariable) {
            var dataBlock = globalDataHash.get(val);
            Register dest = new VirtualRegister();
            new LoadImm(parent, dest, dataBlock).insertBefore(node);
            return dest;
        } else if (val instanceof Constant) {
            if (type.isInt1Ty() || type.isInt32Ty()) {

                Constants.ConstantInt v = (Constants.ConstantInt) val;
                int value = v.getVal();
                if (ImmediateNumber.isLegalImm(value))
                    return new ImmediateNumber(value);
                else {
                    // not a legal immediate number, has to load a literal value
                    Register dest = new VirtualRegister();
                    new LoadImm(parent, dest, value).insertBefore(node);
                    return dest;
                }
            } else if (type.isFloatTy()) {
                float value = ((Constants.ConstantFP) val).getVal();
                Register dest = new VirtualRegister();
                new LoadImm(parent, dest, value).insertBefore(node);
                return dest;
            } else if (type.isPointerTy()) {
                // TODO: ???
                return valueMap.get(val);
            }
        } else if (val instanceof Instruction) {
            var ans = valueMap.get(val);

            if (ans == null)
                if (val instanceof CmpInst)
                    return i1ToReg(parent, val);
                else
                    throw new RuntimeException("Not defined instruction: " + val);
            return ans;
        } else if (val instanceof Argument) {
            return valueMap.get(val);
        }

        throw new RuntimeException("Unreachable point: " + val);
    }

    public static Register valueToReg(MachineBasicBlock parent, Value val) {
        return valueToRegInsertBefore(parent, val, parent.getInstList().getTail());
    }

    public static Register valueToRegInsertBefore(MachineBasicBlock parent, Value val, IListNode<MachineInstruction, MachineBasicBlock> node) {
        MCOperand res = valueToMCOperand(parent, val);
        if (res instanceof Register)
            return (Register) res;
        if (res instanceof ImmediateNumber) {
            var dest = new VirtualRegister();
            ImmediateNumber.loadNumInsertBefore(parent, dest, ((ImmediateNumber) res).getValue(), node);
            return dest;
        }
        if (res instanceof Address) {
            // TODO : need to modify to global array
            throw new RuntimeException("Try to convert an address to register");
        }
        throw new RuntimeException("can't convert to Register, or maybe haven't finished this part");
    }

    public static Register valueToFloatReg(MachineBasicBlock parent, Value val) {
        return valueToFloatRegInsertBefore(parent, val, parent.getInstList().getTail());
    }

    public static Register valueToFloatRegInsertBefore(MachineBasicBlock parent, Value val, IListNode<MachineInstruction, MachineBasicBlock> node) {
        Register reg = valueToRegInsertBefore(parent, val, node);
        if (!reg.isFloat()) {
            return regToFloatRegInsertBefore(parent, reg, node);
        } else
            return reg;
    }

    public static Register regToFloatReg(MachineBasicBlock parent, Register reg) {
        return regToFloatRegInsertBefore(parent, reg, parent.getInstList().getTail());
    }

    public static Register regToFloatRegInsertBefore(MachineBasicBlock parent, Register reg, IListNode<MachineInstruction, MachineBasicBlock> node) {
        if (reg.isFloat()) return reg;
        var rr1 = new VirtualRegister(Register.Content.Float);
        new Move(parent, rr1, reg).setForFloat(true).insertBefore(node);

        return rr1;
    }

    public static MCOperand valueToFloatOp(MachineBasicBlock parent, Value val) {
        var ans = valueToMCOperand(parent, val);
        if (ans instanceof ImmediateNumber) {
            return ans;
        } else if (ans instanceof Register) {
            return regToFloatReg(parent, (Register) ans);
        } else {
            throw new RuntimeException("Don't know");
        }
    }


    public static Register i1ToReg(MachineBasicBlock parent, Value ir) {
        var func = parent.getParent();
        var valueMap = func.getValueMap();
        var dest = new VirtualRegister();
        if (ir instanceof Constants.ConstantInt) {
            new LoadImm(parent, dest, ((Constants.ConstantInt) ir).getVal()).pushBacktoInstList();
            return dest;
        } else { // Instruction
            var ans = valueMap.get(ir);
            if (ans != null) return ans;

            var op1 = ((CmpInst) ir).getOperand(0);
            var op2 = ((CmpInst) ir).getOperand(1);
            var cond = ((CmpInst) ir).getPredicate();

            Register r1 = valueToReg(parent, op1);
            var r2 = valueToMCOperand(parent, op2);
            if (((CmpInst) ir).getOp() == Instruction.Ops.FCmp) {
                if (r1.isFloat()) {
                    var rr1 = new VirtualRegister(Register.Content.Float);
                    new Move(parent, rr1, r1).setForFloat(true).pushBacktoInstList();

                    r1 = rr1;
                } else if (r2 instanceof Register && ((Register) r2).isFloat()) {
                    var rr2 = new VirtualRegister(Register.Content.Float);
                    new Move(parent, rr2, r2).setForFloat(true).pushBacktoInstList();

                    r2 = rr2;
                }
            }
            new Cmp(parent, r1, r2).setForFloat(((CmpInst) ir).getOp() == Instruction.Ops.FCmp, new ArrayList<>(List.of("F32"))).pushBacktoInstList();
            if (((CmpInst) ir).getOp() == Instruction.Ops.FCmp)
                new VMRS(parent, new MCRegister(MCRegister.RegName.APSR_nzcv), new MCRegister(MCRegister.RegName.FPSCR))
                        .pushBacktoInstList();
            new Move(parent, dest, new ImmediateNumber(0)).pushBacktoInstList();

            MachineInstruction inst = new Move(parent, dest, new ImmediateNumber(1));
            inst.setCond(MachineInstruction.Condition.irToMCCond(cond));

            inst.pushBacktoInstList();

            valueMap.put(ir, dest);
            return dest;
        }
    }


}
