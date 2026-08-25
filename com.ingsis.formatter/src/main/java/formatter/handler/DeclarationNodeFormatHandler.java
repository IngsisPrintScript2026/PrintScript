package formatter.handler;

import formatter.ASTFormatter;
import formatter.FormatContext;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.nullObject.NilExpressionNode;
import node.keyword.DeclarationKeywordNode;

public class DeclarationNodeFormatHandler implements FormatNodeHandler<DeclarationKeywordNode> {
    @Override
    public Class<DeclarationKeywordNode> nodeType() {
        return DeclarationKeywordNode.class;
    }

    @Override
    public String format(DeclarationKeywordNode decl, FormatContext context, ASTFormatter formatter) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getIndent());
        sb.append(decl.declarationType().keyword());
        sb.append(" ");
        sb.append(decl.identifierNode().name());

        if (context.spaceBeforeColon()) {
            sb.append(" ");
        }
        sb.append(":");
        if (context.spaceAfterColon()) {
            sb.append(" ");
        }

        String typeStr = "number";
        if (decl.declaredType() != null) {
            typeStr = decl.declaredType().toString().toLowerCase();
        } else if (decl.expressionNode() instanceof StringLiteralNode) {
            typeStr = "string";
        } else if (decl.expressionNode() instanceof BooleanLiteralNode) {
            typeStr = "boolean";
        } else if (decl.expressionNode() instanceof NumberLiteralNode) {
            typeStr = "number";
        }
        sb.append(typeStr);

        if (decl.expressionNode() != null && !(decl.expressionNode() instanceof NilExpressionNode)) {
            if (context.spaceAroundEquals()) {
                sb.append(" = ");
            } else {
                sb.append("=");
            }
            sb.append(formatter.formatExpression(decl.expressionNode(), context));
        }

        sb.append(";");
        return sb.toString();
    }
}
