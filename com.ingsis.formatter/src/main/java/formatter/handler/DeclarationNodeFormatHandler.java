/*
 * My Project
 */

package formatter.handler;

import formatter.ASTFormatter;
import formatter.FormatContext;
import java.util.Locale;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.nullObject.NilExpressionNode;
import node.keyword.DeclarationKeywordNode;

public class DeclarationNodeFormatHandler implements FormatNodeHandler<DeclarationKeywordNode> {
    @Override
    public Class<DeclarationKeywordNode> nodeType() {
        return DeclarationKeywordNode.class;
    }

    @Override
    public String format(
            DeclarationKeywordNode decl, FormatContext context, ASTFormatter formatter) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getIndent())
                .append(decl.declarationType().keyword())
                .append(" ")
                .append(decl.identifierNode().name());
        appendColonAndType(sb, decl, context);
        appendInitialization(sb, decl, context, formatter);
        sb.append(";");
        return sb.toString();
    }

    private void appendColonAndType(
            StringBuilder sb, DeclarationKeywordNode decl, FormatContext context) {
        if (context.isSpaceBeforeColon()) {
            sb.append(" ");
        }
        sb.append(":");
        if (context.isSpaceAfterColon()) {
            sb.append(" ");
        }
        sb.append(resolveType(decl));
    }

    private String resolveType(DeclarationKeywordNode decl) {
        if (decl.declaredType() != null) {
            return decl.declaredType().toString().toLowerCase(Locale.ROOT);
        }
        if (decl.expressionNode() instanceof StringLiteralNode) {
            return "string";
        }
        if (decl.expressionNode() instanceof BooleanLiteralNode) {
            return "boolean";
        }
        return "number";
    }

    private void appendInitialization(
            StringBuilder sb,
            DeclarationKeywordNode decl,
            FormatContext context,
            ASTFormatter formatter) {
        if (decl.expressionNode() != null
                && !(decl.expressionNode() instanceof NilExpressionNode)) {
            sb.append(context.isSpaceAroundEquals() ? " = " : "=");
            sb.append(formatter.formatExpression(decl.expressionNode(), context));
        }
    }
}
