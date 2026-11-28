/*
 * My Project
 */

package sca.fake;

import java.math.BigDecimal;
import java.util.List;
import node.CallFunctionNode;
import node.DeclarationKeywordNode;
import node.ExpressionNode;
import node.IdentifierNode;
import node.NumberLiteralNode;
import node.OperatorNode;
import node.StringLiteralNode;
import node.expression.literal.DataType;
import node.expression.operator.OperatorType;
import node.keyword.declaration.DeclarationType;

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

    /** {@code 1 + 2} con el '+' en (line, column): ocupa de column - 2 a column + 2. */
    public static OperatorNode sumAt(int line, int column) {
        return new OperatorNode(
                OperatorType.PLUS,
                new NumberLiteralNode(BigDecimal.valueOf(1), line, column - 2),
                new NumberLiteralNode(BigDecimal.valueOf(2), line, column + 2),
                line,
                column);
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
