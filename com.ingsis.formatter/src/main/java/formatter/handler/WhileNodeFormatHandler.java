/*
 * My Project
 */

package formatter.handler;

import formatter.ASTFormatter;
import formatter.FormatContext;
import java.util.List;
import node.Node;
import node.keyword.WhileKeywordNode;

public class WhileNodeFormatHandler implements FormatNodeHandler<WhileKeywordNode> {
    @Override
    public Class<WhileKeywordNode> nodeType() {
        return WhileKeywordNode.class;
    }

    @Override
    public String format(
            WhileKeywordNode whileNode, FormatContext context, ASTFormatter formatter) {
        StringBuilder sb = new StringBuilder();
        appendWhileHeader(sb, whileNode, context, formatter);
        appendBlock(sb, whileNode.body(), context, formatter);
        return sb.toString();
    }

    private void appendWhileHeader(
            StringBuilder sb,
            WhileKeywordNode whileNode,
            FormatContext context,
            ASTFormatter formatter) {
        sb.append(context.getIndent())
                .append("while (")
                .append(formatter.formatExpression(whileNode.condition(), context))
                .append(")");
        appendOpenBrace(sb, context);
    }

    private void appendOpenBrace(StringBuilder sb, FormatContext context) {
        if (context.isIfBraceSameLine()) {
            sb.append(" {\n");
        } else {
            sb.append("\n").append(context.getIndent()).append("{\n");
        }
    }

    private void appendBlock(
            StringBuilder sb, List<Node> body, FormatContext context, ASTFormatter formatter) {
        FormatContext innerContext = context.incrementIndent();
        for (Node stmt : body) {
            sb.append(formatter.formatStatement(stmt, innerContext)).append("\n");
        }
        sb.append(context.getIndent()).append("}");
    }
}
