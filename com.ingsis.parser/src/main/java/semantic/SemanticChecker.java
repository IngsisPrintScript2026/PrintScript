package semantic;

import node.Node;
import node.ProgramNode;
import node.expression.function.CallFunctionNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
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

public class SemanticChecker {
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
        SemanticEnvironment currentEnv = new SemanticEnvironment();
        for (Node statement : program.statements()) {
            Result<SemanticEnvironment> stepRes = checkNode(statement, currentEnv);
            if (!stepRes.isCorrect()) {
                return stepRes;
            }
            currentEnv = ((CorrectResult<SemanticEnvironment>) stepRes).value();
        }
        return Result.success(currentEnv);
    }

    @SuppressWarnings("unchecked")
    public Result<SemanticEnvironment> checkNode(Node node, SemanticEnvironment env) {
        return switch (node) {
            case DeclarationKeywordNode decl ->
                    ((SemanticNodeHandler<DeclarationKeywordNode>) handlers.get(DeclarationKeywordNode.class)).check(decl, env);
            case AssignNode assign ->
                    ((SemanticNodeHandler<AssignNode>) handlers.get(AssignNode.class)).check(assign, env);
            case IfKeywordNode ifNode -> checkIfNode(ifNode, env);
            case CallFunctionNode call ->
                    ((SemanticNodeHandler<CallFunctionNode>) handlers.get(CallFunctionNode.class)).check(call, env);
            default -> Result.success(env);
        };
    }

    private Result<SemanticEnvironment> checkIfNode(IfKeywordNode ifNode, SemanticEnvironment env) {
        SemanticNodeHandler<IfKeywordNode> handler =
                (SemanticNodeHandler<IfKeywordNode>) handlers.get(IfKeywordNode.class);

        Result<SemanticEnvironment> condRes = handler.check(ifNode, env);
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
}
