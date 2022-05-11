package ir.instructions;

import ir.instructions.Instructions.*;
import ir.Type;
import ir.Value;

public class CastInst extends UnaryInstruction {
    public CastInst(Type type,Ops op, Value V) {
        super(type,op, V);
    }

    public static CastInst create(Ops op, Value S, Type Ty) {
        switch (op) {
            case ZExt:
                return new ZExtInst(Ty, S);
            case SIToFP:
                return new SIToFPInst(Ty, S);
            case FPToSI:
                return new FPToSIInst(Ty,S);
        }
        return null;
    }

}
