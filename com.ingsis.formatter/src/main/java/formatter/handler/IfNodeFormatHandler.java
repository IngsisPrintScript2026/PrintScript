package formatter.handler;

import formatter.ASTFormatter;
import formatter.FormatContext;
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
        sb.append(context.getIndent());
        sb.append("if (");
        sb.append(formatter.formatExpression(ifNode.condition(), context));
        sb.append(")");

        if (context.ifBraceSameLine()) {
            sb.append(" {\n");
        } else {
            sb.append("\n");
            sb.append(context.getIndent());
            sb.append("{\n");
        }

        FormatContext innerContext = context.incrementIndent();
        for (Node stmt : ifNode.thenBody()) {
            sb.append(formatter.formatStatement(stmt, innerContext));
            sb.append("\n");
        }

        sb.append(context.getIndent());
        sb.append("}");

        if (ifNode.elseBody() != null && !ifNode.elseBody().isEmpty()) {
            if (context.ifBraceSameLine()) {
                sb.append(" else {\n");
            } else {
                sb.append("\n");
                sb.append(context.getIndent());
                sb.append("else\n");
                sb.append(context.getIndent());
                sb.append("{\n");
            }
            for (Node stmt : ifNode.elseBody()) {
                sb.append(formatter.formatStatement(stmt, innerContext));
                sb.append("\n");
            }
            sb.append(context.getIndent());
            sb.append("}");
        }

        return sb.toString();
    }
}
