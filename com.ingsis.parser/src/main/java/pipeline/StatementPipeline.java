/*
 * My Project
 */

package pipeline;

import iterator.IterationStep;
import java.util.Locale;
import node.Node;
import position.Position;
import position.SourceRange;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import syntactic.Parser;
import syntactic.parser.ParserFactory;
import token.Token;
import tokenstream.TokenStream;
import version.Version;

/**
 * Drives the streaming flow shared by every tool: parse one statement, check it semantically, hand
 * it to a {@link StatementAction} and move on. Only one statement is alive at a time, so sources of
 * any size can be processed. Every error is labelled (syntactic, semantic, runtime) and carries the
 * position where it happened.
 */
public final class StatementPipeline {
    private final Parser<Node> parser;
    private final SemanticChecker checker;

    public StatementPipeline(Parser<Node> parser, SemanticChecker checker) {
        this.parser = parser;
        this.checker = checker;
    }

    public StatementPipeline(Version version) {
        this(ParserFactory.createParser(version), new SemanticChecker(version));
    }

    public Result<SemanticEnvironment> run(
            TokenStream stream, SemanticEnvironment env, StatementAction action) {
        TokenStream current = stream;
        SemanticEnvironment currentEnv = env;
        while (!current.isEmpty()) {
            Result<IterationStep<Node>> parsed = parser.parse(current);
            if (parsed instanceof IncorrectResult<IterationStep<Node>>(String err)) {
                return endOfInputOrSyntacticError(err, current, currentEnv);
            }
            IterationStep<Node> step = ((CorrectResult<IterationStep<Node>>) parsed).value();
            TokenStream next = (TokenStream) step.next();
            Result<SemanticEnvironment> processed =
                    process(step.value(), currentEnv, action, statementRange(current, next));
            if (!(processed instanceof CorrectResult<SemanticEnvironment>(var updated))) {
                return processed;
            }
            currentEnv = updated;
            current = next;
        }
        return Result.success(currentEnv);
    }

    private Result<SemanticEnvironment> process(
            Node statement, SemanticEnvironment env, StatementAction action, SourceRange range) {
        SemanticEnvironment nextEnv = env;
        if (checker != null) {
            Result<SemanticEnvironment> checked = checker.checkNode(statement, env);
            if (checked instanceof IncorrectResult<SemanticEnvironment>(String err)) {
                return Result.failure(labelled("Semantic", err, range.toString()));
            }
            nextEnv = ((CorrectResult<SemanticEnvironment>) checked).value();
        }
        Result<Void> applied = action.apply(statement, nextEnv);
        if (applied instanceof IncorrectResult<Void>(String err)) {
            return Result.failure(labelled("Runtime", err, range.toString()));
        }
        return Result.success(nextEnv);
    }

    private static Result<SemanticEnvironment> endOfInputOrSyntacticError(
            String error, TokenStream statementStart, SemanticEnvironment env) {
        if (error.toUpperCase(Locale.ROOT).startsWith("EOF")) {
            return Result.success(env);
        }
        return Result.failure(syntacticError(error, statementStart));
    }

    private static String syntacticError(String error, TokenStream statementStart) {
        String location =
                switch (statementStart.peek(0)) {
                    case CorrectResult<Token>(Token token) ->
                            "in statement starting at " + describe(token.startPosition());
                    case IncorrectResult<Token> ignored -> "at an unknown position";
                };
        String kind = error.startsWith("Lexical error") ? "Lexical" : "Syntactic";
        return labelled(kind, error, location);
    }

    private static String labelled(String kind, String error, String location) {
        String prefix = kind + " error";
        String message = error.startsWith(prefix) ? error : prefix + ": " + error;
        return message + " [" + location + "]";
    }

    /** Range from the first token of the statement to the last one it consumed. */
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
}
