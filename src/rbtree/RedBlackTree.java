package rbtree;

import java.util.ArrayList;
import java.util.List;

/*
* General Rules for Red-Black Trees:
* 1. Every node is RED or BLACK.
* 2. The root is ALWAYS BLACK.
* 3. Every null leaf (NIL) is ALWAYS BLACK.
* 4. If a node is RED, both of its children are ALWAYS BLACK.
* 5. All paths from any node to its child NIL leaf ALWAYS contain the same number
*       of BLACK nodes.
 */

/*
* These rules ensure that the tree's height is at most 2*log2(n+1),
* keeping searches, insertions and deletions at O(log n) worst case.
 */

/* The whole "project" itself, the rest of the files are just helpers or the Main file.
 * Starting here, we need to compare values inserted, so we take any generic "T" value.
 * The "extends" keyword is somewhat similar to Python's "import" keyword.
 */
public class RedBlackTree<T extends Comparable<T>> {

    // The "red-black" aspect of our tree.
    private static final boolean RED = true;
    private static final boolean BLACK = false;


    // Each node will store its own data, colour, and its position.
    private class Node {
        T data;
        boolean colour;
        Node left, right, parent;

        Node(T data, boolean colour, Node parent) {
            this.data = data;
            this.colour = colour;
            this.parent = parent;
            this.left = NIL;
            this.right = NIL;
        }
    }

    // Leaves point here instead of null.
    /*
    * Instead of defining new nodes as "null", we create a dummy node.
    * This dummy node will ALWAYS:
    * - Represent leaf nodes
    * - Colour them BLACK
    * - Prevent null pointer errors (prevent headaches for me).
     */
    private final Node NIL;

    private Node root;
    private int size;

    // Analysis counters
    private long totalRotations = 0;
    private long totalRecolourings = 0;
    private long totalInsertions = 0;
    private long totalSearches = 0;
    private long totalComparisons = 0;

    // API
    // This is the constructor for the tree.
    // It always starts empty and points to our dummy NIL node.
    public RedBlackTree() {
        NIL  = new Node(null, BLACK, null);
        NIL.left = NIL;
        NIL.right = NIL;
        root = NIL;
        size = 0;
    }

    public void insert(T value) {
        totalInsertions++;

        /* When inserting a node, it is coloured RED.
        * As we may know, RBTs must always be balanced.
        * RED nodes allow us to temporarily "bend" the tree.
        * This is important so that we do not need to fully rebalance
        *   the whole tree during insertions and deletions.
         */
        Node z = new Node(value, RED, NIL);

        // BST inserts
        Node y = NIL;
        Node x = root;
        while (x != NIL) {
            y = x;
            totalComparisons++;
            // "cmp" is short for compare
            int cmp = value.compareTo(x.data);
            if (cmp < 0) x = x.left;
            else if (cmp > 0) x = x.right;
            else return; // if dupe, ignore
        }

        // If the tree is empty, the new node becomes the root.
        z.parent = y;
        if (y == NIL) root = z;
        else {
            totalInsertions++;
            if (value.compareTo(y.data) <= 0) y.left = z;
            else y.right = z;
        }

        size++;

        /*
        * Remember the rules at the top?
        * This new node that was inserted is coloured red and
        *   also became the root, but this violates rule 2.
        * Thus, we call an important "insertFixup" function.
         */
        insertFixup(z);
    }

    // These are mainly just setters and getters,
    //  methods that alter or retrieve existing data.
    public T search(T value) {
        totalSearches++;
        Node n = searchNode(value);
        return (n == NIL) ?  null : n.data;
    }

    public List<T> rangeSearch(T low, T high) {
        List<T> results = new ArrayList<>();
        rangeSearch(root, low, high, results);
        return results;
    }

    public void delete(T value) {
        Node z = searchNode(value);
        if (z == NIL) return;
        deleteNode(z);
        size--;
    }

    public T minimum() {
        if (root == NIL) return null;
        return treeMinimum(root).data;
    }

    public T maximum() {
        if (root == NIL) return null;
        return treeMaximum(root).data;
    }

    public List<T> inOrder() {
        List<T> results = new ArrayList<>();
        inOrderHelper(root, results);
        return results;
    }

    // Analysis helpers
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int height() { return heightHelper(root); }
    public int blackHeight() { return blackHeightHelper(root); }

    public long getTotalRotations() { return totalRotations; }
    public long getTotalRecolourings() { return totalRecolourings; }
    public long getTotalInsertions() { return totalInsertions; }
    public long getTotalSearches() { return totalSearches; }
    public long getTotalComparisons() { return totalComparisons; }

    public void resetCounters() {
        totalRotations = 0;
        totalRecolourings = 0;
        totalInsertions = 0;
        totalSearches = 0;
        totalComparisons = 0;
    }

    // Rule Verification
    // Essentially just a violation logger.
    public String verifyRules() {
        // No Rules Violated
        if (root == NIL) return "";

        // Rule 2 Violation
        if (root.colour != BLACK) return "VIOLATION: root is not BLACK";

        // Rule 3-5 Violations
        int[] blackH = {-1};
        String result = verifyRulesHelper(root, 0, blackH);
        return result == null ? "" : result;
    }

    // Rotations
    private void leftRotate(Node x) {
        totalRotations++;
        Node y = x.right;
        x.right = y.left;

        if (y.left != NIL) y.left.parent = x;

        // Warnings here can be safely ignored.
        // If you do not see them then that's even better
        //  because they're just false-positives.
        y.parent = x.parent;
        if (x.parent == NIL) root = y;
        else if (x == x.parent.left) x.parent.left = y;
        else x.parent.right = y;

        y.left = x;
        x.parent = y;
    }

    private void rightRotate(Node y) {
        totalRotations++;
        Node x = y.left;
        y.left = x.right;

        if (x.right != NIL) x.right.parent = y;

        x.parent = y.parent;
        if (y.parent == NIL) root = x;
        else if (y == y.parent.right) y.parent.right = x;
        else y.parent.left = x;

        // Same goes for the warning here.
        x.right = y;
        y.parent = x;
    }

    // Insert Fixup
    // This will restore Red-Black properties after a RED node is inserted.
    // There are three cases for needing a fixup.
    private void insertFixup(Node z) {
        // RED nodes cannot have RED parents, violates rule 4.
        while (z.parent.colour == RED) {
            if (z.parent == z.parent.parent.left) {
                Node y = z.parent.parent.right;

                // CASE 1: Uncle is RED, thus recolour and move up.
                if (y.colour == RED) {
                    z.parent.colour = BLACK;
                    y.colour = BLACK;
                    z.parent.parent.colour = RED;
                    totalRecolourings += 3;
                    z = z.parent.parent;
                }
                else {
                    // CASE 2: Uncle is BLACK, "z" is right child, thus rotate left.
                    if (z == z.parent.right) {
                        z = z.parent;
                        leftRotate(z);
                    }

                    // CASE 3: Uncle is BLACK, "z" is left child, thus recolour and rotate right.
                    z.parent.colour = BLACK;
                    z.parent.parent.colour = RED;
                    totalRecolourings += 2;
                    rightRotate(z.parent.parent);
                }
            }
            else {
                // Parent is right child of grandparent
                Node y = z.parent.parent.left;

                if (y.colour == RED) {
                    z.parent.colour = BLACK;
                    y.colour = BLACK;
                    z.parent.parent.colour = RED;
                    totalRecolourings += 3;
                    z = z.parent.parent;
                }
                else {
                    if (z == z.parent.left) {
                        z = z.parent;
                        rightRotate(z);
                    }
                    z.parent.colour = BLACK;
                    z.parent.parent.colour = RED;
                    totalRecolourings += 2;
                    leftRotate(z.parent.parent);
                }
            }
        }
        // Usually on the first node insertion where it is coloured RED.
        if (root.colour != BLACK) {
            root.colour = BLACK;
            totalInsertions++;
        }
    }

    // Deletions
    private void deleteNode(Node z) {
        Node y = z;
        Node x;
        boolean yOriginalColor = y.colour;

        if (z.left == NIL) {
            x = z.right;
            transplant(z, z.right);
        } else if (z.right == NIL) {
            x = z.left;
            transplant(z, z.left);
        } else {
            y = treeMinimum(z.right);
            yOriginalColor = y.colour;
            x = y.right;
            if (y.parent == z) {
                x.parent = y;
            } else {
                transplant(y, y.right);
                y.right        = z.right;
                y.right.parent = y;
            }
            transplant(z, y);
            y.left        = z.left;
            y.left.parent = y;
            y.colour = z.colour;
        }

        if (yOriginalColor == BLACK)
            deleteFixup(x);
    }

    // Deletion Fixup
    // Deleting BLACK nodes usually ends with rule 5 violations.
    private void deleteFixup(Node x) {
        while (x != root && x.colour == BLACK) {
            if (x == x.parent.left) {
                Node w = x.parent.right;   // sibling

                if (w.colour == RED) {
                    // Case 1: Sibling node is RED.
                    // Thus, we rotate, which then leads to another case below.
                    w.colour = BLACK;
                    x.parent.colour = RED;
                    totalRecolourings += 2;
                    leftRotate(x.parent);
                    w = x.parent.right;
                }
                if (w.left.colour == BLACK && w.right.colour == BLACK) {
                    // Case 2: Sibling's children are BLACK.
                    // Thus, we recolour the sibling.
                    w.colour = RED;
                    totalRecolourings++;
                    x = x.parent;
                } else {
                    if (w.right.colour == BLACK) {
                        // Case 3: One RED child (inner node).
                        // Thus, we rotate the sibling.
                        w.left.colour = BLACK;
                        w.colour = RED;
                        totalRecolourings += 2;
                        rightRotate(w);
                        w = x.parent.right;
                    }
                    // Case 4: One RED child (outer node).
                    // Thus, perform a final rotation and recolour.
                    w.colour = x.parent.colour;
                    x.parent.colour = BLACK;
                    w.right.colour = BLACK;
                    totalRecolourings += 3;
                    leftRotate(x.parent);
                    x = root;
                }
            } else {
                // Mirror
                Node w = x.parent.left;

                if (w.colour == RED) {
                    w.colour = BLACK;
                    x.parent.colour = RED;
                    totalRecolourings += 2;
                    rightRotate(x.parent);
                    w = x.parent.left;
                }
                if (w.right.colour == BLACK && w.left.colour == BLACK) {
                    w.colour = RED;
                    totalRecolourings++;
                    x = x.parent;
                } else {
                    if (w.left.colour == BLACK) {
                        w.right.colour = BLACK;
                        w.colour = RED;
                        totalRecolourings += 2;
                        leftRotate(w);
                        w = x.parent.left;
                    }
                    w.colour = x.parent.colour;
                    x.parent.colour = BLACK;
                    w.left.colour = BLACK;
                    totalRecolourings += 3;
                    rightRotate(x.parent);
                    x = root;
                }
            }
        }
        if (x.colour != BLACK) {
            x.colour = BLACK;
            totalRecolourings++;
        }
    }

    // Transplant
    // Replaces subtree positions.
    private void transplant(Node u, Node v) {
        if (u.parent == NIL) root = v;
        else if (u == u.parent.left) u.parent.left = v;
        else u.parent.right = v;
        v.parent = u.parent;
    }

    // Utilities
    /*
    * Node searches
    * Range searches
    * Find tree minimum and maximum
    * In-order assistance
    * Find tree height
    * Find tree height but only BLACK nodes
    * Rule Verification helper
    * JSON Export helper
     */
    private Node searchNode(T value) {
        Node x = root;
        while (x != NIL) {
            totalComparisons++;
            int cmp = value.compareTo(x.data);
            if (cmp < 0) x = x.left;
            else if (cmp > 0) x = x.right;
            else return x;
        }
        return NIL;
    }

    private void rangeSearch(Node x, T low, T high, List<T> results) {
        if (x == NIL) return;
        totalComparisons++;
        if (low.compareTo(x.data) < 0)
            rangeSearch(x.left, low, high, results);
        totalComparisons++;
        if (low.compareTo(x.data) <= 0 && high.compareTo(x.data) >= 0)
            results.add(x.data);
        totalComparisons++;
        if (high.compareTo(x.data) > 0)
            rangeSearch(x.right, low, high, results);
    }

    private Node treeMinimum(Node x) {
        while (x.left != NIL) x = x.left;
        return x;
    }

    private Node treeMaximum(Node x) {
        while (x.right != NIL) x = x.right;
        return x;
    }

    private void inOrderHelper(Node x, List<T> result) {
        if (x == NIL) return;
        inOrderHelper(x.left, result);
        result.add(x.data);
        inOrderHelper(x.right, result);
    }

    private int heightHelper(Node x) {
        if (x == NIL) return 0;
        return 1 + Math.max(heightHelper(x.left), heightHelper(x.right));
    }

    private int blackHeightHelper(Node x) {
        if (x == NIL) return 1;
        int left = blackHeightHelper(x.left);
        return left + (x.colour == BLACK ? 1 : 0);
    }

    private String verifyRulesHelper(Node x, int currentBlack, int[] expectedBlack) {
        if (x == NIL) {
            if (expectedBlack[0] == -1) expectedBlack[0] = currentBlack;
            else if (currentBlack != expectedBlack[0])
                return "VIOLATION: inconsistent black-height (" +
                        currentBlack + ", " + expectedBlack[0] + ")";
            return null;
        }

        if (x.colour == RED) {
            if (x.left.colour == RED) return "VIOLATION: RED node has RED left child";
            if (x.right.colour == RED) return "VIOLATION: RED node has RED right child";
        }

        int b = currentBlack + (x.colour == BLACK ? 1 : 0);
        String left = verifyRulesHelper(x.left, b, expectedBlack);
        if (left != null) return left;
        String right = verifyRulesHelper(x.right, b, expectedBlack);
        return right;
    }

    public void exportNodes(NodeVisitor<T> visitor) {
        exportHelper(root, visitor);
    }

    private void exportHelper(Node x, NodeVisitor<T> visitor) {
        if (x == NIL) return;
        int leftId = (x.left == NIL) ? -1 : System.identityHashCode(x.left);
        int rightId = (x.right == NIL) ? -1 : System.identityHashCode(x.right);
        visitor.visit(System.identityHashCode(x), x.data, x.colour == RED,
                leftId, rightId);
        exportHelper(x.left, visitor);
        exportHelper(x.right, visitor);
    }

    public interface NodeVisitor<T> {
        void visit(int id, T data, boolean isRed, int leftId, int rightId);
    }
}