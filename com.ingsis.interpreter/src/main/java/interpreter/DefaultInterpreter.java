/*
 * My Project
 */

package interpreter;

import environment.Environment;
import evaluator.ExpressionEvaluator;
import executor.DefaultStatementExecutor;
import executor.StatementExecutor;
import iterator.IterationStep;
import java.util.function.Consumer;
import node.Node;
import node.ProgramNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import tokenstream.TokenStream;

public class DefaultInterpreter implements Interpreter {
    private final syntactic.Parser<Node> statementParser;
    private final SemanticChecker semanticChecker;
    private final StatementExecutor statementExecutor;

    public DefaultInterpreter(
            syntactic.Parser<Node> statementParser,
            SemanticChecker semanticChecker,
            StatementExecutor statementExecutor) {
        this.statementParser = statementParser;
        this.semanticChecker = semanticChecker;
        this.statementExecutor = statementExecutor;
    }

    public DefaultInterpreter(
            syntactic.Parser<Node> statementParser,
            SemanticChecker semanticChecker,
            Consumer<String> outputEmitter,
            builtin.FunctionRegistry functionRegistry) {
        this(
                statementParser,
                semanticChecker,
                new DefaultStatementExecutor(outputEmitter, functionRegistry));
    }

    public DefaultInterpreter(
            SemanticChecker semanticChecker, StatementExecutor statementExecutor) {
        this(
                syntactic.parser.ParserFactory.createParser(version.Version.V_1_0),
                semanticChecker,
                statementExecutor);
    }

    public DefaultInterpreter(
            SemanticChecker semanticChecker,
            ExpressionEvaluator expressionEvaluator,
            Consumer<String> outputEmitter) {
        this(
                syntactic.parser.ParserFactory.createParser(version.Version.V_1_0),
                semanticChecker,
                new DefaultStatementExecutor(
                        expressionEvaluator, new builtin.DefaultFunctionRegistry(), outputEmitter));
    }

    public DefaultInterpreter(
            SemanticChecker semanticChecker,
            Consumer<String> outputEmitter,
            builtin.provider.InputProvider inputProvider,
            builtin.provider.EnvProvider envProvider) {
        this(
                syntactic.parser.ParserFactory.createParser(version.Version.V_1_0),
                semanticChecker,
                outputEmitter,
                new builtin.DefaultFunctionRegistry(inputProvider, envProvider));
    }

    public DefaultInterpreter(SemanticChecker semanticChecker, Consumer<String> outputEmitter) {
        this(semanticChecker, outputEmitter, prompt -> "", System::getenv);
    }

    public DefaultInterpreter(Consumer<String> outputEmitter) {
        this(new SemanticChecker(), outputEmitter);
    }

    public DefaultInterpreter() {
        this(System.out::println);
    }

    @Override
    public Result<SemanticEnvironment> interpret(
            TokenStream tokenStream, SemanticEnvironment semanticEnv, Environment runtimeEnv) {
        if (statementParser == null) {
            return Result.failure("Syntactic parser dependency must be injected into Interpreter.");
        }
        return executeStream(tokenStream, semanticEnv, runtimeEnv);
    }

    private Result<SemanticEnvironment> executeStream(
            TokenStream stream, SemanticEnvironment semEnv, Environment runtimeEnv) {
        TokenStream currentStream = stream;
        SemanticEnvironment currentSemEnv = semEnv;
        while (!currentStream.isEmpty()) {
            Result<IterationStep<Node>> parseResult = statementParser.parse(currentStream);
            if (!parseResult.isCorrect()) {
                return handleParseError(parseResult, currentSemEnv);
            }
            IterationStep<Node> step = ((CorrectResult<IterationStep<Node>>) parseResult).value();
            currentStream = (TokenStream) step.next();
            Result<SemanticEnvironment> stepRes =
                    processNode(step.value(), currentSemEnv, runtimeEnv);
            if (!stepRes.isCorrect()) {
                return stepRes;
            }
            currentSemEnv = ((CorrectResult<SemanticEnvironment>) stepRes).value();
        }
        return Result.success(currentSemEnv);
    }

    private Result<SemanticEnvironment> handleParseError(
            Result<IterationStep<Node>> parseResult, SemanticEnvironment currentSemEnv) {
        String err = ((IncorrectResult<IterationStep<Node>>) parseResult).error();
        if ("EOF".equalsIgnoreCase(err) || err.contains("EOF")) {
            return Result.success(currentSemEnv);
        }
        return Result.failure(err.startsWith("Syntactic error:") ? err : "Syntactic error: " + err);
    }

    private Result<SemanticEnvironment> processNode(
            Node statement, SemanticEnvironment semEnv, Environment runtimeEnv) {
        SemanticEnvironment nextEnv = semEnv;
        if (semanticChecker != null) {
            Result<SemanticEnvironment> semResult = semanticChecker.checkNode(statement, semEnv);
            if (!semResult.isCorrect()) {
                String err = ((IncorrectResult<SemanticEnvironment>) semResult).error();
                return Result.failure(
                        err.startsWith("Semantic error:") ? err : "Semantic error: " + err);
            }
            nextEnv = ((CorrectResult<SemanticEnvironment>) semResult).value();
        }
        Result<Void> execRes = statementExecutor.execute(statement, runtimeEnv);
        if (!execRes.isCorrect()) {
            String err = ((IncorrectResult<Void>) execRes).error();
            return Result.failure(err.startsWith("Runtime error:") ? err : "Runtime error: " + err);
        }
        return Result.success(nextEnv);
    }

    @Override
    public Result<Void> interpret(ProgramNode program) {
        return interpret(program, new Environment());
    }

    @Override
    public Result<Void> interpret(ProgramNode program, Environment globalEnv) {
        for (Node statement : program.statements()) {
            Result<Void> execRes = statementExecutor.execute(statement, globalEnv);
            if (!execRes.isCorrect()) {
                String err = ((IncorrectResult<Void>) execRes).error();
                return Result.failure(
                        err.startsWith("Runtime error:") ? err : "Runtime error: " + err);
            }
        }
        return Result.success(null);
    }
}
