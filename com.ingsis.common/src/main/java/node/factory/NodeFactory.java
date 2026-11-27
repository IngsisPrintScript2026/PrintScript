/*
 * My Project
 */

package node.factory;

import java.util.List;
import node.AssignNode;
import node.CallFunctionNode;
import node.DeclarationKeywordNode;
import node.ExpressionNode;
import node.IdentifierNode;
import node.IfKeywordNode;
import node.OperatorNode;
import node.ProgramNode;
import node.StatementNode;
import node.WhileKeywordNode;
import node.expression.operator.OperatorType;
import node.keyword.declaration.DeclarationInfo;
import node.keyword.declaration.DeclarationType;
import token.Token;

public final class NodeFactory {

    private NodeFactory() {}

    public static IdentifierNode createIdentifier(Token token) {
        return new IdentifierNode(
                token.value(), token.startPosition().line(), token.startPosition().column());
    }

    public static DeclarationKeywordNode createDeclaration(
            DeclarationInfo info, IdentifierNode id, ExpressionNode expr) {
        return new DeclarationKeywordNode(
                info.type(),
                id,
                expr,
                info.declaredType(),
                info.keyword().startPosition().line(),
                info.keyword().startPosition().column());
    }

    public static DeclarationKeywordNode createDeclaration(
            DeclarationType type, IdentifierNode id, ExpressionNode expr, Token keyword) {
        return new DeclarationKeywordNode(
                type,
                id,
                expr,
                null,
                keyword.startPosition().line(),
                keyword.startPosition().column());
    }

    public static AssignNode createAssign(IdentifierNode id, ExpressionNode expr, Token idToken) {
        return new AssignNode(
                id, expr, idToken.startPosition().line(), idToken.startPosition().column());
    }

    public static CallFunctionNode createCall(
            String functionName, List<ExpressionNode> args, Token token) {
        IdentifierNode id =
                new IdentifierNode(
                        functionName, token.startPosition().line(), token.startPosition().column());
        return new CallFunctionNode(
                id, args, token.startPosition().line(), token.startPosition().column());
    }

    public static IfKeywordNode createIf(
            ExpressionNode cond,
            List<StatementNode> thenBody,
            List<StatementNode> elseBody,
            Token ifToken) {
        return new IfKeywordNode(
                cond,
                thenBody,
                elseBody,
                ifToken.startPosition().line(),
                ifToken.startPosition().column());
    }

    public static OperatorNode createOperator(
            OperatorType type, ExpressionNode left, ExpressionNode right, Token opToken) {
        return new OperatorNode(
                type,
                left,
                right,
                opToken.startPosition().line(),
                opToken.startPosition().column());
    }

    public static ProgramNode createProgram(List<StatementNode> statements) {
        return new ProgramNode(statements, 1, 1);
    }

    public static WhileKeywordNode createWhile(
            ExpressionNode cond, List<StatementNode> body, Token whileToken) {
        return new WhileKeywordNode(
                cond, body, whileToken.startPosition().line(), whileToken.startPosition().column());
    }
}
