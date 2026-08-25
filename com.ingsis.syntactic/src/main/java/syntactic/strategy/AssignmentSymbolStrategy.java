package syntactic.strategy;

import iterator.IterationStep;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
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

public class AssignmentSymbolStrategy implements DeclarationSymbolStrategy {
    @Override
    public SymbolType targetSymbol() {
        return SymbolType.EQUAL;
    }

    @Override
    public Result<IterationStep<DeclarationKeywordNode>> parse(
            Token keywordToken,
            DeclarationType declType,
            IdentifierNode identifier,
            TokenStream stream,
            Parser<ExpressionNode> expressionParser) {

        Result<IterationStep<Token>> equalResult = stream.consume(TokenType.EQUAL);
        if (!equalResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) equalResult).error());
        }

        IterationStep<Token> equalStep = ((CorrectResult<IterationStep<Token>>) equalResult).value();
        TokenStream postEqualStream = (TokenStream) equalStep.next();

        Result<IterationStep<ExpressionNode>> exprResult = expressionParser.parse(postEqualStream);
        if (!exprResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<ExpressionNode>>) exprResult).error());
        }

        IterationStep<ExpressionNode> exprStep = ((CorrectResult<IterationStep<ExpressionNode>>) exprResult).value();
        TokenStream postExprStream = (TokenStream) exprStep.next();

        Result<IterationStep<Token>> semiResult = postExprStream.consume(TokenType.SEMICOLON);
        if (!semiResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) semiResult).error());
        }

        IterationStep<Token> semiStep = ((CorrectResult<IterationStep<Token>>) semiResult).value();
        DeclarationKeywordNode node = NodeFactory.createDeclaration(
                declType, identifier, exprStep.value(), keywordToken);

        return Result.success(new IterationStep<>(node, (TokenStream) semiStep.next()));
    }
}
