package analysis;

import ir.BasicBlock;
import ir.Function;
import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

/**
 * 参考资料：https://blog.csdn.net/dashuniuniu/article/details/52224882?ops_request_misc=%257B%2522request%255Fid%2522%253A%2522162795675516780366525582%2522%252C%2522scm%2522%253A%252220140713.130102334.pc%255Fblog.%2522%257D&request_id=162795675516780366525582&biz_id=0&utm_medium=distribute.pc_search_result.none-task-blog-2~blog~first_rank_v2~rank_v29-1-52224882.pc_v2_rank_blog_default&utm_term=%E6%94%AF%E9%85%8D%E6%A0%91
 * 由CFG生成DominatorTree
 */
public class DominatorTree {
    public Function Parent;
    public TreeNode Root;
    public HashMap<BasicBlock, TreeNode> DomTreeNodes;
    private final ArrayList<TreeNode> PostOrder;// 后序遍历CFG
    private final ArrayList<TreeNode> ReversePostOrder;// 逆后序遍历CFG
    private final ArrayList<TreeNode> DTPostOrder;// 后序遍历DT
    public HashMap<TreeNode, ArrayList<TreeNode>> DominanceFrontier;
    private final ArrayList<TreeNode> JoinNodes;// CFG中拥有多个前驱的节点

    public DominatorTree(Function F) {
        DomTreeNodes = new HashMap<>();
        PostOrder = new ArrayList<>();
        ReversePostOrder = new ArrayList<>();
        DTPostOrder = new ArrayList<>();
        DominanceFrontier = new HashMap<>();
        JoinNodes = new ArrayList<>();

        computeOnFunction(F);
    }

    /**
     * 消除不可到达的基本块
     */
    public boolean removeUnreachableBB() {
        boolean ret = false;
        for (BasicBlock BB : Parent.getBbList()) {
            if (getNode(BB) == null) {
                ret = true;
                BB.removeThisAndAllInst();
            }
        }
        return ret;
    }

    /**
     * 按照CFG生成TreeNode，IDom信息在CalculateDomTree中生成
     */
    public void initTreeNode(TreeNode p) {
        if (p == null)
            return;
        for (var child : p.BB.getSuccessors()) {
            if (DomTreeNodes.containsKey(child)) {
                DomTreeNodes.get(child).Predecessors.add(p);
                JoinNodes.add(DomTreeNodes.get(child));
                continue;
            }
            TreeNode newP = new TreeNode(child, p);
            DomTreeNodes.put(child, newP);
            initTreeNode(newP);
        }
    }

    public void computeOnFunction(Function F) {
        Parent = F;
        TreeNode root = new TreeNode(F.getEntryBB());
        Root = root;
        DomTreeNodes.put(F.getEntryBB(), Root);
        initTreeNode(root);
        removeUnreachableBB();
        calculateDomTree();
        calculateDomFrontier();
        updateDFSNumbers();
    }

    public void update(Function F) {
        clear();
        computeOnFunction(F);
    }

    public void clear() {
        Parent = null;
        Root = null;
        DomTreeNodes.clear();
        PostOrder.clear();
        ReversePostOrder.clear();
        DTPostOrder.clear();
        DominanceFrontier.clear();
        JoinNodes.clear();
    }

    public ArrayList<TreeNode> getPostOrder() {
        if (PostOrder.size() == 0) {
            Set<TreeNode> visited = new HashSet<>();
            PostOrderDFS(Root, visited,PostOrder);
        }
        return PostOrder;
    }

    /**
     * 后序遍历
     */
    private void PostOrderDFS(TreeNode p, Set<TreeNode> visited, ArrayList<TreeNode> PostOrder) {
        visited.add(p);
        for (var child : p.BB.getSuccessors()) {
            if (!visited.contains(getNode(child))) {
                PostOrderDFS(getNode(child), visited,PostOrder);
            }
        }
        p.setPostNumber(PostOrder.size());
        PostOrder.add(p);
    }

    public ArrayList<TreeNode> getReversePostOrder() {
        if (ReversePostOrder.size() == 0) {
            var tmp = getPostOrder();
            for (int i = tmp.size() - 1; i >= 0; i--) {
                ReversePostOrder.add(tmp.get(i));
            }
        }
        return ReversePostOrder;
    }

    public ArrayList<TreeNode> getDTPostOrder() {
        if (DTPostOrder.size() == 0) {
            DTPostOrderDFS(Root);
        }
        return DTPostOrder;
    }

    private void DTPostOrderDFS(TreeNode p) {
        for (var child : p.Children) {
            DTPostOrderDFS(child);
        }
        DTPostOrder.add(p);
    }

    /**
     * A是否支配B
     */
    public boolean dominates(BasicBlock A, BasicBlock B) {
        if (B == A)
            return true;
        TreeNode TA = DomTreeNodes.get(A);
        TreeNode TB = DomTreeNodes.get(B);
        if (TB.IDom == TA)
            return true;
        if (TA.IDom == TB)
            return false;
        if (TA.level >= TB.level)
            return false;
        return TB.dominatedBy(TA);
    }

    public TreeNode findSharedParent(TreeNode A, TreeNode B) {
        while (A != B) {
            while (A.PostNumber < B.PostNumber) {
                A = A.IDom;
            }
            while (A.PostNumber > B.PostNumber) {
                B = B.IDom;
            }
        }
        return A;
    }

    public TreeNode getNode(BasicBlock BB) {
        return DomTreeNodes.get(BB);
    }

    /**
     * 计算支配树节点的IDom信息，Cooper的A Simple, Fast Dominance Algorithm
     * 参考：https://www.cs.rice.edu/~keith/EMBED/dom.pdf
     */
    public void calculateDomTree() {
        getReversePostOrder();
        boolean changed = true;
        Root.IDom = Root;
        while (changed) {
            changed = false;
            for (var cur : ReversePostOrder) {
                if (cur == Root) {
                    continue;
                }
                var PredDomNode = cur.Predecessors;
                TreeNode usefulNode = null;
                // 查找IDom不为null的前驱节点
                for (var pre : PredDomNode) {
                    if (pre.IDom != null) {
                        usefulNode = pre;
                        break;
                    }
                }
                // 查找公共父节点
                for (var pre : PredDomNode) {
                    if (pre == usefulNode) {
                        continue;
                    }
                    if (pre.IDom != null) {
                        usefulNode = findSharedParent(usefulNode, pre);
                    }
                }
                // 检查IDom是否改变
                if (cur.IDom != usefulNode) {
                    cur.setIDom(usefulNode);
                    changed = true;
                }
            }
        }
    }

    public void calculateDomFrontier() {
        TreeNode runner = null;
        for (var node : JoinNodes) {
            for (var pred : node.Predecessors) {
                runner = pred;
                while (runner != node.IDom) {
                    if (!DominanceFrontier.containsKey(runner)) {
                        DominanceFrontier.put(runner, new ArrayList<>());
                    }
                    DominanceFrontier.get(runner).add(node);
                    runner = runner.IDom;
                }
            }
        }
    }

    public void updateDFSNumbers() {
        Stack<Pair<TreeNode, Iterator<TreeNode>>> WorkStack = new Stack<>();
        TreeNode ThisRoot = Root;
        WorkStack.push(new Pair<>(ThisRoot, ThisRoot.Children.iterator()));

        int DFSNum = 0;
        ThisRoot.DFSInNum = DFSNum++;

        while (!WorkStack.empty()) {
            TreeNode Node = WorkStack.peek().a;
            Iterator<TreeNode> ChildIt = WorkStack.peek().b;
            if (!ChildIt.hasNext()) {
                Node.DFSOutNum = DFSNum++;
                WorkStack.pop();
            } else {
                TreeNode Child = ChildIt.next();

                WorkStack.push(new Pair<>(Child, Child.Children.iterator()));
                Child.DFSInNum = DFSNum++;
            }
        }
    }

    public static class TreeNode {
        public BasicBlock BB;
        public TreeNode IDom;// 直接支配节点
        public ArrayList<TreeNode> Children = new ArrayList<>();
        public int level;

        public TreeNode Father;//CFG中的前驱（只记录了一个）

        private int DFSInNum, DFSOutNum;
        private int PostNumber;// CFG前序遍历序号
        private ArrayList<TreeNode> Predecessors = new ArrayList<>();

        public TreeNode(BasicBlock BB, TreeNode Father) {
            this.BB = BB;
            this.IDom = null;
            this.Father = Father;
            Predecessors.add(Father);
            this.level = 0;
            this.DFSInNum = this.DFSOutNum = this.PostNumber = -1;
        }

        public TreeNode(BasicBlock BB) {
            this.BB = BB;
            this.IDom = null;
            this.level = 0;
            this.DFSInNum = this.DFSOutNum = this.PostNumber = -1;
        }

        /**
         * 判断是否为叶节点
         */
        public boolean isLeaf() {
            return Children.isEmpty();
        }

        public int getChildrenNum() {
            return Children.size();
        }

        public void addChild(TreeNode child) {
            Children.add(child);
        }

        public boolean dominatedBy(TreeNode TA) {
//            TreeNode IDom, TB = this;
//            while ((IDom = TB.IDom) != TB && IDom != null && IDom.level >= TA.level) {
//                TB = IDom;
//            }
//            return TB == TA;
             return this.DFSInNum>=TA.DFSInNum&&this.DFSOutNum<=TA.DFSOutNum;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TreeNode)) {
                return false;
            }
            TreeNode other = (TreeNode) obj;
            if (getChildrenNum() != ((TreeNode) obj).getChildrenNum()) {
                return false;
            }
            if (level != other.level) {
                return false;
            }
            if (!BB.equals(other.BB)) {
                return false;
            }
            for (int i = 0; i < Children.size(); i++) {
                if (!Children.get(i).equals(other.Children.get(i))) {
                    return false;
                }
            }
            return true;
        }

        public void setIDom(TreeNode NP) {
            if (IDom == null) {
                IDom = NP;
                IDom.Children.add(this);
                updateLevel();
                return;
            }
            if (IDom.equals(NP)) {
                return;
            }
            assert IDom.Children.contains(this);
            IDom.Children.remove(this);
            IDom = NP;
            IDom.Children.add(this);
            updateLevel();
        }

        // 广度优先遍历更新level
        public void updateLevel() {
            if (level == IDom.level + 1)
                return;
            Stack<TreeNode> stack = new Stack<>();
            stack.push(this);
            while (!stack.isEmpty()) {
                TreeNode curr = stack.pop();
                curr.level = curr.IDom.level + 1;
                for (TreeNode treeNode : curr.Children) {
                    if (treeNode.level != treeNode.IDom.level + 1) {
                        stack.push(treeNode);
                    }
                }
            }
        }

        public int getDFSInNum() {
            return DFSInNum;
        }

        public void setDFSInNum(int DFSInNum) {
            this.DFSInNum = DFSInNum;
        }

        public int getDFSOutNum() {
            return DFSOutNum;
        }

        public void setDFSOutNum(int DFSOutNum) {
            this.DFSOutNum = DFSOutNum;
        }

        public int getPostNumber() {
            return PostNumber;
        }

        public void setPostNumber(int postNumber) {
            PostNumber = postNumber;
        }

        public ArrayList<TreeNode> getPredecessors() {
            return Predecessors;
        }

        public void setPredecessors(ArrayList<TreeNode> predecessors) {
            Predecessors = predecessors;
        }

        public ArrayList<TreeNode> getChildren() {
            return Children;
        }
    }
}
