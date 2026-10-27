/*
 * My Project
 */

package syntactic.parser;

import iterator.IterationStep;
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
import syntactic.version.LiteralParsers;
import syntactic.version.VersionStrategy;
import syntactic.version.VersionStrategyRegistry;
import token.Token;
import tokenstream.TokenStream;
import version.Version;

public final class ParserFactory {
    private ParserFactory() {}

    public static Parser<Node> createParser(Version version) {
        VersionStrategy strategy =
                new VersionStrategyRegistry()
                        .getStrategy(version != null ? version : Version.V_1_0);
        Parser<IdentifierNode> identifierParser = new IdentifierParser();
        Parser<ExpressionNode> prattParser = getExpressionNodeParser(identifierParser, strategy);
        AtomicReference<Parser<Node>> stmtParserRef = new AtomicReference<>();
        List<StatementParser<? extends Node>> parsers =
                buildStatementParsers(strategy, identifierParser, prattParser, stmtParserRef);
        Parser<Node> statementParser = buildCompositeParser(parsers, strategy.version());
        stmtParserRef.set(statementParser);
        return statementParser;
    }

    private static List<StatementParser<? extends Node>> buildStatementParsers(
            VersionStrategy strategy,
            Parser<IdentifierNode> identifierParser,
            Parser<ExpressionNode> prattParser,
            AtomicReference<Parser<Node>> stmtParserRef) {
        DeclarationParser declarationParser =
                new DeclarationParser(
                        identifierParser,
                        prattParser,
                        strategy.declarationKeywords(),
                        strategy.supportedDataTypes());
        AssignParser assignParser = new AssignParser(identifierParser, prattParser);
        LineExpressionParser lineExprParser = new LineExpressionParser(prattParser);
        ConditionalParser conditionalParser =
                new ConditionalParser(prattParser, stream -> stmtParserRef.get().parse(stream));
        return strategy.statementParsers(
                declarationParser, assignParser, conditionalParser, lineExprParser);
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
                            + versionLabel(version)
                            + ")";
            case IncorrectResult<Token>(String err) ->
                    err.startsWith("Lexical error") ? err : "Unexpected end of file";
        };
    }

    private static String versionLabel(Version version) {
        return version == Version.V_1_0 ? "1.0" : "1.1";
    }

    private static Parser<ExpressionNode> getExpressionNodeParser(
            Parser<IdentifierNode> identifierParser, VersionStrategy strategy) {
        AtomicReference<Parser<ExpressionNode>> exprParserRef = new AtomicReference<>();
        Parser<ExpressionNode> primaryParser =
                createPrimaryParser(identifierParser, strategy, exprParserRef::get);
        Parser<ExpressionNode> prattParser = new OperatorParser(() -> primaryParser);
        exprParserRef.set(prattParser);
        return prattParser;
    }

    private static Parser<ExpressionNode> createPrimaryParser(
            Parser<IdentifierNode> identifierParser,
            VersionStrategy strategy,
            Supplier<Parser<ExpressionNode>> exprParserSupplier) {
        LiteralParsers literalParsers =
                new LiteralParsers(
                        new NumberLiteralParser(),
                        new StringLiteralParser(),
                        new BooleanLiteralParser());
        return stream -> {
            FunctionParser functionParser =
                    new FunctionParser(identifierParser, exprParserSupplier.get());
            List<Parser<? extends ExpressionNode>> primaryList =
                    strategy.primaryParsers(literalParsers, functionParser, identifierParser);
            return parseFirstMatching(primaryList, stream, strategy.version());
        };
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
