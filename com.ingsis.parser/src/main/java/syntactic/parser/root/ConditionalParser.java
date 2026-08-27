/*
 * My Project
 */

package syntactic.parser.root;

import iterator.IterationStep;
import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
import node.keyword.IfKeywordNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.strategy.ConditionalElseStrategy;
import syntactic.strategy.WithElseStrategy;
import syntactic.strategy.WithoutElseStrategy;
import syntactic.util.BlockParserUtils;
import token.SymbolType;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

public class ConditionalParser implements Parser<IfKeywordNode> {
    private final Parser<ExpressionNode> conditionParser;
    private final Parser<Node> statementParser;
    private final List<ConditionalElseStrategy> elseStrategies;

    public ConditionalParser(
            Parser<ExpressionNode> conditionParser,
            Parser<Node> statementParser,
            List<ConditionalElseStrategy> elseStrategies) {
        this.conditionParser = conditionParser;
        this.statementParser = statementParser;
        this.elseStrategies = elseStrategies;
    }

    public ConditionalParser(Parser<ExpressionNode> conditionParser, Parser<Node> statementParser) {
        this(
                conditionParser,
                statementParser,
                List.of(new WithElseStrategy(), new WithoutElseStrategy()));
    }

    @Override
    public Result<IterationStep<IfKeywordNode>> parse(TokenStream stream) {
        Result<IterationStep<Token>> ifResult = stream.consume(TokenType.IF);
        if (!ifResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) ifResult).error());
        }

        IterationStep<Token> ifStep = ((CorrectResult<IterationStep<Token>>) ifResult).value();
        TokenStream postIfStream = (TokenStream) ifStep.next();

        Result<IterationStep<Token>> lParenResult = postIfStream.consume(TokenType.LPAREN);
        if (!lParenResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) lParenResult).error());
        }

        IterationStep<Token> lParenStep =
                ((CorrectResult<IterationStep<Token>>) lParenResult).value();
        return parseCondition(ifStep.value(), (TokenStream) lParenStep.next());
    }

    private Result<IterationStep<IfKeywordNode>> parseCondition(Token ifToken, TokenStream stream) {
        Result<IterationStep<ExpressionNode>> condResult = conditionParser.parse(stream);
        if (!condResult.isCorrect()) {
            return Result.failure(
                    ((IncorrectResult<IterationStep<ExpressionNode>>) condResult).error());
        }

        IterationStep<ExpressionNode> condStep =
                ((CorrectResult<IterationStep<ExpressionNode>>) condResult).value();
        TokenStream postCondStream = (TokenStream) condStep.next();

        Result<IterationStep<Token>> rParenResult = postCondStream.consume(TokenType.RPAREN);
        if (!rParenResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) rParenResult).error());
        }

        IterationStep<Token> rParenStep =
                ((CorrectResult<IterationStep<Token>>) rParenResult).value();
        return parseThenBody(ifToken, condStep.value(), (TokenStream) rParenStep.next());
    }

    private Result<IterationStep<IfKeywordNode>> parseThenBody(
            Token ifToken, ExpressionNode condition, TokenStream stream) {
        Result<IterationStep<List<Node>>> blockResult =
                BlockParserUtils.parseBlock(
                        stream, statementParser, SymbolType.LBRACE, SymbolType.RBRACE);

        if (!blockResult.isCorrect()) {
            return Result.failure(
                    ((IncorrectResult<IterationStep<List<Node>>>) blockResult).error());
        }

        IterationStep<List<Node>> blockStep =
                ((CorrectResult<IterationStep<List<Node>>>) blockResult).value();
        return parseElse(ifToken, condition, blockStep.value(), (TokenStream) blockStep.next());
    }

    private Result<IterationStep<IfKeywordNode>> parseElse(
            Token ifToken, ExpressionNode condition, List<Node> thenBody, TokenStream stream) {
        for (ConditionalElseStrategy strategy : elseStrategies) {
            if (strategy.matches(stream)) {
                return strategy.parseElse(ifToken, condition, thenBody, stream, statementParser);
            }
        }
        return Result.failure("Unexpected error resolving else strategy");
    }
}
