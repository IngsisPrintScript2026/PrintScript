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
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import result.CorrectResult;
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
import syntactic.version.VersionStrategy;
import syntactic.version.VersionStrategyRegistry;
import version.Version;

public final class ParserFactory {
    private ParserFactory() {}

    public static Parser<Node> createParser(Version version) {
        VersionStrategy strategy =
                new VersionStrategyRegistry()
                        .getStrategy(version != null ? version : Version.V_1_0);
        Parser<IdentifierNode> identifierParser = new IdentifierParser();
        Parser<ExpressionNode> prattParser = getExpressionNodeParser(identifierParser, strategy);

        DeclarationParser declarationParser =
                new DeclarationParser(
                        identifierParser,
                        prattParser,
                        strategy.declarationKeywords(),
                        strategy.supportedDataTypes());
        AssignParser assignParser = new AssignParser(identifierParser, prattParser);
        LineExpressionParser lineExprParser = new LineExpressionParser(prattParser);

        AtomicReference<Parser<Node>> stmtParserRef = new AtomicReference<>();
        ConditionalParser conditionalParser =
                new ConditionalParser(prattParser, stream -> stmtParserRef.get().parse(stream));

        List<Parser<? extends Node>> parsers =
                strategy.statementParsers(
                        declarationParser, assignParser, conditionalParser, lineExprParser);

        Parser<Node> statementParser =
                stream -> {
                    if (stream == null || stream.isEmpty()) {
                        return Result.failure("EOF");
                    }
                    for (Parser<? extends Node> p : parsers) {
                        Result<? extends IterationStep<? extends Node>> res = p.parse(stream);
                        if (res.isCorrect()) {
                            IterationStep<? extends Node> step =
                                    ((CorrectResult<? extends IterationStep<? extends Node>>) res)
                                            .value();
                            return Result.success(new IterationStep<>(step.value(), step.next()));
                        }
                    }
                    return Result.failure("Failed to parse statement at token stream");
                };

        stmtParserRef.set(statementParser);
        return statementParser;
    }

    private static Parser<ExpressionNode> getExpressionNodeParser(
            Parser<IdentifierNode> identifierParser, VersionStrategy strategy) {
        Parser<NumberLiteralNode> numberLiteralParser = new NumberLiteralParser();
        Parser<StringLiteralNode> stringLiteralParser = new StringLiteralParser();
        Parser<BooleanLiteralNode> booleanLiteralParser = new BooleanLiteralParser();

        AtomicReference<Parser<ExpressionNode>> exprParserRef = new AtomicReference<>();
        Supplier<Parser<ExpressionNode>> exprParserSupplier = () -> exprParserRef.get();

        Parser<ExpressionNode> primaryParser =
                stream -> {
                    FunctionParser functionParser =
                            new FunctionParser(identifierParser, exprParserSupplier.get());
                    List<Parser<? extends ExpressionNode>> primaryList =
                            strategy.primaryParsers(
                                    numberLiteralParser,
                                    stringLiteralParser,
                                    booleanLiteralParser,
                                    functionParser,
                                    identifierParser);
                    for (Parser<? extends ExpressionNode> p : primaryList) {
                        Result<? extends IterationStep<? extends ExpressionNode>> res =
                                p.parse(stream);
                        if (res.isCorrect()) {
                            IterationStep<? extends ExpressionNode> step =
                                    ((CorrectResult<
                                                            ? extends
                                                                    IterationStep<
                                                                            ? extends
                                                                                    ExpressionNode>>)
                                                    res)
                                            .value();
                            return Result.success(new IterationStep<>(step.value(), step.next()));
                        }
                    }
                    return Result.failure("Unrecognized primary expression token stream");
                };

        Parser<ExpressionNode> prattParser = new OperatorParser(() -> primaryParser);
        exprParserRef.set(prattParser);
        return prattParser;
    }
}
