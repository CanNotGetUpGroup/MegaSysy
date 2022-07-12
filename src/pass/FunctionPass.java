package pass;

import ir.Function;
import ir.Module;

//===----------------------------------------------------------------------===//
/// FunctionPass class - This class is used to implement most global
/// optimizations.  Optimizations should subclass this class if they meet the
/// following constraints:
///
///  1. Optimizations are organized globally, i.e., a function at a time
///  2. Optimizing a function does not cause the addition or removal of any
///     functions in the module
///
public class FunctionPass extends Pass{
    public FunctionPass() {
        super(PassKind.Function);
    }

    /**
     * 将该FunctionPass在Function上运行的接口，需要重写
     */
    public void runOnFunction(Function F){

    }

    /**
     * 在M上的所有函数上运行该FunctionPass
     */
    public void runOnModule(Module M){
        for(Function F:M.getFuncList()){
            //跳过引入函数
            if(!F.isDefined()){
                continue;
            }
            runOnFunction(F);
        }
    }
}
