package syntactic.strategy;

import iterator.IterationStep;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.nullObject.NilExpressionNode;
import node.factory.NodeFactory;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;
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
            Token keywordToken,
            DeclarationType declType,
            IdentifierNode identifier,
            TokenStream stream,
            Parser<ExpressionNode> expressionParser) {

        Result<IterationStep<Token>> semiResult = stream.consume(TokenType.SEMICOLON);
        if (!semiResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) semiResult).error());
        }

        IterationStep<Token> semiStep = ((CorrectResult<IterationStep<Token>>) semiResult).value();
        DeclarationKeywordNode node = NodeFactory.createDeclaration(
                declType, identifier, new NilExpressionNode(), keywordToken);

        return Result.success(new IterationStep<>(node, (TokenStream) semiStep.next()));
    }
}
