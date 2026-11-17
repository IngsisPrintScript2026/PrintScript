/*
 * My Project
 */

package node.expression.operator;

import java.util.List;
import node.expression.ExpressionNode;
import position.Position;
import position.SourceRange;

public record OperatorNode(
        OperatorType operatorType,
        ExpressionNode left,
        ExpressionNode right,
        Integer line,
        Integer column)
        implements ExpressionNode {

    @Override
    public String symbol() {
        return operatorType.symbol();
    }

    /** Va desde el inicio del operando izquierdo hasta el fin del derecho. */
    @Override
    public SourceRange range() {
        SourceRange own = SourceRange.at(new Position(line, column));
        SourceRange from = left != null ? left.range() : own;
        SourceRange to = right != null ? right.range() : own;
        Position start = from.start() != null ? from.start() : own.start();
        Position end = to.end() != null ? to.end() : own.end();
        return new SourceRange(start, end);
    }

    @Override
    public List<ExpressionNode> children() {
        return List.of(left, right);
    }
}
