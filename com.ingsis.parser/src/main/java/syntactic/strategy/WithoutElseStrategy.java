package syntactic.strategy;

import iterator.IterationStep;
import node.Node;
import node.expression.ExpressionNode;
import node.factory.NodeFactory;
import node.keyword.IfKeywordNode;
import result.CorrectResult;
import result.Result;
import syntactic.Parser;
import token.SymbolType;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

import java.util.List;

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
            Token ifToken,
            ExpressionNode condition,
            List<Node> thenBody,
            TokenStream stream,
            Parser<Node> statementParser) {

        IfKeywordNode ifNode = NodeFactory.createIf(condition, thenBody, List.of(), ifToken);
        return Result.success(new IterationStep<>(ifNode, stream));
    }
}
