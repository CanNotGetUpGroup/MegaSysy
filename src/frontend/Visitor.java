package frontend;

import ir.Module;
import ir.Value;
import util.MyIRBuilder;

import java.util.HashMap;
import java.util.logging.Logger;

public class Visitor extends SysyBaseVisitor<Value> {
    static MyIRBuilder builder;
    static Module module;
    static HashMap<String, Value> NamedValues;
    static Logger logger;
    static Value LogError(String str){
        logger.info(str);
        return null;
    }

}
