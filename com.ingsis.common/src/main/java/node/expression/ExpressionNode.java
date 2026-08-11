package node.expression;

import node.Node;

import java.util.List;

public interface ExpressionNode extends Node {

    List<ExpressionNode> children();

    String symbol();
}

