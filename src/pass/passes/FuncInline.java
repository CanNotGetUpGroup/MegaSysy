package pass.passes;

import analysis.CallGraph;
import ir.Function;
import ir.Module;

import pass.ModulePass;

import java.util.HashSet;
import java.util.Set;

public class FuncInline extends ModulePass {
    public FuncInline() {
        super();
    }

    @Override
    public void runOnModule(Module M) {
        CallGraph CG=new CallGraph(M);
        Set<Function> functions=new HashSet<>();

    }

    /**
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

    @Override
    public String getName() {
        return "Function Inline";
    }
}
