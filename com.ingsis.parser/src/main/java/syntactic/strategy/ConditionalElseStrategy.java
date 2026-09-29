/*
 * My Project
 */

package syntactic.strategy;

import iterator.IterationStep;
import node.Node;
import node.keyword.IfKeywordNode;
import result.Result;
import syntactic.Parser;
import tokenstream.TokenStream;

public interface ConditionalElseStrategy {
    boolean matches(TokenStream stream);

    Result<IterationStep<IfKeywordNode>> parseElse(
            ConditionalBlock block, TokenStream stream, Parser<Node> statementParser);
}
