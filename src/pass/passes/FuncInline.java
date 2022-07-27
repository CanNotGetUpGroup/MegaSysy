package pass.passes;

import analysis.CallGraph;
import ir.*;
import ir.Module;
import ir.instructions.Instructions.*;

import pass.ModulePass;
import util.CloneMap;
import util.IListIterator;
import util.MyIRBuilder;

import java.util.*;

/**
 * 《Engineering a compiler》8.7.1
 * 《Advanced Compiler Design and Implementation》15.2-15.3
 */
public class FuncInline extends ModulePass {
    int Threshold = 150;//内联的函数的行数阈值，超过该值放弃内联

    public FuncInline() {
        super();
    }

    @Override
    public void runOnModule(Module M) {
        CallGraph CG = new CallGraph(M);
        Set<Function> functions = CG.getCallNodes().keySet();
        ArrayList<CallInst> callInsts = new ArrayList<>();

        for (Function F : M.getFuncList()) {
            if (!functions.contains(F)) {
                continue;
            }
            for (BasicBlock BB : F.getBbList()) {
                for (Instruction I : BB.getInstList()) {
                    if (!(I instanceof CallInst)) {
                        continue;
                    }
                    CallInst CI = (CallInst) I;
                    //调用的函数是引入的函数，不能内联
                    if (!CI.getCalledFunction().isDefined()) {
                        continue;
                    }
                    callInsts.add((CI));
                }
            }
        }
        if (callInsts.isEmpty()) {
            return;
        }

        boolean localChanged;
        do {
            localChanged = false;

            int firstCall = callInsts.size();
            //把没有调用其它函数，或只调用了引入函数的函数放在前面处理
            for (int i = 0; i < firstCall; i++) {
                Function f=callInsts.get(i).getCalledFunction();
                if (CG.getNode(f).getCalledFunctions().size()>0) {
                    CallInst pair = callInsts.get(i);
                    callInsts.set(i--, callInsts.get(--firstCall));
                    callInsts.set(firstCall, pair);
                }
            }

            for (int i = 0; i < callInsts.size(); i++) {
                CallInst CI = callInsts.get(i);
                Function caller = CI.getFunction();
                Function callee = CI.getCalledFunction();

                if (CG.getNode(callee).getCalledFunctions().size()>0) {
                    continue;
                }
                //不能或不值得inline
                if (!shouldInline(CI)) continue;
                //内联失败
                if (!inlineFunction(CI)) {
                    System.out.println(callee + " didn't inline into " + caller);
                    continue;
                }else{
                    localChanged=true;
                    CG.getNode(caller).removeCall(CI);
                    callInsts.remove(i--);
                }
            }
        } while (localChanged);
        M.rename();
    }

    /**
     * 1. 参数处理: 处理Caller传递给Callee的调用参数
     * 根据值传递，引用传递创建实参变量(alloc内存)
     * 将callee中的BasicBlock中对参数的引用替换为对新建变量的引用
     * <p>
     * 2. 函数体拷贝
     * 将Callee的BasicBlock拷贝到Caller的CallSite位置
     * <p>
     * 3. 参数返回值处理
     * 将CallSite对函数的引用替换为return指令返回变量的引用
     * <p>
     * 4. Caller优化
     * Callee拷贝至Caller后，对Caller做简单的代码优化
     */
    public boolean inlineFunction(CallInst CI) {
        Function caller = CI.getFunction();
        Function callee = CI.getCalledFunction();
        HashMap<Value, Value> ArgToVal = new HashMap<>();
        //拷贝出一个Function，该Function用于内联，不在Module中
        CloneMap cloneMap = new CloneMap();
        Function copy = callee.copy(cloneMap);
        BasicBlock insertBB = copy.getEntryBB();
        insertBB.setComment("inline "+CI);
        BasicBlock originBB = CI.getParent();

        Iterator<Value> CArgIt = CI.getArgs().iterator();
        for (Argument arg : copy.getArguments()) {
            Value CArg = CArgIt.next();
            ArgToVal.put(arg, CArg);
        }

        //1. 参数处理: 处理Caller传递给Callee的调用参数
        for (Argument arg : copy.getArguments()) {
//            if(!(arg.getType() instanceof DerivedTypes.PointerType))
                arg.replaceAllUsesWith(ArgToVal.get(arg));
//            else{
//                ArrayList<Use> copyUseList=new ArrayList<>(arg.getUseList());
//                for(Use use:copyUseList){
//                    use.getU().setOperand(use.getOperandNo(),ArgToVal.get(arg));
//                    if(use.getU() instanceof StoreInst){
//                        AllocaInst alloca=(AllocaInst)use.getU().getOperand(1);
//                        ArrayList<Use> copyUseList1=new ArrayList<>(alloca.getUseList());
//                        for(Use ause:copyUseList1){
//                            if(ause.getU() instanceof LoadInst){
//                                ause.getU().replaceAllUsesWith(ArgToVal.get(arg));
//                            }
//                            (ause.getU()).remove();
//                        }
//                        alloca.remove();
//                    }
//                }
//            }
        }

        //2. 函数体拷贝
        /*
         * originBB:           originBB:
         * part1       --->    part1
         *
         *                     insertBB:
         *                     f body
         *
         *                     leaveBB:
         * call f()            callInst
         * part2               part2
         */
        //在caller的开头插入alloca
        IListIterator<Instruction, BasicBlock> callerHead = (IListIterator<Instruction, BasicBlock>) caller.getEntryBB().getInstList().iterator();
        MyIRBuilder builder = MyIRBuilder.getInstance();

        Iterator<Instruction> It = insertBB.getInstList().iterator();
        Instruction I = It.next();
        //将alloca移动到caller首部
        if (insertBB.front() instanceof AllocaInst) {
            AllocaInst AI = (AllocaInst) insertBB.front();
            while (I instanceof AllocaInst) {
                if (I.getUseList().isEmpty()) {
                    I.remove();
                }
                I = It.next();
            }
            caller.getEntryBB().getInstList().splice(callerHead, AI.getInstNode(), I.getInstNode());
        }

        BasicBlock leave = new BasicBlock("", caller, originBB);
        IListIterator<Instruction,BasicBlock> callInstIt= (IListIterator<Instruction, BasicBlock>) leave.getInstList().iterator();
        leave.getInstList().splice(callInstIt,CI.getInstNode(),null);
        leave.setComment("leave inline "+CI);
        //将originBB的phi指令转换为leave的
        for(PHIInst phiInst:originBB.getPHIs()){
            phiInst.replaceIncomingBlock(originBB,leave);
        }
        originBB.getPHIs().clear();
        builder.setInsertPoint(originBB);
        builder.createBr(copy.getEntryBB());

        //copy的最后一个指令的返回指令
        ReturnInst returnInst= (ReturnInst) copy.getBbList().getLast().getVal().getInstList().getLast().getVal();
        IListIterator<BasicBlock, Function> beforeLeaveIt=leave.getBbNode().getIterator();
        caller.getBbList().splice(beforeLeaveIt,copy.getBbList());
        //3. 参数返回值处理
        if(returnInst.getNumOperands()==1)
            CI.replaceAllUsesWith(returnInst.getOperand(0));
        CI.remove();
        builder.setInsertPoint(returnInst);
        builder.createBr(leave);
        returnInst.remove();
        //TODO:4.Caller优化
        return true;
    }

    /**
     * 判断是否可以，是否值得inline
     * 递归函数不选择内联
     */
    public boolean shouldInline(CallInst CI) {
        Function caller = CI.getFunction();
        Function F = CI.getCalledFunction();
        int cost = 0;

        //检查是否递归调用
        for (User U : caller.getUsers()) {
            CallInst call = (CallInst) U;
            if (call.getFunction() == caller) {
                return false;
            }
        }
        for (BasicBlock BB:F.getBbList()) {
            if (BB.front() == null) {
                continue;
            }
            for (Instruction I : BB.getInstList()) {
                cost++;
                if (cost >= Threshold) {
                    return false;
                }
            }
        }
        return cost < Threshold;
    }

    @Override
    public String getName() {
        return "Function Inline";
    }
}
