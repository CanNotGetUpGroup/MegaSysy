package pass.passes;

import analysis.CallGraph;
import ir.*;
import ir.Module;
import ir.instructions.Instructions.*;

import ir.instructions.Instructions;
import org.antlr.v4.runtime.misc.Pair;
import pass.ModulePass;

import java.util.*;

/**
 * 《Engineering a compiler》8.7.1
 * 《Advanced Compiler Design and Implementation》15.2-15.3
 */
public class FuncInline extends ModulePass {
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
        int firstCall=callSites.size();
        for(int i=0;i<firstCall;i++){
            if(functions.contains(callSites.get(i).a.getCalledFunction())){
                Pair<CallInst,Integer> pair=callSites.get(i);
                callSites.set(i--,callSites.get(--firstCall));
                callSites.set(firstCall,pair);
            }
        }

        boolean localChanged;
        do{
            localChanged=false;
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
                }else{

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
    public void inlineFunction(){

    }

    /**
     * 判断是否值得inline
     * @param CI
     * @return
     */
    public boolean shouldInline(CallInst CI){
        Function caller=CI.getFunction();
        Function F=CI.getCalledFunction();
        boolean isRecursive=false;
        HashMap<Value,Constant> SimplifiedValues=new HashMap<>();

        for(User U:caller.getUsers()){
            CallInst call=(CallInst)U;
            if(call.getFunction()==caller){
                isRecursive=true;
                break;
            }
        }
        Iterator<Value> CArgIt=CI.getArgs().iterator();
        for(Argument arg:F.getArguments()){
            Value CArg=CArgIt.next();
            if(CArg instanceof Constant){
                Constant C=(Constant)CArg;
                SimplifiedValues.put(arg,C);
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return "Function Inline";
    }
}
