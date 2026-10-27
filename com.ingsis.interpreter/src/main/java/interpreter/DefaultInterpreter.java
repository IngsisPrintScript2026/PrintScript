/*
 * My Project
 */

package interpreter;

import environment.Environment;
import evaluator.ExpressionEvaluator;
import executor.DefaultStatementExecutor;
import executor.StatementExecutor;
import iterator.IterationStep;
import java.util.Locale;
import java.util.function.Consumer;
import node.Node;
import node.ProgramNode;
import position.Position;
import position.SourceRange;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import token.Token;
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
        TokenStream[] source = new TokenStream[] {tokenStream};
        tokenStream = null;
        return executeLoop(source, semanticEnv, runtimeEnv);
    }

    private Result<SemanticEnvironment> executeLoop(
            TokenStream[] source, SemanticEnvironment env, Environment runtimeEnv) {
        TokenStream current = source[0];
        source[0] = null;
        while (!current.isEmpty()) {
            Result<IterationStep<Node>> parsed = statementParser.parse(current);
            if (!parsed.isCorrect()) {
                return handleParseError(parsed, current, env);
            }
            IterationStep<Node> step = ((CorrectResult<IterationStep<Node>>) parsed).value();
            TokenStream next = (TokenStream) step.next();
            Result<SemanticEnvironment> res =
                    process(step.value(), env, runtimeEnv, statementRange(current, next));
            if (!res.isCorrect()) {
                return res;
            }
            env = ((CorrectResult<SemanticEnvironment>) res).value();
            current = next;
        }
        return Result.success(env);
    }

    private Result<SemanticEnvironment> handleParseError(
            Result<IterationStep<Node>> parseResult,
            TokenStream statementStart,
            SemanticEnvironment currentSemEnv) {
        String err = ((IncorrectResult<IterationStep<Node>>) parseResult).error();
        if (err.toUpperCase(Locale.ROOT).startsWith("EOF")) {
            return Result.success(currentSemEnv);
        }
        String location = describeTokenStart(statementStart);
        String kind = err.startsWith("Lexical error") ? "Lexical" : "Syntactic";
        return Result.failure(labelled(kind, err, location));
    }

    private String describeTokenStart(TokenStream statementStart) {
        return switch (statementStart.peek(0)) {
            case CorrectResult<Token>(Token token) ->
                    "in statement starting at " + describe(token.startPosition());
            case IncorrectResult<Token> ignored -> "at an unknown position";
        };
    }

    private Result<SemanticEnvironment> process(
            Node statement, SemanticEnvironment semEnv, Environment runtimeEnv, SourceRange range) {
        SemanticEnvironment nextEnv = semEnv;
        if (semanticChecker != null) {
            Result<SemanticEnvironment> semResult = semanticChecker.checkNode(statement, semEnv);
            if (!semResult.isCorrect()) {
                String err = ((IncorrectResult<SemanticEnvironment>) semResult).error();
                return Result.failure(labelled("Semantic", err, range.toString()));
            }
            nextEnv = ((CorrectResult<SemanticEnvironment>) semResult).value();
        }
        Result<Void> execRes = statementExecutor.execute(statement, runtimeEnv);
        if (!execRes.isCorrect()) {
            String err = ((IncorrectResult<Void>) execRes).error();
            return Result.failure(labelled("Runtime", err, range.toString()));
        }
        return Result.success(nextEnv);
    }

    private static String labelled(String kind, String error, String location) {
        String prefix = kind + " error";
        String message = error.startsWith(prefix) ? error : prefix + ": " + error;
        return message + " [" + location + "]";
    }

    private static SourceRange statementRange(TokenStream before, TokenStream after) {
        Result<Token> first = before.peek(0);
        Result<Token> last = before.peek(Math.max(0, after.pointer() - before.pointer() - 1));
        if (first instanceof CorrectResult<Token>(Token start)
                && last instanceof CorrectResult<Token>(Token end)) {
            return new SourceRange(start.startPosition(), end.range().end());
        }
        return new SourceRange(null, null);
    }

    private static String describe(Position position) {
        return position != null ? position.describe() : "an unknown position";
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
