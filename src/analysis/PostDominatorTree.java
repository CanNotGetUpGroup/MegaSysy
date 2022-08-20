package analysis;

import ir.BasicBlock;
import ir.Function;
import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

public class PostDominatorTree {
    public Function Parent;
    public DominatorTree.TreeNode PostRoot;
    public DominatorTree.TreeNode Root;
    public HashMap<BasicBlock, DominatorTree.TreeNode> DomTreeNodes;
    private final ArrayList<DominatorTree.TreeNode> PostOrder;// 后序遍历CFG
    private final ArrayList<DominatorTree.TreeNode> DTPostOrder;// 后序遍历DT
    private final ArrayList<DominatorTree.TreeNode> JoinNodes;// CFG中拥有多个前驱的节点

    public PostDominatorTree(Function F) {
        DomTreeNodes = new HashMap<>();
        PostOrder = new ArrayList<>();
        DTPostOrder = new ArrayList<>();
        JoinNodes = new ArrayList<>();

        computeOnFunction(F);
    }

    /**
     * 按照CFG生成TreeNode，IDom信息在CalculateDomTree中生成
     */
    public void initTreeNode(DominatorTree.TreeNode p) {
        if (p == null)
            return;
        for (var child : p.BB.getSuccessors()) {
            if (DomTreeNodes.containsKey(child)) {
                DomTreeNodes.get(child).Predecessors.add(p);
                JoinNodes.add(DomTreeNodes.get(child));
                continue;
            }
            DominatorTree.TreeNode newP = new DominatorTree.TreeNode(child, p);
            DomTreeNodes.put(child, newP);
            initTreeNode(newP);
        }
    }

    public void computeOnFunction(Function F) {
        Parent = F;
        Root=new DominatorTree.TreeNode(F.getEntryBB());
        DomTreeNodes.put(F.getEntryBB(),Root);
        initTreeNode(PostRoot);

        BasicBlock root = (F.getEntryBB());
        while(root.getSuccessorsNum()!=0){
            root=root.getSuccessor(0);
        }
        PostRoot=new DominatorTree.TreeNode(root);
        DomTreeNodes.put(root, PostRoot);
        calculateDomTree();
        updateDFSNumbers();
    }

    public void clear() {
        Parent = null;
        PostRoot = null;
        DomTreeNodes.clear();
        PostOrder.clear();
        DTPostOrder.clear();
        JoinNodes.clear();
    }

    public ArrayList<DominatorTree.TreeNode> getPostOrder() {
        if (PostOrder.size() == 0) {
            Set<DominatorTree.TreeNode> visited = new HashSet<>();
            PostOrderDFS(PostRoot, visited,PostOrder);
        }
        return PostOrder;
    }

    /**
     * 后序遍历
     */
    private void PostOrderDFS(DominatorTree.TreeNode p, Set<DominatorTree.TreeNode> visited, ArrayList<DominatorTree.TreeNode> PostOrder) {
        visited.add(p);
        for (var child : p.BB.getSuccessors()) {
            if (!visited.contains(getNode(child))) {
                PostOrderDFS(getNode(child), visited,PostOrder);
            }
        }
        p.setPostNumber(PostOrder.size());
        PostOrder.add(p);
    }

    public ArrayList<DominatorTree.TreeNode> getDTPostOrder() {
        if (DTPostOrder.size() == 0) {
            DTPostOrderDFS(Root);
        }
        return DTPostOrder;
    }

    private void DTPostOrderDFS(DominatorTree.TreeNode p) {
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
        DominatorTree.TreeNode TA = DomTreeNodes.get(A);
        DominatorTree.TreeNode TB = DomTreeNodes.get(B);
        if (TB.IDom == TA)
            return true;
        if (TA.IDom == TB)
            return false;
        if (TA.level >= TB.level)
            return false;
        return TB.dominatedBy(TA);
    }

    public DominatorTree.TreeNode findSharedParent(DominatorTree.TreeNode A, DominatorTree.TreeNode B) {
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

    public DominatorTree.TreeNode getNode(BasicBlock BB) {
        return DomTreeNodes.get(BB);
    }

    /**
     * 计算支配树节点的IDom信息，Cooper的A Simple, Fast Dominance Algorithm
     * 参考：https://www.cs.rice.edu/~keith/EMBED/dom.pdf
     */
    public void calculateDomTree() {
        getPostOrder();
        boolean changed = true;
        Root.IDom = Root;
        while (changed) {
            changed = false;
            for (var cur : PostOrder) {
                if (cur == Root) {
                    continue;
                }
                var PredDomNode = cur.Predecessors;
                DominatorTree.TreeNode usefulNode = null;
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

    public void updateDFSNumbers() {
        Stack<Pair<DominatorTree.TreeNode, Iterator<DominatorTree.TreeNode>>> WorkStack = new Stack<>();
        DominatorTree.TreeNode ThisRoot = Root;
        WorkStack.push(new Pair<>(ThisRoot, ThisRoot.Children.iterator()));

        int DFSNum = 0;
        ThisRoot.DFSInNum = DFSNum++;

        while (!WorkStack.empty()) {
            DominatorTree.TreeNode Node = WorkStack.peek().a;
            Iterator<DominatorTree.TreeNode> ChildIt = WorkStack.peek().b;
            if (!ChildIt.hasNext()) {
                Node.DFSOutNum = DFSNum++;
                WorkStack.pop();
            } else {
                DominatorTree.TreeNode Child = ChildIt.next();

                WorkStack.push(new Pair<>(Child, Child.Children.iterator()));
                Child.DFSInNum = DFSNum++;
            }
        }
    }
}
