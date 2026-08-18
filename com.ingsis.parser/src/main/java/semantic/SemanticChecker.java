package semantic;

import iterator.IterationStep;
import node.Node;
import node.ProgramNode;
import node.expression.function.CallFunctionNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.handler.AssignNodeSemanticHandler;
import semantic.handler.CallFunctionNodeSemanticHandler;
import semantic.handler.DeclarationNodeSemanticHandler;
import semantic.handler.IfNodeSemanticHandler;
import semantic.handler.SemanticNodeHandler;
import syntactic.Parser;
import tokenstream.TokenStream;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SemanticChecker {
    private final Parser<Node> syntacticParser;
    private final Map<Class<? extends Node>, SemanticNodeHandler<?>> handlers;

    public SemanticChecker(Parser<Node> syntacticParser, List<SemanticNodeHandler<?>> handlers) {
        this.syntacticParser = syntacticParser;
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(SemanticNodeHandler::nodeType, h -> h));
    }

    public SemanticChecker(Parser<Node> syntacticParser) {
        this(syntacticParser, List.of(
                new DeclarationNodeSemanticHandler(),
                new AssignNodeSemanticHandler(),
                new IfNodeSemanticHandler(),
                new CallFunctionNodeSemanticHandler()
        ));
    }

    public SemanticChecker(List<SemanticNodeHandler<?>> handlers) {
        this(null, handlers);
    }

    public SemanticChecker() {
        this((Parser<Node>) null);
    }

    public Result<SemanticStep> parseAndCheckStatement(TokenStream stream, SemanticEnvironment env) {
        if (syntacticParser == null) {
            return Result.failure("SyntacticParser dependency must be injected into SemanticChecker to parse and check streams.");
        }
        Result<IterationStep<Node>> parseResult = syntacticParser.parse(stream);
        if (!parseResult.isCorrect()) {
            String err = ((IncorrectResult<IterationStep<Node>>) parseResult).error();
            return Result.failure(err.startsWith("Syntactic error:") ? err : "Syntactic error: " + err);
        }

        IterationStep<Node> step = ((CorrectResult<IterationStep<Node>>) parseResult).value();
        Node statement = step.value();
        TokenStream nextStream = (TokenStream) step.next();

        Result<SemanticEnvironment> semResult = checkNode(statement, env);
        if (!semResult.isCorrect()) {
            String err = ((IncorrectResult<SemanticEnvironment>) semResult).error();
            return Result.failure(err.startsWith("Semantic error:") ? err : "Semantic error: " + err);
        }

        SemanticEnvironment updatedEnv = ((CorrectResult<SemanticEnvironment>) semResult).value();
        return Result.success(new SemanticStep(statement, updatedEnv, nextStream));
    }

    public Result<SemanticEnvironment> check(ProgramNode program) {
        return check(program, new SemanticEnvironment());
    }

    public Result<SemanticEnvironment> check(ProgramNode program, SemanticEnvironment initialEnv) {
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
