package pass;

public abstract class Pass {
    enum PassKind {
        Loop,
        Function,
        CallGraphSCC,
        Module,
        PassManager
    }
    private PassKind kind;

}
