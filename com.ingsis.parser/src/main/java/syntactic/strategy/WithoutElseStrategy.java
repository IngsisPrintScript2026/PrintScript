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
import result.Result;
import syntactic.Parser;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

public class WithoutElseStrategy implements ConditionalElseStrategy {
    @Override
    public boolean matches(TokenStream stream) {
        Result<Token> peek = stream.peek(0);
        if (!peek.isCorrect()) {
            return true;
        }
        Token token = ((CorrectResult<Token>) peek).value();
        return token.type() != TokenType.ELSE;
    }

    @Override
    public Result<IterationStep<IfKeywordNode>> parseElse(
            ConditionalBlock block, TokenStream stream, Parser<StatementNode> statementParser) {
        IfKeywordNode ifNode =
                NodeFactory.createIf(
                        block.condition(), block.thenBody(), List.of(), block.ifToken());
        return Result.success(new IterationStep<>(ifNode, stream));
    }
}
