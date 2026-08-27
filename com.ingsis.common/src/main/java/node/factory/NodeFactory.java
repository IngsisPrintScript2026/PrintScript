package node.factory;

import node.Node;
import node.ProgramNode;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.operator.OperatorNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import token.Token;

import java.util.List;

public final class NodeFactory {

    private NodeFactory() {}

    public static IdentifierNode createIdentifier(Token token) {
        return new IdentifierNode(token.value(), token.startPosition().line(), token.startPosition().column());
    }

    public static DeclarationKeywordNode createDeclaration(DeclarationType type, IdentifierNode id, ExpressionNode expr, node.expression.literal.DataType declaredType, Token keyword) {
        return new DeclarationKeywordNode(type, id, expr, declaredType, keyword.startPosition().line(), keyword.startPosition().column());
    }

    public static DeclarationKeywordNode createDeclaration(DeclarationType type, IdentifierNode id, ExpressionNode expr, Token keyword) {
        return new DeclarationKeywordNode(type, id, expr, null, keyword.startPosition().line(), keyword.startPosition().column());
    }

    public static AssignNode createAssign(IdentifierNode id, ExpressionNode expr, Token idToken) {
        return new AssignNode(id, expr, idToken.startPosition().line(), idToken.startPosition().column());
    }

    public static CallFunctionNode createCall(String functionName, List<ExpressionNode> args, Token token) {
        IdentifierNode id = new IdentifierNode(functionName, token.startPosition().line(), token.startPosition().column());
        return new CallFunctionNode(id, args, token.startPosition().line(), token.startPosition().column());
    }

    public static IfKeywordNode createIf(ExpressionNode cond, List<Node> thenBody, List<Node> elseBody, Token ifToken) {
        return new IfKeywordNode(cond, thenBody, elseBody, ifToken.startPosition().line(), ifToken.startPosition().column());
    }

    public static node.expression.operator.OperatorNode createOperator(node.expression.operator.OperatorType type, ExpressionNode left, ExpressionNode right, Token opToken) {
        return new OperatorNode(type, left, right, opToken.startPosition().line(), opToken.startPosition().column());
    }

    public static ProgramNode createProgram(List<Node> statements) {
        return new ProgramNode(statements, 1, 1);
    }
}
