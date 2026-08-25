package syntactic.strategy;

import iterator.IterationStep;
import node.Node;
import node.expression.ExpressionNode;
import node.keyword.IfKeywordNode;
import result.Result;
import syntactic.Parser;
import token.Token;
import tokenstream.TokenStream;

import java.util.List;

public interface ConditionalElseStrategy {
    boolean matches(TokenStream stream);

    Result<IterationStep<IfKeywordNode>> parseElse(
            Token ifToken,
            ExpressionNode condition,
            List<Node> thenBody,
            TokenStream stream,
            Parser<Node> statementParser);
}
