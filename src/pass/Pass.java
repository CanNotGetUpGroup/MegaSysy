package pass;

import ir.Module;

public abstract class Pass {
    enum PassKind {
        Loop,
        Function,
        CallGraphSCC,
        Module,
        PassManager
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

    public void runOnModule(Module M){

    }
}
