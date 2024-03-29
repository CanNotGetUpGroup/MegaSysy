package pass;

import ir.Module;

public abstract class Pass {
    enum PassKind {
        Function,
        Module,
        MachineCode,
    }
    private PassKind kind;

    public Pass(PassKind K){
        kind=K;
    }

    public PassKind getKind() {
        return kind;
    }

    public void setKind(PassKind kind) {
        this.kind = kind;
    }

    public String getName(){
        return "Pass";
    }

    public abstract void runOnModule(Module M);
}
