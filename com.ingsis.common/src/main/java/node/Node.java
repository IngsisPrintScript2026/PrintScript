package node;

import node.visitor.NodeVisitor;

import java.util.List;

//Composite an PODO (Plain Old Data Object)
public interface Node {
    Integer line();
    Integer column();
    String symbol();
    List<? extends Node> children();

    default <R, C> R accept(NodeVisitor<R, C> visitor, C context) {
        return visitor.visitDefault(this, context);
    }
}

