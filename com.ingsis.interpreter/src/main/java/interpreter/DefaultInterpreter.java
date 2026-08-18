package interpreter;

import environment.Environment;
import evaluator.DefaultExpressionEvaluator;
import evaluator.ExpressionEvaluator;
import executor.DefaultStatementExecutor;
import executor.StatementExecutor;
import node.Node;
import node.ProgramNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.SemanticStep;
import semantic.environment.SemanticEnvironment;
import tokenstream.TokenStream;

import java.util.List;
import java.util.function.Consumer;

public class DefaultInterpreter implements Interpreter {
    private final SemanticChecker semanticChecker;
    private final StatementExecutor statementExecutor;

    public DefaultInterpreter(
            SemanticChecker semanticChecker,
            StatementExecutor statementExecutor) {
        this.semanticChecker = semanticChecker;
        this.statementExecutor = statementExecutor;
    }

    public DefaultInterpreter(
            SemanticChecker semanticChecker,
            ExpressionEvaluator expressionEvaluator,
            Consumer<String> outputEmitter) {
        this(semanticChecker, new DefaultStatementExecutor(
                expressionEvaluator,
                new builtin.DefaultFunctionRegistry(),
                outputEmitter));
    }

    public DefaultInterpreter(
            SemanticChecker semanticChecker,
            Consumer<String> outputEmitter) {
        this(semanticChecker, new DefaultStatementExecutor(outputEmitter));
    }

    public DefaultInterpreter(Consumer<String> outputEmitter) {
        this(null, outputEmitter);
    }

    public DefaultInterpreter() {
        this(System.out::println);
    }

    @Override
    public Result<SemanticEnvironment> interpret(
            TokenStream tokenStream,
            SemanticEnvironment semanticEnv,
            Environment runtimeEnv) {
        if (semanticChecker == null) {
            return Result.failure("SemanticChecker dependency must be injected into Interpreter.");
        }

        TokenStream currentStream = tokenStream;
        SemanticEnvironment currentSemEnv = semanticEnv;

        while (!currentStream.isEmpty()) {
            Result<SemanticStep> stepResult = semanticChecker.parseAndCheckStatement(currentStream, currentSemEnv);
            if (!stepResult.isCorrect()) {
                String err = ((IncorrectResult<SemanticStep>) stepResult).error();
                if ("EOF".equalsIgnoreCase(err) || err.contains("EOF")) {
                    break;
                }
                return Result.failure(err);
            }

            SemanticStep step = ((CorrectResult<SemanticStep>) stepResult).value();
            Node statement = step.node();
            currentSemEnv = step.updatedEnv();
            currentStream = step.nextStream();

            Result<Void> interpretResult = interpret(new ProgramNode(List.of(statement), 1, 1), runtimeEnv);
            if (!interpretResult.isCorrect()) {
                String err = ((IncorrectResult<Void>) interpretResult).error();
                return Result.failure(err);
            }
        }

        return Result.success(currentSemEnv);
    }

    @Override
    public Result<Void> interpret(ProgramNode program) {
        return interpret(program, new Environment());
    }

    @Override
    public Result<Void> interpret(ProgramNode program, Environment globalEnv) {
        try {
            for (Node statement : program.statements()) {
                statementExecutor.execute(statement, globalEnv);
            }
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure("Runtime error: " + e.getMessage());
        }
    }
}
