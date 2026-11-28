/*
 * My Project
 */

package formatter;

import formatter.config.YamlFormatRulesLoader;
import formatter.handler.*;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import node.AssignNode;
import node.BooleanLiteralNode;
import node.CallFunctionNode;
import node.DeclarationKeywordNode;
import node.ExpressionNode;
import node.IdentifierNode;
import node.IfKeywordNode;
import node.NilExpressionNode;
import node.Node;
import node.NumberLiteralNode;
import node.OperatorNode;
import node.ProgramNode;
import node.StatementNode;
import node.StringLiteralNode;
import node.WhileKeywordNode;
import result.Result;

public class ASTFormatter implements Formatter {
    private final Map<Class<? extends Node>, FormatNodeHandler<?>> handlers;
    private final FormatContext defaultContext;

    public ASTFormatter(List<FormatNodeHandler<?>> handlerList, FormatContext defaultContext) {
        this.handlers =
                handlerList.stream().collect(Collectors.toMap(FormatNodeHandler::nodeType, h -> h));
        this.defaultContext = defaultContext != null ? defaultContext : new FormatContext();
    }

    public ASTFormatter(FormatContext defaultContext) {
        this(
                List.of(
                        new DeclarationNodeFormatHandler(),
                        new AssignNodeFormatHandler(),
                        new IfNodeFormatHandler(),
                        new WhileNodeFormatHandler(),
                        new CallFunctionNodeFormatHandler(),
                        new ProgramNodeFormatHandler()),
                defaultContext);
    }

    public ASTFormatter() {
        this(new FormatContext());
    }

    public static ASTFormatter fromYamlConfig(InputStream yamlConfigStream) {
        FormatContext context = YamlFormatRulesLoader.loadFromYaml(yamlConfigStream);
        return new ASTFormatter(context);
    }

    @Override
    public Result<String> format(ProgramNode program) {
        return format(program, defaultContext);
    }

    public Result<String> format(ProgramNode program, FormatContext context) {
        if (program == null) {
            return Result.failure("ProgramNode cannot be null");
        }
        return Result.success(formatStatement(program, context != null ? context : defaultContext));
    }

    @Override
    public Result<String> formatNode(Node node) {
        return formatNode(node, defaultContext);
    }

    public Result<String> formatNode(Node node, FormatContext context) {
        if (node == null) {
            return Result.failure("Node cannot be null");
        }
        return Result.success(formatStatement(node, context != null ? context : defaultContext));
    }

    public String formatStatement(Node node, FormatContext context) {
        return switch (node) {
            case ProgramNode program -> runHandler(ProgramNode.class, program, context);
            case StatementNode statement -> formatStatement(statement, context);
        };
    }

    public String formatStatement(StatementNode statement, FormatContext context) {
        return switch (statement) {
            case DeclarationKeywordNode decl ->
                    runHandler(DeclarationKeywordNode.class, decl, context);
            case AssignNode assign -> runHandler(AssignNode.class, assign, context);
            case IfKeywordNode ifNode -> runHandler(IfKeywordNode.class, ifNode, context);
            case WhileKeywordNode whileNode ->
                    runHandler(WhileKeywordNode.class, whileNode, context);
            case CallFunctionNode call -> runHandler(CallFunctionNode.class, call, context);
            case ExpressionNode expr -> context.getIndent() + formatExpression(expr, context) + ";";
        };
    }

    private <T extends Node> String runHandler(Class<T> type, T node, FormatContext context) {
        FormatNodeHandler<?> handler = handlers.get(type);
        return handler != null ? handler.formatUntyped(node, context, this) : "";
    }

    public String formatExpression(ExpressionNode expr) {
        return formatExpression(expr, defaultContext);
    }

    public String formatExpression(ExpressionNode expr, FormatContext context) {
        return switch (expr) {
            case null -> "";
            case OperatorNode op -> formatOperator(op, context);
            case CallFunctionNode call -> formatCall(call, context);
            case IdentifierNode id -> id.name();
            case StringLiteralNode str -> "\"" + str.rawValue() + "\"";
            case NumberLiteralNode num ->
                    num.rawValue() == null
                            ? "0"
                            : num.rawValue().stripTrailingZeros().toPlainString();
            case BooleanLiteralNode bool -> String.valueOf(bool.rawValue());
            case NilExpressionNode nil -> nil.symbol();
        };
    }

    private String formatOperator(OperatorNode op, FormatContext context) {
        String symbol = context.isSpaceAroundOperators() ? (" " + op.symbol() + " ") : op.symbol();
        return formatExpression(op.left(), context)
                + symbol
                + formatExpression(op.right(), context);
    }

    private String formatCall(CallFunctionNode call, FormatContext context) {
        String delimiter = context.isSpaceAfterComma() ? ", " : ",";
        String args =
                call.argumentNodes().stream()
                        .map(arg -> formatExpression(arg, context))
                        .collect(Collectors.joining(delimiter));
        return call.identifierNode().name() + "(" + args + ")";
    }
}
