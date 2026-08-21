package node.visitor;

import node.Node;
import node.ProgramNode;
import node.expression.function.CallFunctionNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;

public interface NodeVisitor<R, C> {
    R visit(DeclarationKeywordNode decl, C context);
    R visit(AssignNode assign, C context);
    R visit(IfKeywordNode ifNode, C context);
    R visit(CallFunctionNode call, C context);
    R visit(ProgramNode program, C context);
    R visitDefault(Node node, C context);
}
