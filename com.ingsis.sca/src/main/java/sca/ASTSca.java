/*
 * My Project
 */

package sca;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import node.Node;
import node.ProgramNode;
import node.expression.function.CallFunctionNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.visitor.NodeVisitor;
import result.Result;
import sca.config.YamlScaRulesLoader;
import sca.handler.*;
import semantic.environment.SemanticEnvironment;

public class ASTSca implements NodeVisitor<List<String>, ScaContext>, Sca {
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
        List<String> violations = visit(program, context != null ? context : defaultContext);
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
        return node.accept(this, context);
    }

    @Override
    public List<String> visit(DeclarationKeywordNode decl, ScaContext context) {
        ScaNodeHandler<?> handler = handlers.get(DeclarationKeywordNode.class);
        return handler != null
                ? handler.checkUntyped(decl, currentEnv, context, this)
                : Collections.emptyList();
    }

    @Override
    public List<String> visit(AssignNode assign, ScaContext context) {
        ScaNodeHandler<?> handler = handlers.get(AssignNode.class);
        return handler != null
                ? handler.checkUntyped(assign, currentEnv, context, this)
                : Collections.emptyList();
    }

    @Override
    public List<String> visit(IfKeywordNode ifNode, ScaContext context) {
        ScaNodeHandler<?> handler = handlers.get(IfKeywordNode.class);
        return handler != null
                ? handler.checkUntyped(ifNode, currentEnv, context, this)
                : Collections.emptyList();
    }

    @Override
    public List<String> visit(CallFunctionNode call, ScaContext context) {
        ScaNodeHandler<?> handler = handlers.get(CallFunctionNode.class);
        return handler != null
                ? handler.checkUntyped(call, currentEnv, context, this)
                : Collections.emptyList();
    }

    @Override
    public List<String> visit(ProgramNode program, ScaContext context) {
        ScaNodeHandler<?> handler = handlers.get(ProgramNode.class);
        return handler != null
                ? handler.checkUntyped(program, currentEnv, context, this)
                : Collections.emptyList();
    }

    @Override
    public List<String> visitDefault(Node node, ScaContext context) {
        return Collections.emptyList();
    }
}
