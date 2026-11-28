/*
 * My Project
 */

package syntactic.strategy;

import iterator.IterationStep;
import java.util.List;
import node.IfKeywordNode;
import node.StatementNode;
import node.factory.NodeFactory;
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
            ConditionalBlock block, TokenStream stream, Parser<StatementNode> statementParser) {
        Result<IterationStep<Token>> elseResult = stream.consume(TokenType.ELSE);
        if (!elseResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) elseResult).error());
        }
        TokenStream postElseStream =
                (TokenStream) ((CorrectResult<IterationStep<Token>>) elseResult).value().next();
        return parseElseBody(block, postElseStream, statementParser);
    }

    private Result<IterationStep<IfKeywordNode>> parseElseBody(
            ConditionalBlock block,
            TokenStream postElseStream,
            Parser<StatementNode> statementParser) {
        Result<IterationStep<List<StatementNode>>> elseBlockResult =
                BlockParserUtils.parseBlock(
                        postElseStream, statementParser, SymbolType.LBRACE, SymbolType.RBRACE);
        if (!elseBlockResult.isCorrect()) {
            return Result.failure(
                    ((IncorrectResult<IterationStep<List<StatementNode>>>) elseBlockResult)
                            .error());
        }
        IterationStep<List<StatementNode>> elseBlockStep =
                ((CorrectResult<IterationStep<List<StatementNode>>>) elseBlockResult).value();
        IfKeywordNode ifNode =
                NodeFactory.createIf(
                        block.condition(),
                        block.thenBody(),
                        elseBlockStep.value(),
                        block.ifToken());
        return Result.success(new IterationStep<>(ifNode, (TokenStream) elseBlockStep.next()));
    }
}
