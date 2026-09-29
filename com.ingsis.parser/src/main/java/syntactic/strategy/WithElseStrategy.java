/*
 * My Project
 */

package syntactic.strategy;

import iterator.IterationStep;
import java.util.List;
import node.Node;
import node.factory.NodeFactory;
import node.keyword.IfKeywordNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.util.BlockParserUtils;
import token.SymbolType;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

public class WithElseStrategy implements ConditionalElseStrategy {
    @Override
    public boolean matches(TokenStream stream) {
        Result<Token> peek = stream.peek(0);
        return peek.isCorrect() && ((CorrectResult<Token>) peek).value().type() == TokenType.ELSE;
    }

    @Override
    public Result<IterationStep<IfKeywordNode>> parseElse(
            ConditionalBlock block, TokenStream stream, Parser<Node> statementParser) {
        Result<IterationStep<Token>> elseResult = stream.consume(TokenType.ELSE);
        if (!elseResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) elseResult).error());
        }
        TokenStream postElseStream =
                (TokenStream) ((CorrectResult<IterationStep<Token>>) elseResult).value().next();
        return parseElseBody(block, postElseStream, statementParser);
    }

    private Result<IterationStep<IfKeywordNode>> parseElseBody(
            ConditionalBlock block, TokenStream postElseStream, Parser<Node> statementParser) {
        Result<IterationStep<List<Node>>> elseBlockResult =
                BlockParserUtils.parseBlock(
                        postElseStream, statementParser, SymbolType.LBRACE, SymbolType.RBRACE);
        if (!elseBlockResult.isCorrect()) {
            return Result.failure(
                    ((IncorrectResult<IterationStep<List<Node>>>) elseBlockResult).error());
        }
        IterationStep<List<Node>> elseBlockStep =
                ((CorrectResult<IterationStep<List<Node>>>) elseBlockResult).value();
        IfKeywordNode ifNode =
                NodeFactory.createIf(
                        block.condition(),
                        block.thenBody(),
                        elseBlockStep.value(),
                        block.ifToken());
        return Result.success(new IterationStep<>(ifNode, (TokenStream) elseBlockStep.next()));
    }
}
