package node;


import node.visitor.NodeVisitor;

import java.util.List;

public record ProgramNode(List<Node> statements, Integer line, Integer column) implements Node {

    public ProgramNode {
        statements = List.copyOf(statements);
    }

    @Override
    public String symbol() {
        return "PROGRAM";
    }

    @Override
    public List<Node> children() {
        return statements;
    }

    @Override
    public <R, C> R accept(NodeVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }

}
