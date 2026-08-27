/*
 * My Project
 */

package node;

import java.util.List;
import node.visitor.NodeVisitor;

// Composite an PODO (Plain Old Data Object)
public interface Node {
    Integer line();

    Integer column();

    String symbol();

    List<? extends Node> children();

    default <R, C> R accept(NodeVisitor<R, C> visitor, C context) {
        return visitor.visitDefault(this, context);
    }
}
