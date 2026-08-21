package executor;

import builtin.BuiltInFunction;
import builtin.DefaultFunctionRegistry;
import builtin.FunctionRegistry;
import environment.Environment;
import evaluator.DefaultExpressionEvaluator;
import evaluator.ExpressionEvaluator;
import node.Node;
import node.expression.ExpressionNode;
import node.expression.function.CallFunctionNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DefaultStatementExecutor implements StatementExecutor {
    private final ExpressionEvaluator expressionEvaluator;
    private final FunctionRegistry functionRegistry;
    private final Consumer<String> outputEmitter;

    public DefaultStatementExecutor(
            ExpressionEvaluator expressionEvaluator,
            FunctionRegistry functionRegistry,
            Consumer<String> outputEmitter) {
        this.expressionEvaluator = expressionEvaluator;
        this.functionRegistry = functionRegistry;
        this.outputEmitter = outputEmitter;
    }

    public DefaultStatementExecutor(Consumer<String> outputEmitter) {
        this(outputEmitter, new DefaultFunctionRegistry());
    }

    public DefaultStatementExecutor(Consumer<String> outputEmitter, FunctionRegistry functionRegistry) {
        this(new DefaultExpressionEvaluator(functionRegistry, outputEmitter), functionRegistry, outputEmitter);
    }

    @Override
    public Result<Void> execute(Node statement, Environment env) {
        return switch (statement) {
            case DeclarationKeywordNode decl -> executeDeclaration(decl, env);
            case AssignNode assign -> executeAssign(assign, env);
            case IfKeywordNode ifNode -> executeIf(ifNode, env);
            case CallFunctionNode call -> executeCall(call, env);
            default -> Result.failure("Unsupported statement node: " + statement);
        };
    }

    private Result<Void> executeDeclaration(DeclarationKeywordNode decl, Environment env) {
        Object value = null;
        if (decl.expressionNode() != null) {
            Result<Object> valRes = expressionEvaluator.evaluate(decl.expressionNode(), env);
            if (!valRes.isCorrect()) return Result.failure(((IncorrectResult<Object>) valRes).error());
            value = ((CorrectResult<Object>) valRes).value();
        }
        try {
            env.declare(decl.identifierNode().name(), value, null, decl.isMutable());
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure("Declaration error: " + e.getMessage());
        }
    }

    private Result<Void> executeAssign(AssignNode assign, Environment env) {
        Result<Object> valRes = expressionEvaluator.evaluate(assign.expressionNode(), env);
        if (!valRes.isCorrect()) return Result.failure(((IncorrectResult<Object>) valRes).error());
        try {
            env.assign(assign.identifierNode().name(), ((CorrectResult<Object>) valRes).value());
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure("Assignment error: " + e.getMessage());
        }
    }

    private Result<Void> executeIf(IfKeywordNode ifNode, Environment env) {
        Result<Object> condRes = expressionEvaluator.evaluate(ifNode.condition(), env);
        if (!condRes.isCorrect()) return Result.failure(((IncorrectResult<Object>) condRes).error());

        Object conditionValue = ((CorrectResult<Object>) condRes).value();
        if (!(conditionValue instanceof Boolean boolCond)) {
            return Result.failure("Condition of 'if' statement must evaluate to a boolean value");
        }

        Environment blockEnv = new Environment(env);
        List<Node> bodyToExecute = boolCond ? ifNode.thenBody() : ifNode.elseBody();
        for (Node stmt : bodyToExecute) {
            Result<Void> execRes = execute(stmt, blockEnv);
            if (!execRes.isCorrect()) return execRes;
        }
        return Result.success(null);
    }

    private Result<Void> executeCall(CallFunctionNode call, Environment env) {
        String functionName = call.identifierNode().name();
        BuiltInFunction function = functionRegistry.get(functionName);
        if (function == null) {
            return Result.failure("Undefined function: " + functionName);
        }

        List<Object> argValues = new ArrayList<>();
        for (ExpressionNode argNode : call.argumentNodes()) {
            Result<Object> argRes = expressionEvaluator.evaluate(argNode, env);
            if (!argRes.isCorrect()) return Result.failure(((IncorrectResult<Object>) argRes).error());
            argValues.add(((CorrectResult<Object>) argRes).value());
        }

        try {
            function.execute(argValues, outputEmitter);
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure("Function execution error: " + e.getMessage());
        }
    }
}
