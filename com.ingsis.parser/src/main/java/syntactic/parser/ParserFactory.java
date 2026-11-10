/*
 * My Project
 */

package syntactic.parser;

import iterator.IterationStep;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import node.Node;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.parser.literal.BooleanLiteralParser;
import syntactic.parser.literal.IdentifierParser;
import syntactic.parser.literal.NumberLiteralParser;
import syntactic.parser.literal.StringLiteralParser;
import syntactic.parser.operator.OperatorParser;
import syntactic.parser.root.AssignParser;
import syntactic.parser.root.ConditionalParser;
import syntactic.parser.root.DeclarationParser;
import syntactic.parser.root.FunctionParser;
import syntactic.parser.root.LineExpressionParser;
import syntactic.parser.root.StatementParser;
import syntactic.parser.root.WhileParser;
import syntactic.version.LanguageFeatures;
import syntactic.version.LanguageFeatures.Feature;
import token.Token;
import tokenstream.TokenStream;
import version.Version;

public final class ParserFactory {
    private ParserFactory() {}

    public static Parser<Node> createParser(Version version) {
        LanguageFeatures features =
                LanguageFeatures.forVersion(version != null ? version : Version.V_1_0);
        Parser<IdentifierNode> identifierParser = new IdentifierParser();
        Parser<ExpressionNode> prattParser = getExpressionNodeParser(identifierParser, features);
        AtomicReference<Parser<Node>> stmtParserRef = new AtomicReference<>();
        List<StatementParser<? extends Node>> parsers =
                buildStatementParsers(features, identifierParser, prattParser, stmtParserRef);
        Parser<Node> statementParser = buildCompositeParser(parsers, features.version());
        stmtParserRef.set(statementParser);
        return statementParser;
    }

    // Order matters: the line-expression parser accepts any expression, so it goes last.
    private static List<StatementParser<? extends Node>> buildStatementParsers(
            LanguageFeatures features,
            Parser<IdentifierNode> identifierParser,
            Parser<ExpressionNode> prattParser,
            AtomicReference<Parser<Node>> stmtParserRef) {
        List<StatementParser<? extends Node>> parsers = new ArrayList<>();
        parsers.add(
                new DeclarationParser(
                        identifierParser,
                        prattParser,
                        features.declarationKeywordMatcher(),
                        features.dataTypeMatcher()));
        parsers.add(new AssignParser(identifierParser, prattParser));
        parsers.addAll(blockStatementParsers(features, prattParser, stmtParserRef));
        parsers.add(new LineExpressionParser(prattParser));
        return parsers;
    }

    // Statements that own a block: their body is parsed with the full statement parser.
    private static List<StatementParser<? extends Node>> blockStatementParsers(
            LanguageFeatures features,
            Parser<ExpressionNode> prattParser,
            AtomicReference<Parser<Node>> stmtParserRef) {
        Parser<Node> statementParser = stream -> stmtParserRef.get().parse(stream);
        List<StatementParser<? extends Node>> parsers = new ArrayList<>();
        if (features.supports(Feature.IF_STATEMENT)) {
            parsers.add(new ConditionalParser(prattParser, statementParser));
        }
        if (features.supports(Feature.WHILE_STATEMENT)) {
            parsers.add(new WhileParser(prattParser, statementParser));
        }
        return parsers;
    }

    // Predictive dispatch: the first parser whose lookahead accepts the stream owns the statement,
    // so its specific error is the one reported to the user.
    private static Parser<Node> buildCompositeParser(
            List<StatementParser<? extends Node>> parsers, Version version) {
        return stream -> {
            if (stream == null || stream.isEmpty()) {
                return Result.failure("EOF");
            }
            for (StatementParser<? extends Node> p : parsers) {
                if (p.canStart(stream)) {
                    return widen(p.parse(stream));
                }
            }
            return Result.failure(unexpectedToken(stream, version));
        };
    }

    private static <T extends Node> Result<IterationStep<T>> widen(
            Result<? extends IterationStep<? extends T>> res) {
        return switch (res) {
            case CorrectResult<? extends IterationStep<? extends T>>(var step) ->
                    Result.success(new IterationStep<>(step.value(), step.next()));
            case IncorrectResult<? extends IterationStep<? extends T>>(String err) ->
                    Result.failure(err);
        };
    }

    private static String unexpectedToken(TokenStream stream, Version version) {
        return switch (stream.peek(0)) {
            case CorrectResult<Token>(Token token) ->
                    "Unexpected token '"
                            + token.value()
                            + "' "
                            + token.range()
                            + " (not valid in PrintScript "
                            + version.label()
                            + ")";
            case IncorrectResult<Token>(String err) ->
                    err.startsWith("Lexical error") ? err : "Unexpected end of file";
        };
    }

    private static Parser<ExpressionNode> getExpressionNodeParser(
            Parser<IdentifierNode> identifierParser, LanguageFeatures features) {
        AtomicReference<Parser<ExpressionNode>> exprParserRef = new AtomicReference<>();
        Parser<ExpressionNode> primaryParser =
                createPrimaryParser(identifierParser, features, exprParserRef::get);
        Parser<ExpressionNode> prattParser = new OperatorParser(() -> primaryParser);
        exprParserRef.set(prattParser);
        return prattParser;
    }

    private static Parser<ExpressionNode> createPrimaryParser(
            Parser<IdentifierNode> identifierParser,
            LanguageFeatures features,
            Supplier<Parser<ExpressionNode>> exprParserSupplier) {
        return stream -> {
            FunctionParser functionParser =
                    new FunctionParser(identifierParser, exprParserSupplier.get());
            List<Parser<? extends ExpressionNode>> primaryList =
                    primaryParsers(features, functionParser, identifierParser);
            return parseFirstMatching(primaryList, stream, features.version());
        };
    }

    // Order matters: a function call must be tried before a plain identifier.
    private static List<Parser<? extends ExpressionNode>> primaryParsers(
            LanguageFeatures features,
            FunctionParser functionParser,
            Parser<IdentifierNode> identifierParser) {
        List<Parser<? extends ExpressionNode>> parsers = new ArrayList<>();
        parsers.add(new NumberLiteralParser());
        parsers.add(new StringLiteralParser());
        if (features.supports(Feature.BOOLEAN_LITERAL)) {
            parsers.add(new BooleanLiteralParser());
        }
        parsers.add(functionParser);
        parsers.add(identifierParser);
        return parsers;
    }

    private static Result<IterationStep<ExpressionNode>> parseFirstMatching(
            List<Parser<? extends ExpressionNode>> parsers, TokenStream stream, Version version) {
        for (Parser<? extends ExpressionNode> p : parsers) {
            Result<? extends IterationStep<? extends ExpressionNode>> res = p.parse(stream);
            if (res.isCorrect()) {
                return widen(res);
            }
        }
        return Result.failure(unexpectedToken(stream, version));
    }
}
