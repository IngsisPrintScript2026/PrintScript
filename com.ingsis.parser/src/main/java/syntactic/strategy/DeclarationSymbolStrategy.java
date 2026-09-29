/*
 * My Project
 */

package syntactic.strategy;

import iterator.IterationStep;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.keyword.DeclarationKeywordNode;
import result.Result;
import syntactic.Parser;
import token.SymbolType;
import tokenstream.TokenStream;

public interface DeclarationSymbolStrategy {
    SymbolType targetSymbol();

    Result<IterationStep<DeclarationKeywordNode>> parse(
            node.keyword.declaration.DeclarationInfo info,
            IdentifierNode identifier,
            TokenStream stream,
            Parser<ExpressionNode> expressionParser);
}
