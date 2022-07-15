package ir;

import java.util.ArrayList;
import java.util.Stack;

/**
 * 参考资料：https://blog.csdn.net/dashuniuniu/article/details/52224882?ops_request_misc=%257B%2522request%255Fid%2522%253A%2522162795675516780366525582%2522%252C%2522scm%2522%253A%252220140713.130102334.pc%255Fblog.%2522%257D&request_id=162795675516780366525582&biz_id=0&utm_medium=distribute.pc_search_result.none-task-blog-2~blog~first_rank_v2~rank_v29-1-52224882.pc_v2_rank_blog_default&utm_term=%E6%94%AF%E9%85%8D%E6%A0%91
 *
 */
public class DominatorTree {
    public Function Parent;
    private TreeNode Root;

    public DominatorTree(Function F) {
        computeTree(F);
    }

    public void computeTree(Function F){
        Parent=F;

    }

    /**
     * A是否支配B
     */
    public boolean dominates(BasicBlock A,BasicBlock B){
        return false;
    }

    public static class TreeNode{
        public BasicBlock BB;
        public TreeNode IDom;//直接支配节点
        public ArrayList<TreeNode> Children;
        public int level;

        public TreeNode(BasicBlock BB,TreeNode IDom) {
            this.BB = BB;
            this.IDom = IDom;
            this.level=(IDom ==null)?0: IDom.level+1;
            if(IDom !=null){
                IDom.Children.add(this);
            }
        }

        /**
         * 判断是否为叶节点
         */
        public boolean isLeaf(){
            return Children.isEmpty();
        }

        public int getChildrenNum(){
            return Children.size();
        }

        public void addChild(TreeNode child){
            Children.add(child);
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof TreeNode)){
                return false;
            }
            TreeNode other=(TreeNode)obj;
            if(getChildrenNum()!=((TreeNode) obj).getChildrenNum()){
                return false;
            }
            if(level!=other.level){
                return false;
            }
            if(!BB.equals(other.BB)){
                return false;
            }
            for(int i=0;i<Children.size();i++){
                if(!Children.get(i).equals(other.Children.get(i))){
                    return false;
                }
            }
            return true;
        }

        public void setIDom(TreeNode NP){
            if(IDom.equals(NP)){
                return;
            }
            assert IDom.Children.contains(this);
            IDom.Children.remove(this);
            IDom =NP;
            IDom.Children.add(this);
        }

        // 广度优先遍历更新level
        public void updateLevel(){
            if(level== IDom.level+1) return;
            Stack<TreeNode> stack=new Stack<>();
            stack.push(this);
            while(!stack.isEmpty()){
                TreeNode curr=stack.pop();
                curr.level=curr.IDom.level+1;
                for(TreeNode treeNode:curr.Children){
                    if(treeNode.level!=treeNode.IDom.level+1){
                        stack.push(treeNode);
                    }
                }
            }
        }
    }
}
