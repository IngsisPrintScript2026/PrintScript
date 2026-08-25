package formatter.handler;

import formatter.ASTFormatter;
import formatter.FormatContext;
import node.keyword.AssignNode;

public class AssignNodeFormatHandler implements FormatNodeHandler<AssignNode> {
    @Override
    public Class<AssignNode> nodeType() {
        return AssignNode.class;
    }

    @Override
    public String format(AssignNode assign, FormatContext context, ASTFormatter formatter) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getIndent());
        sb.append(assign.identifierNode().name());
        if (context.spaceAroundEquals()) {
            sb.append(" = ");
        } else {
            sb.append("=");
        }
        sb.append(formatter.formatExpression(assign.expressionNode(), context));
        sb.append(";");
        return sb.toString();
    }
}
