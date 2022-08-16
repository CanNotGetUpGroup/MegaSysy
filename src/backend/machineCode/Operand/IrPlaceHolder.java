package backend.machineCode.Operand;

import ir.Instruction;

public class IrPlaceHolder extends Register {

    public Instruction getIr() {
        return ir;
    }

    Instruction ir;

    public IrPlaceHolder(Instruction ir) {
        super(Type.PlaceHolder, ir.getType().isFloatTy() ? Content.Float : Content.Int);
        this.ir = ir;
    }

    @Override
    public String toString() {
        return "holder(" + ir + ")";

    }
}
