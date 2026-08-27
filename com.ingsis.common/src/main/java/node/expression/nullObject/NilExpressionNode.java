/*
 * My Project
 */

package node.expression.nullObject;

import java.util.Collections;
import java.util.List;
import node.expression.ExpressionNode;

public record NilExpressionNode() implements ExpressionNode {

    @Override
    public List<ExpressionNode> children() {
        return Collections.emptyList();
    }

    @Override
    public String symbol() {
        return "NIL";
    }

    @Override
    public Integer line() {
        return -1;
    }

    @Override
    public Integer column() {
        return -1;
    }
}
