package pass;


//===----------------------------------------------------------------------===//
/// ModulePass class - This class is used to implement unstructured
/// interprocedural optimizations and analyses.  ModulePasses may do anything
/// they want to the program.
///
public class ModulePass extends Pass{
    public ModulePass() {
        super(PassKind.Module);
    }

    /**
     * 将该ModulePass在Module上运行的接口，需要重写
     */
    public void runOnModule(Module M){

    }
}
