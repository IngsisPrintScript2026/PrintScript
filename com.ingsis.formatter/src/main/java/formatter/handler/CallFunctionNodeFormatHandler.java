package formatter.handler;

import formatter.ASTFormatter;
import formatter.FormatContext;
import node.expression.function.CallFunctionNode;

import java.util.stream.Collectors;

public class CallFunctionNodeFormatHandler implements FormatNodeHandler<CallFunctionNode> {
    @Override
    public Class<CallFunctionNode> nodeType() {
        return CallFunctionNode.class;
    }

    @Override
    public String format(CallFunctionNode call, FormatContext context, ASTFormatter formatter) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getIndent());
        sb.append(call.identifierNode().name());
        sb.append("(");
        String args = call.argumentNodes().stream()
                .map(arg -> formatter.formatExpression(arg, context))
                .collect(Collectors.joining(", "));
        sb.append(args);
        sb.append(");");

        if ("println".equalsIgnoreCase(call.identifierNode().name()) && context.lineBreaksAfterPrintln() > 1) {
            sb.append("\n".repeat(context.lineBreaksAfterPrintln() - 1));
        }

        return sb.toString();
    }
}
