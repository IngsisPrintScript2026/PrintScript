/*
 * My Project
 */

package node;

import java.util.Collections;
import java.util.List;
import position.Position;
import position.SourceRange;
import result.Result;

public record StringLiteralNode(String rawValue, Integer line, Integer column)
        implements LiteralNode<String> {

    @Override
    public Result<String> value() {
        return Result.success(rawValue);
    }

    @Override
    public SourceRange range() {
        return SourceRange.spanning(new Position(line, column), "\"" + rawValue + "\"");
    }

    @Override
    public List<ExpressionNode> children() {
        return Collections.emptyList();
    }

    @Override
    public String symbol() {
        return rawValue;
    }
}
