/*
 * My Project
 */

package syntactic.strategy;

import iterator.IterationStep;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;
import result.Result;
import syntactic.Parser;
import token.SymbolType;
import token.Token;
import tokenstream.TokenStream;

public interface DeclarationSymbolStrategy {
    SymbolType targetSymbol();

    Result<IterationStep<DeclarationKeywordNode>> parse(
            Token keywordToken,
            DeclarationType declType,
            IdentifierNode identifier,
            node.expression.literal.DataType declaredType,
            TokenStream stream,
            Parser<ExpressionNode> expressionParser);
}
