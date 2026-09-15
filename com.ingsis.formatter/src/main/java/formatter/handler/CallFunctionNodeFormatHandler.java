/*
 * My Project
 */

package formatter.handler;

import formatter.ASTFormatter;
import formatter.FormatContext;
import java.util.stream.Collectors;
import node.expression.function.CallFunctionNode;

public class CallFunctionNodeFormatHandler implements FormatNodeHandler<CallFunctionNode> {
    @Override
    public Class<CallFunctionNode> nodeType() {
        return CallFunctionNode.class;
    }

    @Override
    public String format(CallFunctionNode call, FormatContext context, ASTFormatter formatter) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getIndent()).append(call.identifierNode().name()).append("(");
        String args =
                call.argumentNodes().stream()
                        .map(arg -> formatter.formatExpression(arg, context))
                        .collect(Collectors.joining(", "));
        sb.append(args).append(");");
        appendTrailingPrintlnBreaks(sb, call.identifierNode().name(), context);
        return sb.toString();
    }

    private void appendTrailingPrintlnBreaks(
            StringBuilder sb, String funcName, FormatContext context) {
        if ("println".equalsIgnoreCase(funcName) && context.getLineBreaksAfterPrintln() > 1) {
            sb.append("\n".repeat(context.getLineBreaksAfterPrintln() - 1));
        }
    }
}
