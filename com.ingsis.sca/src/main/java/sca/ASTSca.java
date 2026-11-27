/*
 * My Project
 */

package sca;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import node.AssignNode;
import node.CallFunctionNode;
import node.DeclarationKeywordNode;
import node.ExpressionNode;
import node.IfKeywordNode;
import node.Node;
import node.ProgramNode;
import node.StatementNode;
import node.WhileKeywordNode;
import result.Result;
import sca.config.YamlScaRulesLoader;
import sca.handler.AssignScaHandler;
import sca.handler.CallFunctionScaHandler;
import sca.handler.DeclarationScaHandler;
import sca.handler.IfScaHandler;
import sca.handler.ProgramScaHandler;
import sca.handler.ScaNodeHandler;
import sca.handler.WhileScaHandler;
import semantic.environment.SemanticEnvironment;

public class ASTSca implements Sca {
    private final Map<Class<? extends Node>, ScaNodeHandler<?>> handlers;
    private final ScaContext defaultContext;
    private SemanticEnvironment currentEnv;

    public ASTSca(List<ScaNodeHandler<?>> handlerList, ScaContext defaultContext) {
        this.handlers =
                handlerList.stream().collect(Collectors.toMap(ScaNodeHandler::nodeType, h -> h));
        this.defaultContext = defaultContext != null ? defaultContext : new ScaContext();
    }

    public ASTSca(ScaContext defaultContext) {
        this(
                List.of(
                        new DeclarationScaHandler(),
                        new CallFunctionScaHandler(),
                        new IfScaHandler(),
                        new WhileScaHandler(),
                        new AssignScaHandler(),
                        new ProgramScaHandler()),
                defaultContext);
    }

    public ASTSca() {
        this(new ScaContext());
    }

    public static ASTSca fromYamlConfig(InputStream yamlConfigStream) {
        ScaContext context = YamlScaRulesLoader.loadFromYaml(yamlConfigStream);
        return new ASTSca(context);
    }

    @Override
    public Result<List<String>> analyze(ProgramNode program, SemanticEnvironment env) {
        return analyze(program, env, defaultContext);
    }

    public Result<List<String>> analyze(
            ProgramNode program, SemanticEnvironment env, ScaContext context) {
        if (program == null) {
            return Result.failure("ProgramNode cannot be null");
        }
        this.currentEnv = env != null ? env : new SemanticEnvironment();
        List<String> violations = dispatch(program, context != null ? context : defaultContext);
        if (violations.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        return Result.success(violations);
    }

    @Override
    public Result<List<String>> analyzeNode(Node node, SemanticEnvironment env) {
        if (node == null) {
            return Result.failure("Node cannot be null");
        }
        this.currentEnv = env != null ? env : new SemanticEnvironment();
        List<String> violations = analyzeStatement(node, this.currentEnv, defaultContext);
        return Result.success(violations);
    }

    public List<String> analyzeStatement(Node node, SemanticEnvironment env, ScaContext context) {
        this.currentEnv = env;
        return dispatch(node, context);
    }

    private List<String> dispatch(Node node, ScaContext context) {
        return switch (node) {
            case ProgramNode program -> runHandler(ProgramNode.class, program, context);
            case StatementNode statement -> dispatchStatement(statement, context);
        };
    }

    private List<String> dispatchStatement(StatementNode statement, ScaContext context) {
        return switch (statement) {
            case DeclarationKeywordNode decl ->
                    runHandler(DeclarationKeywordNode.class, decl, context);
            case AssignNode assign -> runHandler(AssignNode.class, assign, context);
            case IfKeywordNode ifNode -> runHandler(IfKeywordNode.class, ifNode, context);
            case WhileKeywordNode whileNode ->
                    runHandler(WhileKeywordNode.class, whileNode, context);
            case CallFunctionNode call -> runHandler(CallFunctionNode.class, call, context);
                // Ninguna regla del SCA aplica a una expresión suelta como sentencia.
            case ExpressionNode expression -> Collections.emptyList();
        };
    }

    private <T extends Node> List<String> runHandler(Class<T> type, T node, ScaContext context) {
        ScaNodeHandler<?> handler = handlers.get(type);
        return handler != null
                ? handler.checkUntyped(node, currentEnv, context, this)
                : Collections.emptyList();
    }
}
