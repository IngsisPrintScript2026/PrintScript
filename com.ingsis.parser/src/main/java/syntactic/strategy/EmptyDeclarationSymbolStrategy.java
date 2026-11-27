/*
 * My Project
 */

package syntactic.strategy;

import iterator.IterationStep;
import node.DeclarationKeywordNode;
import node.ExpressionNode;
import node.IdentifierNode;
import node.NilExpressionNode;
import node.factory.NodeFactory;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import token.SymbolType;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

public class EmptyDeclarationSymbolStrategy implements DeclarationSymbolStrategy {
    @Override
    public SymbolType targetSymbol() {
        return SymbolType.SEMICOLON;
    }

    @Override
    public Result<IterationStep<DeclarationKeywordNode>> parse(
            node.keyword.declaration.DeclarationInfo info,
            IdentifierNode identifier,
            TokenStream stream,
            Parser<ExpressionNode> expressionParser) {

        Result<IterationStep<Token>> semiResult = stream.consume(TokenType.SEMICOLON);
        if (!semiResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) semiResult).error());
        }

        IterationStep<Token> semiStep = ((CorrectResult<IterationStep<Token>>) semiResult).value();
        DeclarationKeywordNode node =
                NodeFactory.createDeclaration(info, identifier, new NilExpressionNode());

        return Result.success(new IterationStep<>(node, (TokenStream) semiStep.next()));
    }
}
