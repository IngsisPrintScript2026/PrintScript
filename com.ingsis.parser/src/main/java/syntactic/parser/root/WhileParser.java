/*
 * My Project
 */

package syntactic.parser.root;

import iterator.IterationStep;
import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
import node.factory.NodeFactory;
import node.keyword.WhileKeywordNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.util.BlockParserUtils;
import token.SymbolType;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;
import tokenstream.rules.TokenMatchers;

public class WhileParser implements StatementParser<WhileKeywordNode> {
    private final Parser<ExpressionNode> conditionParser;
    private final Parser<Node> statementParser;

    public WhileParser(Parser<ExpressionNode> conditionParser, Parser<Node> statementParser) {
        this.conditionParser = conditionParser;
        this.statementParser = statementParser;
    }

    @Override
    public boolean canStart(TokenStream stream) {
        return StatementParser.peekMatches(stream, 0, TokenMatchers.isType(TokenType.WHILE));
    }

    @Override
    public Result<IterationStep<WhileKeywordNode>> parse(TokenStream stream) {
        Result<IterationStep<Token>> whileRes = stream.consume(TokenType.WHILE);
        if (!(whileRes instanceof CorrectResult<IterationStep<Token>>(var whileStep))) {
            return failure(whileRes);
        }
        TokenStream afterWhile = (TokenStream) whileStep.next();
        Result<IterationStep<Token>> openRes = afterWhile.consume(TokenType.LPAREN);
        if (!(openRes instanceof CorrectResult<IterationStep<Token>>(var openStep))) {
            return failure(openRes);
        }
        return parseCondition(whileStep.value(), (TokenStream) openStep.next());
    }

    private Result<IterationStep<WhileKeywordNode>> parseCondition(
            Token whileToken, TokenStream stream) {
        Result<IterationStep<ExpressionNode>> condRes = conditionParser.parse(stream);
        if (!(condRes instanceof CorrectResult<IterationStep<ExpressionNode>>(var condStep))) {
            return failure(condRes);
        }
        TokenStream afterCondition = (TokenStream) condStep.next();
        Result<IterationStep<Token>> closeRes = afterCondition.consume(TokenType.RPAREN);
        if (!(closeRes instanceof CorrectResult<IterationStep<Token>>(var closeStep))) {
            return failure(closeRes);
        }
        return parseBody(whileToken, condStep.value(), (TokenStream) closeStep.next());
    }

    private Result<IterationStep<WhileKeywordNode>> parseBody(
            Token whileToken, ExpressionNode condition, TokenStream stream) {
        Result<IterationStep<List<Node>>> blockRes =
                BlockParserUtils.parseBlock(
                        stream, statementParser, SymbolType.LBRACE, SymbolType.RBRACE);
        if (!(blockRes instanceof CorrectResult<IterationStep<List<Node>>>(var blockStep))) {
            return failure(blockRes);
        }
        WhileKeywordNode node = NodeFactory.createWhile(condition, blockStep.value(), whileToken);
        return Result.success(new IterationStep<>(node, blockStep.next()));
    }

    private static <T> Result<IterationStep<WhileKeywordNode>> failure(Result<T> result) {
        return Result.failure(((IncorrectResult<T>) result).error());
    }
}
