package util;

import ir.Constant;
import ir.Function;
import ir.Value;

import java.util.HashMap;

/**
 * 深拷贝一个函数时，记录被拷贝的Value
 */
public class CloneMap {
    private HashMap<Value,Value> cloneMap;
    private boolean inFunctionCopy;

    public CloneMap() {
        this.cloneMap = new HashMap<>();
    }

    public Value get(Value V){
        if(V instanceof Constant||(V instanceof Function && (!((Function)V).isDefined()||inFunctionCopy))){
            return V;
        }
        return cloneMap.get(V);
    }

    public boolean isInFunctionCopy() {
        return inFunctionCopy;
    }

    public void setInFunctionCopy(boolean inFunctionCopy) {
        this.inFunctionCopy = inFunctionCopy;
    }

    public void put(Value V, Value clonedV){
        cloneMap.put(V,clonedV);
    }
}
