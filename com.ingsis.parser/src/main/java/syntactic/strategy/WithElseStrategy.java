/*
 * My Project
 */

package syntactic.strategy;

import iterator.IterationStep;
import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
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
            Token ifToken,
            ExpressionNode condition,
            List<Node> thenBody,
            TokenStream stream,
            Parser<Node> statementParser) {

        Result<IterationStep<Token>> elseResult = stream.consume(TokenType.ELSE);
        if (!elseResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) elseResult).error());
        }

        IterationStep<Token> elseStep = ((CorrectResult<IterationStep<Token>>) elseResult).value();
        TokenStream postElseTokenStream = (TokenStream) elseStep.next();

        Result<IterationStep<List<Node>>> elseBlockResult =
                BlockParserUtils.parseBlock(
                        postElseTokenStream, statementParser, SymbolType.LBRACE, SymbolType.RBRACE);

        if (!elseBlockResult.isCorrect()) {
            return Result.failure(
                    ((IncorrectResult<IterationStep<List<Node>>>) elseBlockResult).error());
        }

        IterationStep<List<Node>> elseBlockStep =
                ((CorrectResult<IterationStep<List<Node>>>) elseBlockResult).value();
        List<Node> elseBody = elseBlockStep.value();
        TokenStream postElseStream = (TokenStream) elseBlockStep.next();

        IfKeywordNode ifNode = NodeFactory.createIf(condition, thenBody, elseBody, ifToken);
        return Result.success(new IterationStep<>(ifNode, postElseStream));
    }
}
