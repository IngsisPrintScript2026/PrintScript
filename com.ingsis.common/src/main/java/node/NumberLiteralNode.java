/*
 * My Project
 */

package node;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import position.Position;
import position.SourceRange;
import result.Result;

public record NumberLiteralNode(BigDecimal rawValue, Integer line, Integer column)
        implements LiteralNode<BigDecimal> {

    @Override
    public Result<BigDecimal> value() {
        return Result.success(rawValue);
    }

    @Override
    public SourceRange range() {
        return SourceRange.spanning(new Position(line, column), rawValue.toPlainString());
    }

    @Override
    public List<ExpressionNode> children() {
        return Collections.emptyList();
    }

    @Override
    public String symbol() {
        return rawValue.toString();
    }
}
