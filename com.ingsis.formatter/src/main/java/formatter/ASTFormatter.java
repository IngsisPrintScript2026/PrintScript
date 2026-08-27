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
import node.Node;
import node.ProgramNode;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.LiteralNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.operator.OperatorNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.visitor.NodeVisitor;
import result.Result;

public class ASTFormatter implements NodeVisitor<String, FormatContext>, Formatter {
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
        return Result.success(visit(program, context != null ? context : defaultContext));
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
        return node.accept(this, context);
    }

    public String formatExpression(ExpressionNode expr) {
        return formatExpression(expr, defaultContext);
    }

    public String formatExpression(ExpressionNode expr, FormatContext context) {
        if (expr instanceof OperatorNode op) {
            String opSymbol =
                    context.isSpaceAroundOperators() ? (" " + op.symbol() + " ") : op.symbol();
            return formatExpression(op.left(), context)
                    + opSymbol
                    + formatExpression(op.right(), context);
        } else if (expr instanceof IdentifierNode id) {
            return id.name();
        } else if (expr instanceof StringLiteralNode str) {
            return "\"" + str.rawValue() + "\"";
        } else if (expr instanceof NumberLiteralNode num) {
            if (num.rawValue() == null) return "0";
            return num.rawValue().stripTrailingZeros().toPlainString();
        } else if (expr instanceof BooleanLiteralNode bool) {
            return String.valueOf(bool.rawValue());
        } else if (expr instanceof CallFunctionNode call) {
            String args =
                    call.argumentNodes().stream()
                            .map(arg -> formatExpression(arg, context))
                            .collect(Collectors.joining(", "));
            return call.identifierNode().name() + "(" + args + ")";
        } else if (expr instanceof LiteralNode<?> lit) {
            return lit.symbol();
        }
        return expr != null ? expr.symbol() : "";
    }

    @Override
    public String visit(DeclarationKeywordNode decl, FormatContext context) {
        FormatNodeHandler<?> handler = handlers.get(DeclarationKeywordNode.class);
        return handler != null ? handler.formatUntyped(decl, context, this) : "";
    }

    @Override
    public String visit(AssignNode assign, FormatContext context) {
        FormatNodeHandler<?> handler = handlers.get(AssignNode.class);
        return handler != null ? handler.formatUntyped(assign, context, this) : "";
    }

    @Override
    public String visit(IfKeywordNode ifNode, FormatContext context) {
        FormatNodeHandler<?> handler = handlers.get(IfKeywordNode.class);
        return handler != null ? handler.formatUntyped(ifNode, context, this) : "";
    }

    @Override
    public String visit(CallFunctionNode call, FormatContext context) {
        FormatNodeHandler<?> handler = handlers.get(CallFunctionNode.class);
        return handler != null ? handler.formatUntyped(call, context, this) : "";
    }

    @Override
    public String visit(ProgramNode program, FormatContext context) {
        FormatNodeHandler<?> handler = handlers.get(ProgramNode.class);
        return handler != null ? handler.formatUntyped(program, context, this) : "";
    }

    @Override
    public String visitDefault(Node node, FormatContext context) {
        if (node instanceof ExpressionNode expr) {
            return context.getIndent() + formatExpression(expr, context) + ";";
        }
        return context.getIndent() + node.symbol();
    }
}
