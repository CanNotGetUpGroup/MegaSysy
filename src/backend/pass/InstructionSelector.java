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
import util.IListNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class InstructionSelector {
    private Module module;
    private ArrayList<MachineFunction> funcList;
    private ArrayList<MachineDataBlock> globalDataList;
    private boolean optimize = false;

    public InstructionSelector(Module module, boolean optimize) {
        this.module = module;
        funcList = new ArrayList<>();
        globalDataList = new ArrayList<>();
        this.optimize = optimize;
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
            mf.setLoopInfo(f.getLoopInfo());

            if (f.isDefined()) mf.setDefined(true);

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

        // initialize all machine basic block
        // prologue block
        MachineBasicBlock firstbb = new MachineBasicBlock(mf, "Prologue");
        firstbb.setLoopDepth(0);
        firstbb.pushBacktoBBList();

        while (bbList.getLast() != null && head != bbList.getLast()) {
            head = head.getNext();
            var bb = head.getVal();
            MachineBasicBlock mbb = new MachineBasicBlock(mf);
            mbb.setLoopDepth(irFunction.getLoopInfo().getLoopDepthForBB(bb));
            mbb.pushBacktoBBList();
            bbMap.put(bb, mbb);
        }
        firstbb.addSuccessor(bbMap.get(irFunction.getEntryBB()));

        // insert sub stack instruction
        // TODO: need modify
        var reg0 = new VirtualRegister();
        new LoadImm(firstbb, reg0, new StackOffsetNumber(0, mf)).pushBacktoInstList();
        new Arithmetic(firstbb, Arithmetic.Type.SUB, new MCRegister(MCRegister.RegName.SP), reg0).pushBacktoInstList();

        for (var bb : bbList) {
            for (var inst : bb.getInstList()) {
                if (inst.getOp() == Instruction.Ops.Call) {
                    mf.setLeaf(false);

                    // reserve space for parameter on stack
                    int paraNum = inst.getNumOperands();

                    int floatParaNum = 0, intParaNum = 0;
                    int firstStackFloatPara = paraNum, firstStackIntPara = paraNum;
                    for (int i = 1; i < paraNum; i++) {
                        if (inst.getOperand(i).getType().isFloatTy()) {
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
                }
            }
        }
        mf.addStackSize(mf.getMaxParaNumOnStack() * 4);

        // set up parameters, move all to virtual parameter
        var paras = irFunction.getArguments();
        int floatNum = 0, intNum = 0, onStackNum = 0;
        for (var para : paras) {
            Register dest;

            if (para.getType().isFloatTy()) {
                dest = new VirtualRegister(Register.Content.Float);
                if (floatNum < 16) { // is on reg
                    new Move(firstbb, dest, new MCRegister(Register.Content.Float, floatNum)).setForFloat(true).pushBacktoInstList();
                    floatNum++;
                } else {
                    // TODO: don't know size now ???  cur solution: Load a stackOffset number, and change it after reg allocation
                    var reg = new VirtualRegister();
                    new LoadImm(firstbb, reg, new StackOffsetNumber(onStackNum * 4, mf, true)).pushBacktoInstList();
                    new Arithmetic(firstbb, Arithmetic.Type.ADD, reg, new MCRegister(MCRegister.RegName.SP)).pushBacktoInstList();
                    new LoadOrStore(firstbb, LoadOrStore.Type.LOAD, dest, new Address(reg)).setForFloat(true).pushBacktoInstList();
                    onStackNum++;
                }
            } else { // para is not float, can be array or int
                dest = new VirtualRegister();
                if (intNum < 4) {
                    new Move(firstbb, dest, new MCRegister(Register.Content.Int, intNum)).pushBacktoInstList();
                    intNum++;
                } else {
                    var reg = new VirtualRegister();
                    new LoadImm(firstbb, reg, new StackOffsetNumber(onStackNum * 4, mf, true)).pushBacktoInstList();
                    new LoadOrStore(firstbb, LoadOrStore.Type.LOAD, dest, new Address(new MCRegister(MCRegister.RegName.SP), reg)).pushBacktoInstList();
                    onStackNum++;
                }
            }
            mf.getValueMap().put(para, dest);
        }

        // translate basic block (add instructions for basic block)
        head = bbList.getHead();
        while (bbList.getLast() != null && head != bbList.getLast()) {
            head = head.getNext();
            var bb = head.getVal();

            translateBB(bb);
        }

        var valueMap = mf.getValueMap();
        for (var bb : mf.getBbList()) {
            for (var i : bb.getInstList()) {
                if (i.getOp1() instanceof IrPlaceHolder) {
                    i.setOp1(valueMap.get(((IrPlaceHolder) i.getOp1()).getIr()));
                }
                if (i.getOp2() instanceof IrPlaceHolder) {
                    i.setOp2(valueMap.get(((IrPlaceHolder) i.getOp2()).getIr()));
                }
                if (i.getOp2() instanceof Address) {
                    Address add = (Address) i.getOp2();
                    if (add.getReg() instanceof IrPlaceHolder) {
                        add.setReg(valueMap.get(((IrPlaceHolder) add.getReg()).getIr()));
                    }
                    if (add.getOffset() instanceof IrPlaceHolder) {
                        add.setOffset(valueMap.get(((IrPlaceHolder) add.getOffset()).getIr()));
                    }
                }
            }
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
        var allocaMap = mf.getAllocaMap();
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

                if (ir.getNumOperands() == 1) { // unconditional branch
                    var dest = ir.getOperand(0);
                    var mdest = mf.getBBMap().get((BasicBlock) dest);
                    var inst = new Branch(mbb, mdest, false, Branch.Type.Block);
                    inst.pushBacktoInstList();
                    mbb.addSuccessor(mdest);
                    mdest.addPredInst(inst);
                } else { // conditional branch
                    if (ir.getOperand(0) instanceof Constants.ConstantInt) {
                        int val = ((Constants.ConstantInt) (ir.getOperand(0))).getVal();
                        if (val == 0) {
                            var dest = mf.getBBMap().get(ir.getOperand(1));
                            var inst = new Branch(mbb, dest, false, Branch.Type.Block);
                            inst.pushBacktoInstList();
                            mbb.addSuccessor(dest);
                            dest.addPredInst(inst);
                        } else {  // must be 1
                            var dest = mf.getBBMap().get(ir.getOperand(2));
                            var inst = new Branch(mbb, dest, false, Branch.Type.Block);
                            inst.pushBacktoInstList();
                            mbb.addSuccessor(dest);
                            dest.addPredInst(inst);
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
                            new VMRS(mbb, new MCRegister(MCRegister.RegName.APSR_nzcv), new MCRegister(MCRegister.RegName.FPSCR)).pushBacktoInstList();
                        // TODO: change the ir.getOperand(2) after merge
                        // TODO: Float number
                        var dest = mf.getBBMap().get(ir.getOperand(2));
                        MachineInstruction inst = new Branch(mbb, dest, false, Branch.Type.Block);
                        mbb.addSuccessor(dest);
                        inst.setCond(MachineInstruction.Condition.irToMCCond(op));
                        inst.pushBacktoInstList();
                        dest.addPredInst(inst);

                        dest = mf.getBBMap().get(ir.getOperand(1));
                        inst = new Branch(mbb, dest, false, Branch.Type.Block);
                        inst.pushBacktoInstList();
                        mbb.addSuccessor(dest);
                        dest.addPredInst(inst);
                    }
                }
                node = node.getNext();
                node.getVal().setforBr(true);
            }
            case Call -> {
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

                for (int i = 4, stackPos = 0; i < paraNum; i++) {
                    var op = ir.getOperand(i);
                    if (op.getType().isFloatTy()) {
                        if (i < firstStackFloatPara) continue;
                        new LoadOrStore(mbb, LoadOrStore.Type.STORE, valueToFloatReg(mbb, op), new Address(new MCRegister(MCRegister.RegName.SP), stackPos * 4)).setForFloat(true).pushBacktoInstList();
                    } else {
                        if (i < firstStackIntPara) continue;
                        new LoadOrStore(mbb, LoadOrStore.Type.STORE, valueToReg(mbb, op), new Address(new MCRegister(MCRegister.RegName.SP), stackPos * 4)).pushBacktoInstList();
                    }
                    stackPos++;
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
                    var contentType = (DerivedTypes.ArrayType) pType.getElementType();
                    var size = contentType.getEleSize() * contentType.getNumElements();

                    // 分配栈空间
                    var dest = new VirtualRegister();
                    var inst = new Arithmetic(mbb, Arithmetic.Type.ADD, dest, new MCRegister(MCRegister.RegName.SP), mf.getStackSize());

                    inst.pushBacktoInstList();


                    // 保存一下位置

                    valueMap.put(ir, dest);
                    allocaMap.put(dest, mf.getStackSize());

                    mf.addStackSize(size * 4);


                } else {
                    // int or float
                    var dest = new VirtualRegister();
                    // 分配栈空间

                    new Arithmetic(mbb, Arithmetic.Type.ADD, dest, new MCRegister(MCRegister.RegName.SP), mf.getStackSize()).pushBacktoInstList();

                    // 保存一下位置
                    valueMap.put(ir, dest);
                    allocaMap.put(dest, mf.getStackSize());

                    mf.addStackSize(4);
                }
            }
            case Store -> {
                Address addr;
                Register op1 = valueToReg(mbb, ir.getOperand(0));

                Register reg = valueMap.get(ir.getOperand(1));
                if (reg != null && allocaMap.containsKey(reg)) {
                    addr = getLegalSpAddr(allocaMap.get(reg), op1.getContent(), mbb);
                } else {
                    Register op2 = valueToReg(mbb, ir.getOperand(1));
                    addr = new Address(op2);
                }
                new LoadOrStore(mbb, LoadOrStore.Type.STORE, op1, addr)
                        .setForFloat(op1.getContent() == Register.Content.Float)
                        .pushBacktoInstList();
            }
            case Load -> {
                Address addr;
                Register dest = new VirtualRegister(ir.getType().isFloatTy() ? Register.Content.Float : Register.Content.Int);
                valueMap.put(ir, dest);
                Register reg = valueMap.get(ir.getOperand(0));

                if (reg != null && allocaMap.containsKey(reg)) {
                    addr = getLegalSpAddr(allocaMap.get(reg), dest.getContent(), mbb);
                } else {
                    Register src = valueToReg(mbb, ir.getOperand(0));
                    addr = new Address(src);
                }

                new LoadOrStore(mbb, LoadOrStore.Type.LOAD, dest, addr).setForFloat(ir.getType().isFloatTy()).pushBacktoInstList();
            }
            case GetElementPtr -> {
                var curIr = (Instructions.GetElementPtrInst) ir;

                var irOp = ir.getOperand(0);
                var srcAddr = valueToReg(mbb, irOp);
                if (curIr.allIndicesZero()) {
                    valueMap.put(ir, srcAddr);
                    break;
                }

                var offset = curIr.getOperand(curIr.getNumOperands() - 1);
//                var size = curIr.get====
                if (allocaMap.containsKey(srcAddr) && offset instanceof Constants.ConstantInt) {
                    int constOffset = allocaMap.get(srcAddr) + 4 * ((Constants.ConstantInt) offset).getVal();
                    var reg = new VirtualRegister();
                    var inst = new Arithmetic(mbb, Arithmetic.Type.ADD, reg, new MCRegister(MCRegister.RegName.SP), constOffset);
                    inst.pushBacktoInstList();
                    valueMap.put(ir, reg);
                    allocaMap.put(reg, constOffset);
                } else {
                    var irOffset = valueToReg(mbb, offset);
                    var dest = new VirtualRegister();
                    var inst = new Arithmetic(mbb, Arithmetic.Type.ADD, dest, srcAddr, irOffset);
                    inst.setShifter(Shift.Type.LSL, 2);
                    inst.pushBacktoInstList();
                    valueMap.put(ir, dest);
                }
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
                    new VMRS(mbb, new MCRegister(MCRegister.RegName.APSR_nzcv), new MCRegister(MCRegister.RegName.FPSCR)).pushBacktoInstList();

                new Move(mbb, dest, new ImmediateNumber(0)).pushBacktoInstList();
                MachineInstruction inst = new Move(mbb, dest, new ImmediateNumber(1));

                inst.setCond(MachineInstruction.Condition.irToMCCond(op));
                inst.pushBacktoInstList();

                valueMap.put(ir, dest);
            }

            case Add -> {
                MCOperand op1 = valueToMCOperand(mbb, ir.getOperand(0)), op2 = valueToMCOperand(mbb, ir.getOperand(1));

                Register dest = new VirtualRegister();
                valueMap.put(ir, dest);

                if (op1 instanceof ImmediateNumber) {
                    MCOperand tmp = op1;
                    op1 = op2;
                    op2 = tmp;
                }
                if (op1 instanceof ImmediateNumber) {
                    Register opp1 = new VirtualRegister();
                    new Move(mbb, opp1, op1).pushBacktoInstList();
                    op1 = opp1;
                }

                Arithmetic.Type mcType = Arithmetic.Type.ADD;
                new Arithmetic(mbb, mcType, dest, (Register) op1, op2).pushBacktoInstList();
            }
            case Sub -> {
                Value irOp1 = ir.getOperand(0), irOp2 = ir.getOperand(1);

                Register dest = new VirtualRegister();
                valueMap.put(ir, dest);

                Register op1;
                MCOperand op2;
                Arithmetic.Type type = Arithmetic.Type.SUB;
                if (irOp1 instanceof Constants.ConstantInt
                        && ImmediateNumber.isLegalImm(((Constants.ConstantInt) irOp1).getVal())) {
                    op2 = new ImmediateNumber(((Constants.ConstantInt) irOp1).getVal());
                    op1 = valueToReg(mbb, irOp2);
                    type = Arithmetic.Type.RSB;
                } else if (irOp2 instanceof Constants.ConstantInt
                        && ImmediateNumber.isLegalImm(((Constants.ConstantInt) irOp2).getVal())) {
                    op2 = new ImmediateNumber(((Constants.ConstantInt) irOp2).getVal());
                    op1 = valueToReg(mbb, irOp1);
                } else {
                    op1 = valueToReg(mbb, irOp1);
                    op2 = valueToReg(mbb, irOp2);
                }

                new Arithmetic(mbb, type, dest, op1, op2).pushBacktoInstList();
            }
            case Shl, Shr -> {
                Register op1 = valueToReg(mbb, ir.getOperand(0));
                MCOperand op2 = valueToMCOperand(mbb, ir.getOperand(1));

                Register dest = new VirtualRegister();
                valueMap.put(ir, dest);
                new Arithmetic(mbb, switch (ir.getOp()) {
                    case Shl -> Arithmetic.Type.LSL;
                    case Shr -> Arithmetic.Type.ASR;
                    default -> null;
                }, dest, op1, op2).pushBacktoInstList();

            }
            case Mul -> {
                var irOp1 = ir.getOperand(0);
                var irOp2 = ir.getOperand(1);

                if (irOp1 instanceof Constants.ConstantInt && irOp2 instanceof Constants.ConstantInt) {
                    // doesn't matter, middle-end will delete this situation
                    Register op1 = valueToReg(mbb, ir.getOperand(0)), op2 = valueToReg(mbb, ir.getOperand(1));

                    Register dest = new VirtualRegister();
                    valueMap.put(ir, dest);
                    new Arithmetic(mbb, Arithmetic.Type.MUL, dest, op1, op2).pushBacktoInstList();
                } else if (irOp1 instanceof Constants.ConstantInt || irOp2 instanceof Constants.ConstantInt) {
                    Register op1;
                    int val;
                    var dest = new VirtualRegister();
                    if (irOp1 instanceof Constants.ConstantInt) {
                        op1 = valueToReg(mbb, ir.getOperand(1));
                        val = ((Constants.ConstantInt) irOp1).getVal();
                    } else {
                        op1 = valueToReg(mbb, ir.getOperand(0));
                        val = ((Constants.ConstantInt) irOp2).getVal();
                    }
                    if (val > 0 && isPowerOfTwo(val)) {
                        new Arithmetic(mbb, Arithmetic.Type.LSL, dest, op1, log2(val)).pushBacktoInstList();
                        valueMap.put(ir, dest);
                        break;
                    } else if (val > 0 && isPowerOfTwo(val + 1)) {
                        var inst = new Arithmetic(mbb, Arithmetic.Type.RSB, dest, op1, op1);
                        inst.setShifter(Shift.Type.LSL, log2(val + 1));
                        inst.pushBacktoInstList();
                        valueMap.put(ir, dest);
                        break;
                    } else if (val > 0 && isPowerOfTwo(val - 1)) {
                        var inst = new Arithmetic(mbb, Arithmetic.Type.ADD, dest, op1, op1);
                        inst.setShifter(Shift.Type.LSL, log2(val - 1));
                        inst.pushBacktoInstList();
                        valueMap.put(ir, dest);
                        break;
                    }
                }
                Register op1 = valueToReg(mbb, ir.getOperand(0)), op2 = valueToReg(mbb, ir.getOperand(1));

                Register dest = new VirtualRegister();
                valueMap.put(ir, dest);
                new Arithmetic(mbb, Arithmetic.Type.MUL, dest, op1, op2).pushBacktoInstList();

            }
            case SDiv -> {
                var irOp1 = ir.getOperand(0);
                var irOp2 = ir.getOperand(1);
                Register op1 = valueToReg(mbb, ir.getOperand(0));
                if (irOp2 instanceof Constants.ConstantInt) {
                    int divisor = ((Constants.ConstantInt) irOp2).getVal();
                    boolean isNeg = divisor < 0;
                    divisor = isNeg ? -divisor : divisor;
                    Register ans = null;
                    System.out.println(ir);
                    var choose = ChooseMultiplier(divisor, 31);
                    long m = choose.m;
                    int l = choose.l, sh = choose.sh;
                    boolean canDo = true;
                    if (divisor == 1) {
                        ans = op1;
                    } else if (isPowerOfTwo(divisor)) {
                        var temp = new VirtualRegister();
                        new Arithmetic(mbb, Arithmetic.Type.ASR, temp, op1, l - 1).pushBacktoInstList();
                        new Arithmetic(mbb, Arithmetic.Type.LSR, temp, 32 - l).pushBacktoInstList();
                        new Arithmetic(mbb, Arithmetic.Type.ADD, temp, op1).pushBacktoInstList();
                        new Arithmetic(mbb, Arithmetic.Type.ASR, temp, l).pushBacktoInstList();
                        ans = temp;
                    } else {
                        // TODO: 长乘法
                        canDo = false;
                    }
                    if (canDo) {
                        if (isNeg) {
                            new Arithmetic(mbb, Arithmetic.Type.RSB, ans, 0).pushBacktoInstList();
                        }
                        valueMap.put(ir, ans);
                        break;
                    }
                }


                Register op2 = valueToReg(mbb, ir.getOperand(1));

                Register dest = new VirtualRegister();
                valueMap.put(ir, dest);
                new Arithmetic(mbb, Arithmetic.Type.SDIV, dest, op1, op2).pushBacktoInstList();
            }
            case SRem -> {
                var op2 = ir.getOperand(1);
                int val = -1;
                if (op2 instanceof Constants.ConstantInt) val = ((Constants.ConstantInt) op2).getVal();
                if (optimize && op2 instanceof Constants.ConstantInt && isPowerOfTwo(val)) {
                    if (val < 0) val = -val;
                    var r1 = valueToMCOperand(mbb, ir.getOperand(0));
                    var copy = new VirtualRegister();
                    var reverse = new VirtualRegister();
                    new Move(mbb, copy, r1).pushBacktoInstList();
                    var inst = new Arithmetic(mbb, Arithmetic.Type.RSB, reverse, copy, 0);
                    inst.setSetState(true);
                    inst.pushBacktoInstList();
                    if (ImmediateNumber.isLegalImm(val - 1)) {
                        new Arithmetic(mbb, Arithmetic.Type.AND, copy, copy, val - 1).pushBacktoInstList();
                        new Arithmetic(mbb, Arithmetic.Type.AND, reverse, reverse, val - 1).pushBacktoInstList();
                    } else {
                        new Ubfx(mbb, copy, copy, 0, log2(val)).pushBacktoInstList();
                        new Ubfx(mbb, reverse, reverse, 0, log2(val)).pushBacktoInstList();
                    }
                    inst = new Arithmetic(mbb, Arithmetic.Type.RSB, copy, reverse, 0);
                    inst.setCond(MachineInstruction.Condition.PL);
                    inst.pushBacktoInstList();

                    valueMap.put(ir, copy);
                } else {
                    mf.setLeaf(false);
                    Register r1 = valueToReg(mbb, ir.getOperand(0)), r2 = valueToReg(mbb, ir.getOperand(1));


                    new Move(mbb, new MCRegister(Register.Content.Int, 0), r1).pushBacktoInstList();
                    new Move(mbb, new MCRegister(Register.Content.Int, 1), r2).pushBacktoInstList();


                    new Branch(mbb, "__aeabi_idivmod", true, Branch.Type.Call).pushBacktoInstList();


                    var dest = new VirtualRegister();
                    new Move(mbb, dest, new MCRegister(MCRegister.RegName.r1)).pushBacktoInstList();
                    valueMap.put(ir, dest);
                }
            }

            case FAdd, FDiv, FMul, FSub -> {
                Register r1 = valueToReg(mbb, ir.getOperand(0)), r2 = valueToReg(mbb, ir.getOperand(1));
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
                }, dest, r1, r2).setForFloat(new ArrayList<>(List.of("f32"))).pushBacktoInstList();
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

    boolean isLegalOffset(int value, Register.Content type) {
        if (type == Register.Content.Float) {
            return value < 1024 && value > -1024;
        } else { // INT
            return value > -4096 && value < 4096;
        }
    }

    Address getLegalSpAddr(int offset, Register.Content type, MachineBasicBlock mbb) {
        if (isLegalOffset(offset, type)) {
            return new Address(new MCRegister(MCRegister.RegName.SP), offset);
        } else {
            var reg = new VirtualRegister();
            new Arithmetic(mbb, Arithmetic.Type.ADD, reg, new MCRegister(MCRegister.RegName.SP), offset)
                    .pushBacktoInstList();
            return new Address(reg);
        }
    }

    class ChooseMultiplierAns {
        public int getL() {
            return l;
        }

        public int getSh() {
            return sh;
        }

        public long getM() {
            return m;
        }

        int l;
        int sh;
        long m;

        public ChooseMultiplierAns(int l, int sh, long m) {
            this.l = l;
            this.sh = sh;
            this.m = m;
        }
    }

    ChooseMultiplierAns ChooseMultiplier(int d, int prec) {
        int l = log2(d), sh;
        long mLow = 1, mHigh;
        if (!isPowerOfTwo(d))
            l = l + 1;
        sh = l;
        for (int i = 0; i < 32 + l; i++) {
            mLow *= 2;
        }
        mHigh = mLow;
        mLow /= d;
        long temp = 1;
        for (int i = 0; i < 32 + l - prec; i++) {
            temp *= 2;
        }
        mHigh += temp;
        mHigh /= d;
        while (mLow / 2 < mHigh / 2 && sh > 0) {
            mLow /= 2;
            mHigh /= 2;
            sh--;
        }
        return new ChooseMultiplierAns(l, sh, mHigh);
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
                if (ImmediateNumber.isLegalImm(value)) return new ImmediateNumber(value);
                else {
                    // not a legal immediate number, has to load a literal value
                    Register dest = new VirtualRegister();
                    new LoadImm(parent, dest, value).insertBefore(node);
                    return dest;
                }
            } else if (type.isFloatTy()) {
                float value = ((Constants.ConstantFP) val).getVal();
                if (value == 0) {
                    return new ImmediateNumber(0);
                }
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
                if (val instanceof CmpInst) return i1ToReg(parent, val);
                else
                    return new IrPlaceHolder((Instruction) val);
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
        MCOperand res = valueToMCOperandInsertBefore(parent, val, node);
        if (res instanceof Register) return (Register) res;
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
        } else return reg;
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
                if (!r1.isFloat()) {
                    var rr1 = new VirtualRegister(Register.Content.Float);
                    new Move(parent, rr1, r1).setForFloat(true).pushBacktoInstList();

                    r1 = rr1;
                } else if (r2 instanceof Register && !((Register) r2).isFloat()) {
                    var rr2 = new VirtualRegister(Register.Content.Float);
                    new Move(parent, rr2, r2).setForFloat(true).pushBacktoInstList();

                    r2 = rr2;
                }
            }
            new Cmp(parent, r1, r2).setForFloat(((CmpInst) ir).getOp() == Instruction.Ops.FCmp, new ArrayList<>(List.of("F32"))).pushBacktoInstList();
            if (((CmpInst) ir).getOp() == Instruction.Ops.FCmp)
                new VMRS(parent, new MCRegister(MCRegister.RegName.APSR_nzcv), new MCRegister(MCRegister.RegName.FPSCR)).pushBacktoInstList();
            new Move(parent, dest, new ImmediateNumber(0)).pushBacktoInstList();

            MachineInstruction inst = new Move(parent, dest, new ImmediateNumber(1));
            inst.setCond(MachineInstruction.Condition.irToMCCond(cond));

            inst.pushBacktoInstList();

            valueMap.put(ir, dest);
            return dest;
        }
    }

    public static boolean isPowerOfTwo(int n) {
        if (n < 0) n = -n;
        return n > 0 && (n & (n - 1)) == 0;
    }

    public static int log2(int N) {

        // calculate log2 N indirectly
        // using log() method
        int result = (int) (Math.log(N) / Math.log(2));

        return result;
    }

    public static void main(String[] args) {
        int x = 1;
        for (int i = 0; i < 31; i++, x *= 2) {
            System.out.println(log2(x));
        }
    }


}
