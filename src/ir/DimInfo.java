package ir;

public class DimInfo extends User {
    public DimInfo(String name) {
        super(Type.getVoidTy(), name);
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof DimInfo)) return false;
        if(getNumOperands()!=((DimInfo) o).getNumOperands()) return false;
        for(int i=0;i<getNumOperands();i++){
            if(!getOperand(i).equals(((DimInfo) o).getOperand(i))){
                return false;
            }
        }
        return true;
    }
}
