package interpreter;

import environment.Environment;
import evaluator.ExpressionEvaluator;
import node.Node;
import node.ProgramNode;
import node.expression.function.CallFunctionNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import result.Result;

import java.util.List;
import java.util.function.Consumer;

public class Interpreter {
    private final ExpressionEvaluator expressionEvaluator;
    private final Consumer<String> outputEmitter;

    public Interpreter(Consumer<String> outputEmitter) {
        this.expressionEvaluator = new ExpressionEvaluator();
        this.outputEmitter = outputEmitter;
    }

    public Interpreter() {
        this(System.out::println);
    }

    public Result<Void> interpret(ProgramNode program) {
        Environment globalEnv = new Environment();
        try {
            for (Node statement : program.statements()) {
                executeStatement(statement, globalEnv);
            }
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure("Runtime error: " + e.getMessage());
        }
    }

    private void executeStatement(Node statement, Environment env) {
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
            executeStatement(stmt, blockEnv);
        }
    }

    private void executeCall(CallFunctionNode call, Environment env) {
        if ("println".equalsIgnoreCase(call.identifierNode().name())) {
            if (!call.argumentNodes().isEmpty()) {
                Object argValue = expressionEvaluator.evaluate(call.argumentNodes().get(0), env);
                outputEmitter.accept(String.valueOf(argValue));
            }
        }
    }
}
