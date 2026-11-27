/*
 * My Project
 */

package semantic;

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
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.evaluator.ExpressionTypeInference;
import semantic.handler.AssignNodeSemanticHandler;
import semantic.handler.CallFunctionNodeSemanticHandler;
import semantic.handler.DeclarationNodeSemanticHandler;
import semantic.handler.IfNodeSemanticHandler;
import semantic.handler.SemanticNodeHandler;
import semantic.handler.WhileNodeSemanticHandler;
import version.Version;

public class SemanticChecker {
    private final Map<Class<? extends Node>, SemanticNodeHandler<?>> handlers;

    public SemanticChecker(List<SemanticNodeHandler<?>> handlers) {
        this.handlers =
                handlers.stream().collect(Collectors.toMap(SemanticNodeHandler::nodeType, h -> h));
    }

    public SemanticChecker(Version version) {
        this(handlersFor(new ExpressionTypeInference(version)));
    }

    public SemanticChecker() {
        this(Version.V_1_1);
    }

    private static List<SemanticNodeHandler<?>> handlersFor(ExpressionTypeInference inference) {
        return List.of(
                new DeclarationNodeSemanticHandler(inference),
                new AssignNodeSemanticHandler(inference),
                new IfNodeSemanticHandler(inference),
                new WhileNodeSemanticHandler(inference),
                new CallFunctionNodeSemanticHandler(inference));
    }

    public Result<SemanticEnvironment> check(ProgramNode program) {
        return check(program, new SemanticEnvironment());
    }

    public Result<SemanticEnvironment> check(ProgramNode program, SemanticEnvironment initialEnv) {
        return checkProgram(program, initialEnv);
    }

    public Result<SemanticEnvironment> checkNode(Node node, SemanticEnvironment env) {
        return switch (node) {
            case ProgramNode program -> checkProgram(program, env);
            case StatementNode statement -> checkStatement(statement, env);
        };
    }

    public Result<SemanticEnvironment> checkStatement(
            StatementNode statement, SemanticEnvironment env) {
        return switch (statement) {
            case DeclarationKeywordNode decl -> runHandler(DeclarationKeywordNode.class, decl, env);
            case AssignNode assign -> runHandler(AssignNode.class, assign, env);
            case IfKeywordNode ifNode -> checkIf(ifNode, env);
            case WhileKeywordNode whileNode -> checkWhile(whileNode, env);
            case CallFunctionNode call -> runHandler(CallFunctionNode.class, call, env);
                // Una expresión suelta como sentencia no introduce ni modifica símbolos.
            case ExpressionNode expression -> Result.success(env);
        };
    }

    private <T extends Node> Result<SemanticEnvironment> runHandler(
            Class<T> type, T node, SemanticEnvironment env) {
        SemanticNodeHandler<?> handler = handlers.get(type);
        return handler != null ? handler.checkUntyped(node, env) : Result.success(env);
    }

    private Result<SemanticEnvironment> checkProgram(
            ProgramNode program, SemanticEnvironment initialEnv) {
        SemanticEnvironment currentEnv = initialEnv;
        for (StatementNode statement : program.statements()) {
            Result<SemanticEnvironment> stepRes = checkNode(statement, currentEnv);
            if (!(stepRes instanceof CorrectResult<SemanticEnvironment>(var updated))) {
                return stepRes;
            }
            currentEnv = updated;
        }
        return Result.success(currentEnv);
    }

    private Result<SemanticEnvironment> checkIf(IfKeywordNode ifNode, SemanticEnvironment env) {
        Result<SemanticEnvironment> condRes = runHandler(IfKeywordNode.class, ifNode, env);
        if (!condRes.isCorrect()) {
            return condRes;
        }
        Result<Void> thenRes = checkBlock(ifNode.thenBody(), env);
        if (thenRes instanceof IncorrectResult<Void>(String err)) {
            return Result.failure(err);
        }
        Result<Void> elseRes = checkBlock(ifNode.elseBody(), env);
        if (elseRes instanceof IncorrectResult<Void>(String err)) {
            return Result.failure(err);
        }
        return Result.success(env);
    }

    private Result<SemanticEnvironment> checkWhile(
            WhileKeywordNode whileNode, SemanticEnvironment env) {
        Result<SemanticEnvironment> condRes = runHandler(WhileKeywordNode.class, whileNode, env);
        if (!condRes.isCorrect()) {
            return condRes;
        }
        Result<Void> bodyRes = checkBlock(whileNode.body(), env);
        if (bodyRes instanceof IncorrectResult<Void>(String err)) {
            return Result.failure(err);
        }
        return Result.success(env);
    }

    private Result<Void> checkBlock(List<StatementNode> stmts, SemanticEnvironment parentEnv) {
        SemanticEnvironment blockEnv = new SemanticEnvironment(parentEnv);
        for (StatementNode stmt : stmts) {
            Result<SemanticEnvironment> res = checkNode(stmt, blockEnv);
            switch (res) {
                case CorrectResult<SemanticEnvironment>(var updated) -> blockEnv = updated;
                case IncorrectResult<SemanticEnvironment>(String err) -> {
                    return Result.failure(err);
                }
            }
        }
        return Result.success(null);
    }
}
