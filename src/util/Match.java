package util;

import ir.*;

import java.util.regex.Matcher;

public abstract class Match {
    public static boolean compare(Value V,Match M){
        return M.match(V);
    }

    public abstract boolean match(Value V);

    public static Match createMatch(Value V){
        if(V instanceof Constant) return new MatchConst((Constant) V);
        else if(V instanceof Instruction) return new MatchInst((Instruction)V);
        else if(V instanceof Argument) return new MatchArgument((Argument) V);
        return null;
    }

    public static class MatchConst extends Match{
        Constant C;

        public MatchConst(Constant c) {
            C = c;
        }

        @Override
        public boolean match(Value V) {
            return C==V;//Constant同值实例唯一
        }
    }

    public static class MatchInst extends Match {
        Instruction I;

        public MatchInst(Instruction i) {
            I = i;
        }

        public boolean match(Value V){
            if(V==I) return true;
            if(!(V instanceof Instruction)) return false;
            Instruction IV=(Instruction)V;
            return false;
         }
    }

    public static class MatchArgument extends Match{
        Argument A;

        public MatchArgument(Argument a) {
            A = a;
        }

        @Override
        public boolean match(Value V) {
            return A==V;//判断参数是否相同
        }
    }

    public static class MatchBin extends Match{
        Match L,R;
        Instruction.Ops Op;

        public MatchBin(Match l, Match r, Instruction.Ops op) {
            L = l;
            R = r;
            assert Instruction.isBinary(op);
            Op = op;
        }

        public boolean match(Value V){
            if(!(V instanceof Instruction)) return false;
            Instruction I=(Instruction)V;
            if(I.getOp()!=Op) return false;
            return L.match(I.getOperand(0))&&R.match(I.getOperand(1))
                    ||(Instruction.isCommutative(Op)&&L.match(I.getOperand(1)))&&R.match(I.getOperand(0));
        }
    }

    /**
     * 用于获取value
     */
    public static class MatchUndef extends Match{
        Value V;

        @Override
        public boolean match(Value V) {
            if(V!=null){
                this.V=V;
                return true;
            }
            return false;
        }

        public Value getV() {
            return V;
        }
    }
}
