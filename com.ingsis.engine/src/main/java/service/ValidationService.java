package service;

import charstream.CharStream;
import charstream.StreamCharReader;
import iterator.IterationStep;
import iterator.SafeIterator;
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ValidationService {

    public Result<String> validate(Version version, InputStream in) {
        try {
            StreamCharReader reader = new StreamCharReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            CharStream charStream = new CharStream(reader);
            SafeIterator<Token> lexer = new Lexer(charStream);
            TokenStream currentStream = new LazyTokenStream(lexer);

            syntactic.Parser<Node> statementParser = syntactic.parser.ParserFactory.createParser(version);
            SemanticChecker semanticChecker = new SemanticChecker();

            SemanticEnvironment currentSemEnv = new SemanticEnvironment();
            int statementCount = 0;

            while (!currentStream.isEmpty()) {
                statementCount++;
                Result<IterationStep<Node>> parseResult = statementParser.parse(currentStream);
                if (!parseResult.isCorrect()) {
                    String err = ((IncorrectResult<IterationStep<Node>>) parseResult).error();
                    if ("EOF".equalsIgnoreCase(err) || err.contains("EOF")) {
                        break;
                    }
                    return Result.failure(formatSyntacticError(err, currentStream));
                }

                IterationStep<Node> step = ((CorrectResult<IterationStep<Node>>) parseResult).value();
                Node statement = step.value();
                currentStream = (TokenStream) step.next();

                System.out.printf("[Progress] Parsing statement %d at line %d, column %d...%n",
                        statementCount, statement.line(), statement.column());

                Result<SemanticEnvironment> semResult = semanticChecker.checkNode(statement, currentSemEnv);
                if (!semResult.isCorrect()) {
                    String err = ((IncorrectResult<SemanticEnvironment>) semResult).error();
                    return Result.failure(formatSemanticError(err, statement));
                }
                currentSemEnv = ((CorrectResult<SemanticEnvironment>) semResult).value();
            }

            return new CorrectResult<>("Validation successful: Syntax and semantics are valid.");
        } catch (Exception e) {
            return Result.failure("Validation error: " + e.getMessage());
        }
    }

    private String formatSyntacticError(String rawError, TokenStream stream) {
        Result<Token> peek = stream.peek(0);
        if (peek.isCorrect()) {
            Token t = ((CorrectResult<Token>) peek).value();
            int startLine = t.startPosition() != null ? t.startPosition().line() : 1;
            int startCol = t.startPosition() != null ? t.startPosition().column() : 1;
            int endLine = t.endPosition() != null ? t.endPosition().line() : startLine;
            int endCol = t.endPosition() != null ? t.endPosition().column() : (startCol + (t.value() != null ? t.value().length() : 1));
            return String.format("Syntactic error [Line %d, Column %d to Line %d, Column %d]: %s",
                    startLine, startCol, endLine, endCol, rawError);
        }
        return "Syntactic error: " + rawError;
    }

    private String formatSemanticError(String rawError, Node statement) {
        int startLine = statement.line() != null ? statement.line() : 1;
        int startCol = statement.column() != null ? statement.column() : 1;
        int endLine = startLine;
        int endCol = startCol + 10;
        return String.format("Semantic error [Line %d, Column %d to Line %d, Column %d]: %s",
                startLine, startCol, endLine, endCol, rawError);
    }
}
