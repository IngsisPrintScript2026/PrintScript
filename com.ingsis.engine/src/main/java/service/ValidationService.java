/*
 * My Project
 */

package service;

import charstream.CharStream;
import charstream.StreamCharReader;
import iterator.IterationStep;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lexer.Lexer;
import node.Node;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import token.Token;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;
import version.Version;

public class ValidationService {

    public Result<String> validate(Version version, InputStream in) {
        try {
            CharStream charStream =
                    new CharStream(
                            new StreamCharReader(
                                    new InputStreamReader(in, StandardCharsets.UTF_8)));
            TokenStream currentStream = new LazyTokenStream(new Lexer(charStream));
            syntactic.Parser<Node> parser = syntactic.parser.ParserFactory.createParser(version);
            return validateStatements(currentStream, parser, new SemanticChecker());
        } catch (Exception e) {
            return Result.failure("Validation error: " + e.getMessage());
        }
    }

    private Result<String> validateStatements(
            TokenStream stream, syntactic.Parser<Node> parser, SemanticChecker checker) {
        TokenStream currentStream = stream;
        SemanticEnvironment currentSemEnv = new SemanticEnvironment();
        while (!currentStream.isEmpty()) {
            Result<IterationStep<Node>> parseResult = parser.parse(currentStream);
            if (!parseResult.isCorrect()) {
                return handleParseError(parseResult, currentStream);
            }
            IterationStep<Node> step = ((CorrectResult<IterationStep<Node>>) parseResult).value();
            currentStream = (TokenStream) step.next();
            Result<SemanticEnvironment> semRes =
                    checkStatement(checker, step.value(), currentSemEnv);
            if (!semRes.isCorrect()) {
                return Result.failure(((IncorrectResult<SemanticEnvironment>) semRes).error());
            }
            currentSemEnv = ((CorrectResult<SemanticEnvironment>) semRes).value();
        }
        return new CorrectResult<>("Validation successful: Syntax and semantics are valid.");
    }

    private Result<String> handleParseError(
            Result<IterationStep<Node>> parseResult, TokenStream stream) {
        String err = ((IncorrectResult<IterationStep<Node>>) parseResult).error();
        if ("EOF".equalsIgnoreCase(err) || err.contains("EOF")) {
            return new CorrectResult<>("Validation successful: Syntax and semantics are valid.");
        }
        return Result.failure(formatSyntacticError(err, stream));
    }

    private Result<SemanticEnvironment> checkStatement(
            SemanticChecker checker, Node statement, SemanticEnvironment env) {
        Result<SemanticEnvironment> res = checker.checkNode(statement, env);
        if (!res.isCorrect()) {
            String err = ((IncorrectResult<SemanticEnvironment>) res).error();
            return Result.failure(formatSemanticError(err, statement));
        }
        return res;
    }

    private String formatSyntacticError(String rawError, TokenStream stream) {
        Result<Token> peek = stream.peek(0);
        if (peek.isCorrect()) {
            Token t = ((CorrectResult<Token>) peek).value();
            int startLine = t.startPosition() != null ? t.startPosition().line() : 1;
            int startCol = t.startPosition() != null ? t.startPosition().column() : 1;
            int endLine = t.endPosition() != null ? t.endPosition().line() : startLine;
            int endCol =
                    t.endPosition() != null
                            ? t.endPosition().column()
                            : (startCol + (t.value() != null ? t.value().length() : 1));
            return String.format(
                    "Syntactic error [Line %d, Column %d to Line %d, Column %d]: %s",
                    startLine, startCol, endLine, endCol, rawError);
        }
        return "Syntactic error: " + rawError;
    }

    private String formatSemanticError(String rawError, Node statement) {
        int startLine = statement.line() != null ? statement.line() : 1;
        int startCol = statement.column() != null ? statement.column() : 1;
        int endLine = startLine;
        int endCol = startCol + 10;
        return String.format(
                "Semantic error [Line %d, Column %d to Line %d, Column %d]: %s",
                startLine, startCol, endLine, endCol, rawError);
    }
}
