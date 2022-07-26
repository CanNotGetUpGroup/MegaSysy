package pass.passes;

import analysis.CallGraph;
import ir.*;
import ir.Module;
import ir.instructions.Instructions.*;

import ir.instructions.Instructions;
import org.antlr.v4.runtime.misc.Pair;
import pass.ModulePass;
import util.IListIterator;

import java.util.*;

/**
 * 《Engineering a compiler》8.7.1
 * 《Advanced Compiler Design and Implementation》15.2-15.3
 */
public class FuncInline extends ModulePass {
    int Threshold=150;//内联的函数的行数阈值，超过该值放弃内联

    public FuncInline() {
        super();
    }

    @Override
    public void runOnModule(Module M) {
        CallGraph CG=new CallGraph(M);
        Set<Function> functions=CG.getCallNodes().keySet();
        ArrayList<Pair<CallInst,Integer>> callSites=new ArrayList<>();
        ArrayList<Pair<Function,Integer>> inlineHistory=new ArrayList<>();

        for(Function F:M.getFuncList()){
            if(!functions.contains(F)){
                continue;
            }
            for(BasicBlock BB:F.getBbList()){
                for(Instruction I:BB.getInstList()){
                    if(!(I instanceof CallInst)){
                        continue;
                    }
                    CallInst CI=(CallInst)I;
                    //调用的函数是引入的函数，不能内联
                    if(!CI.getCalledFunction().isDefined()){
                        continue;
                    }
                    callSites.add(new Pair<>(CI,-1));
                }
            }
        }
        if(callSites.isEmpty()){
            return;
        }

        boolean localChanged;
        do{
            localChanged=false;

            int firstCall=callSites.size();
            //把没有调用其它函数，或只调用了引入函数的函数放在前面处理
            for(int i=0;i<firstCall;i++){
                if(functions.contains(callSites.get(i).a.getCalledFunction())){
                    Pair<CallInst,Integer> pair=callSites.get(i);
                    callSites.set(i--,callSites.get(--firstCall));
                    callSites.set(firstCall,pair);
                }
            }

            for(int i=0;i<callSites.size();i++){
                Pair<CallInst,Integer> pair=callSites.get(i);
                CallInst CI=pair.a;
                int callSite=pair.b;
                Function caller=CI.getFunction();
                Function callee=CI.getCalledFunction();

                //CallInst未被使用
                if(CI.getUseList().isEmpty()){
                    CG.getNode(caller).removeCall(CI);
                    CI.remove();
                }
                if(functions.contains(callee)){
                    continue;
                }
                //不能或不值得inline
                if(!shouldInline(CI)) continue;
                //内联失败
                if(!inlineFunction(CI)){
                    System.out.println(callee+" didn't inline into "+caller);
                    continue;
                }

            }
        }while(localChanged);
    }

    /** TODO：
     * 1. 参数处理: 处理Caller传递给Callee的调用参数
     * 根据值传递，引用传递创建实参变量(alloc内存)
     * 将callee中的BasicBlock中对参数的引用替换为对新建变量的引用
     *
     * 2. 函数体拷贝
     * 将Callee的BasicBlock拷贝到Caller的CallSite位置
     *
     * 3. 参数返回值处理
     * 将CallSite对函数的引用替换为return指令返回变量的引用
     *
     * 4. Caller优化
     * Callee拷贝至Caller后，对Caller做简单的代码优化
     *
     */
    public boolean inlineFunction(CallInst CI){
        Function caller=CI.getFunction();
        Function callee=CI.getCalledFunction();
        HashMap<Value,Constant> ArgToVal=new HashMap<>();

        Iterator<Value> CArgIt=CI.getArgs().iterator();
        for(Argument arg:callee.getArguments()){
            Value CArg=CArgIt.next();
            if(CArg instanceof Constant){
                Constant C=(Constant)CArg;
                ArgToVal.put(arg,C);
            }
        }

        //在caller的开头插入alloca
        IListIterator<Instruction,BasicBlock> callerHead= (IListIterator<Instruction,BasicBlock>) caller.getEntryBB().getInstList().iterator();
        BasicBlock insertBB=callee.getEntryBB();
        for(Instruction I:insertBB.getInstList()){
            if(!(I instanceof AllocaInst)){
                continue;
            }
            AllocaInst AI=(AllocaInst)I;
            if(AI.getUseList().isEmpty()){
                AI.remove();
                continue;
            }
//            caller.getEntryBB().getInstList().splice(callerHead);
        }

        return true;
    }

    public boolean inlineHistoryQuery(Function F,int historyId,ArrayList<Pair<Function,Integer>> inlineHistory){
        while(historyId!=-1){
            if(inlineHistory.get(historyId).a==F){
                return true;
            }
            historyId=inlineHistory.get(historyId).b;
        }
        return false;
    }

    /**
     * 判断是否可以，是否值得inline
     * 递归函数不选择内联
     */
    public boolean shouldInline(CallInst CI){
        Function caller=CI.getFunction();
        Function F=CI.getCalledFunction();
        HashMap<Value,Constant> constantArgs=new HashMap<>();
        int cost=0;

        //检查是否递归调用
        for(User U:caller.getUsers()){
            CallInst call=(CallInst)U;
            if(call.getFunction()==caller){
                return false;
            }
        }
        Iterator<Value> CArgIt=CI.getArgs().iterator();
        //记录常量传参
        for(Argument arg:F.getArguments()){
            Value CArg=CArgIt.next();
            if(CArg instanceof Constant){
                Constant C=(Constant)CArg;
                constantArgs.put(arg,C);
            }
        }
        ArrayList<BasicBlock> liveInlineBB=new ArrayList<>();
        liveInlineBB.add(F.getEntryBB());
        for(int i=0;i<liveInlineBB.size();i++){
            BasicBlock BB=liveInlineBB.get(i);
            if(BB.front()==null){
                continue;
            }
            for(Instruction I:BB.getInstList()){
                cost++;
                if(cost>=Threshold){
                    return false;
                }
            }
            liveInlineBB.addAll(BB.getSuccessors());
        }
        return cost<Threshold;
    }

    @Override
    public String getName() {
        return "Function Inline";
    }
}
