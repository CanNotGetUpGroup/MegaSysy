package util;

import ir.Constant;
import ir.Value;

import java.util.HashMap;

/**
 * 深拷贝一个函数时，记录被拷贝的Value
 */
public class CloneMap {
    private HashMap<Value,Value> cloneMap;

    public CloneMap() {
        this.cloneMap = new HashMap<>();
    }

    public Value get(Value V){
        if(V instanceof Constant){
            return V;
        }
        return cloneMap.get(V);
    }

    public void put(Value V,Value clonedV){
        cloneMap.put(V,clonedV);
    }
}
