package pass;

import backend.CodeGenManager;

public class MCPass extends Pass {
    public MCPass() {
        super(PassKind.MachineCode);
    }

    /**
     * 将该MCPass在CodeGen上运行的接口，需要重写
     */
    public void runOnCodeGen(CodeGenManager CGM){

    }
}
