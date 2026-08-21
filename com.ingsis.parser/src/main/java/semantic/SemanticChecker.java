package semantic;

import node.Node;
import node.ProgramNode;
import node.expression.function.CallFunctionNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.visitor.NodeVisitor;
import result.CorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.handler.AssignNodeSemanticHandler;
import semantic.handler.CallFunctionNodeSemanticHandler;
import semantic.handler.DeclarationNodeSemanticHandler;
import semantic.handler.IfNodeSemanticHandler;
import semantic.handler.SemanticNodeHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SemanticChecker implements NodeVisitor<Result<SemanticEnvironment>, SemanticEnvironment> {
    private final Map<Class<? extends Node>, SemanticNodeHandler<?>> handlers;

    public SemanticChecker(List<SemanticNodeHandler<?>> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(SemanticNodeHandler::nodeType, h -> h));
    }

    public SemanticChecker() {
        this(List.of(
                new DeclarationNodeSemanticHandler(),
                new AssignNodeSemanticHandler(),
                new IfNodeSemanticHandler(),
                new CallFunctionNodeSemanticHandler()
        ));
    }

    public Result<SemanticEnvironment> check(ProgramNode program) {
        return check(program, new SemanticEnvironment());
    }

    public Result<SemanticEnvironment> check(ProgramNode program, SemanticEnvironment initialEnv) {
        return visit(program, initialEnv);
    }

    public Result<SemanticEnvironment> checkNode(Node node, SemanticEnvironment env) {
        return node.accept(this, env);
    }

    @Override
    public Result<SemanticEnvironment> visit(DeclarationKeywordNode decl, SemanticEnvironment env) {
        SemanticNodeHandler<?> handler = handlers.get(DeclarationKeywordNode.class);
        return handler != null ? handler.checkUntyped(decl, env) : Result.success(env);
    }

    @Override
    public Result<SemanticEnvironment> visit(AssignNode assign, SemanticEnvironment env) {
        SemanticNodeHandler<?> handler = handlers.get(AssignNode.class);
        return handler != null ? handler.checkUntyped(assign, env) : Result.success(env);
    }

    @Override
    public Result<SemanticEnvironment> visit(IfKeywordNode ifNode, SemanticEnvironment env) {
        SemanticNodeHandler<?> handler = handlers.get(IfKeywordNode.class);

        Result<SemanticEnvironment> condRes = handler != null ? handler.checkUntyped(ifNode, env) : Result.success(env);
        if (!condRes.isCorrect()) {
            return condRes;
        }

        SemanticEnvironment thenEnv = new SemanticEnvironment(env);
        for (Node stmt : ifNode.thenBody()) {
            Result<SemanticEnvironment> res = checkNode(stmt, thenEnv);
            if (!res.isCorrect()) return res;
            thenEnv = ((CorrectResult<SemanticEnvironment>) res).value();
        }

        SemanticEnvironment elseEnv = new SemanticEnvironment(env);
        for (Node stmt : ifNode.elseBody()) {
            Result<SemanticEnvironment> res = checkNode(stmt, elseEnv);
            if (!res.isCorrect()) return res;
            elseEnv = ((CorrectResult<SemanticEnvironment>) res).value();
        }

        return Result.success(env);
    }

    @Override
    public Result<SemanticEnvironment> visit(CallFunctionNode call, SemanticEnvironment env) {
        SemanticNodeHandler<?> handler = handlers.get(CallFunctionNode.class);
        return handler != null ? handler.checkUntyped(call, env) : Result.success(env);
    }

    @Override
    public Result<SemanticEnvironment> visit(ProgramNode program, SemanticEnvironment initialEnv) {
        SemanticEnvironment currentEnv = initialEnv;
        for (Node statement : program.statements()) {
            Result<SemanticEnvironment> stepRes = checkNode(statement, currentEnv);
            if (!stepRes.isCorrect()) {
                return stepRes;
            }
            currentEnv = ((CorrectResult<SemanticEnvironment>) stepRes).value();
        }
        return Result.success(currentEnv);
    }

    @Override
    public Result<SemanticEnvironment> visitDefault(Node node, SemanticEnvironment env) {
        return Result.success(env);
    }
}
