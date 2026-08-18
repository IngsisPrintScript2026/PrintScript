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
        this(new DefaultExpressionEvaluator(), new DefaultFunctionRegistry(), outputEmitter);
    }

    @Override
    public void execute(Node statement, Environment env) {
        switch (statement) {
            case DeclarationKeywordNode decl -> executeDeclaration(decl, env);
            case AssignNode assign -> executeAssign(assign, env);
            case IfKeywordNode ifNode -> executeIf(ifNode, env);
            case CallFunctionNode call -> executeCall(call, env);
            default -> throw new IllegalArgumentException("Unsupported statement node: " + statement);
        }
    }

    private void executeDeclaration(DeclarationKeywordNode decl, Environment env) {
        Object value = decl.expressionNode() != null
                ? expressionEvaluator.evaluate(decl.expressionNode(), env)
                : null;
        env.declare(decl.identifierNode().name(), value, null, decl.isMutable());
    }

    private void executeAssign(AssignNode assign, Environment env) {
        Object value = expressionEvaluator.evaluate(assign.expressionNode(), env);
        env.assign(assign.identifierNode().name(), value);
    }

    private void executeIf(IfKeywordNode ifNode, Environment env) {
        Object conditionValue = expressionEvaluator.evaluate(ifNode.condition(), env);
        if (!(conditionValue instanceof Boolean boolCond)) {
            throw new RuntimeException("Condition of 'if' statement must evaluate to a boolean value");
        }

        Environment blockEnv = new Environment(env);
        List<Node> bodyToExecute = boolCond ? ifNode.thenBody() : ifNode.elseBody();
        for (Node stmt : bodyToExecute) {
            execute(stmt, blockEnv);
        }
    }

    private void executeCall(CallFunctionNode call, Environment env) {
        String functionName = call.identifierNode().name();
        BuiltInFunction function = functionRegistry.get(functionName);
        if (function == null) {
            throw new RuntimeException("Undefined function: " + functionName);
        }

        List<Object> argValues = new ArrayList<>();
        for (ExpressionNode argNode : call.argumentNodes()) {
            argValues.add(expressionEvaluator.evaluate(argNode, env));
        }

        function.execute(argValues, outputEmitter);
    }
}
