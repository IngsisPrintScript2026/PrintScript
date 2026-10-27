/*
 * My Project
 */

package sca.fake;

import java.math.BigDecimal;
import java.util.List;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;

/** Small builders for hand-made AST nodes used by the SCA unit tests. */
public final class ScaNodes {
    private ScaNodes() {}

    public static DeclarationKeywordNode let(String name, int line, int column) {
        return new DeclarationKeywordNode(
                DeclarationType.LET,
                new IdentifierNode(name, line, column),
                number(1),
                DataType.NUMBER,
                line,
                column);
    }

    public static DeclarationKeywordNode declare(String name, ExpressionNode init) {
        return new DeclarationKeywordNode(
                DeclarationType.LET, new IdentifierNode(name, 1, 5), init, DataType.STRING, 1, 1);
    }

    public static CallFunctionNode call(String function, ExpressionNode... args) {
        return new CallFunctionNode(new IdentifierNode(function, 1, 1), List.of(args), 1, 1);
    }

    public static OperatorNode sumAt(int line, int column) {
        return new OperatorNode(OperatorType.PLUS, number(1), number(2), line, column);
    }

    public static NumberLiteralNode number(int value) {
        return new NumberLiteralNode(BigDecimal.valueOf(value), 1, 1);
    }

    public static StringLiteralNode text(String value) {
        return new StringLiteralNode(value, 1, 1);
    }

    public static IdentifierNode variable(String name) {
        return new IdentifierNode(name, 1, 1);
    }
}
