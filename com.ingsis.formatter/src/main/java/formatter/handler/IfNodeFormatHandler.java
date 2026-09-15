/*
 * My Project
 */

package formatter.handler;

import formatter.ASTFormatter;
import formatter.FormatContext;
import java.util.List;
import node.Node;
import node.keyword.IfKeywordNode;

public class IfNodeFormatHandler implements FormatNodeHandler<IfKeywordNode> {
    @Override
    public Class<IfKeywordNode> nodeType() {
        return IfKeywordNode.class;
    }

    @Override
    public String format(IfKeywordNode ifNode, FormatContext context, ASTFormatter formatter) {
        StringBuilder sb = new StringBuilder();
        appendIfHeader(sb, ifNode, context, formatter);
        FormatContext innerContext = context.incrementIndent();
        appendBlock(sb, ifNode.thenBody(), context, innerContext, formatter);
        if (ifNode.elseBody() != null && !ifNode.elseBody().isEmpty()) {
            appendElseClause(sb, ifNode, context, innerContext, formatter);
        }
        return sb.toString();
    }

    private void appendIfHeader(
            StringBuilder sb, IfKeywordNode ifNode, FormatContext context, ASTFormatter formatter) {
        sb.append(context.getIndent())
                .append("if (")
                .append(formatter.formatExpression(ifNode.condition(), context))
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
            StringBuilder sb,
            List<Node> body,
            FormatContext context,
            FormatContext innerContext,
            ASTFormatter formatter) {
        for (Node stmt : body) {
            sb.append(formatter.formatStatement(stmt, innerContext)).append("\n");
        }
        sb.append(context.getIndent()).append("}");
    }

    private void appendElseClause(
            StringBuilder sb,
            IfKeywordNode ifNode,
            FormatContext context,
            FormatContext innerContext,
            ASTFormatter formatter) {
        if (context.isIfBraceSameLine()) {
            sb.append(" else {\n");
        } else {
            sb.append("\n").append(context.getIndent()).append("else\n");
            sb.append(context.getIndent()).append("{\n");
        }
        appendBlock(sb, ifNode.elseBody(), context, innerContext, formatter);
    }
}
