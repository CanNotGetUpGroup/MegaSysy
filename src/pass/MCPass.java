package pass;

import backend.CodeGenManager;

public abstract class MCPass extends Pass {
    public MCPass() {
        super(PassKind.MachineCode);
    }

    /**
     * 将该MCPass在CodeGen上运行的接口，需要重写
     */
    public abstract void runOnCodeGen(CodeGenManager CGM);
}
