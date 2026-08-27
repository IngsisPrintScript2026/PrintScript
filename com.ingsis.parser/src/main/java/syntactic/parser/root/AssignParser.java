/*
 * My Project
 */

package syntactic.parser.root;

import iterator.IterationStep;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.keyword.AssignNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

public class AssignParser implements Parser<AssignNode> {
    private final Parser<IdentifierNode> identifierParser;
    private final Parser<ExpressionNode> expressionParser;

    public AssignParser(
            Parser<IdentifierNode> identifierParser, Parser<ExpressionNode> expressionParser) {
        this.identifierParser = identifierParser;
        this.expressionParser = expressionParser;
    }

    @Override
    public Result<IterationStep<AssignNode>> parse(TokenStream stream) {
        Result<IterationStep<IdentifierNode>> idRes = identifierParser.parse(stream);
        if (!idRes.isCorrect()) {
            return Result.failure(((IncorrectResult<?>) idRes).error());
        }

        IterationStep<IdentifierNode> idStep =
                ((CorrectResult<IterationStep<IdentifierNode>>) idRes).value();
        TokenStream postIdStream = (TokenStream) idStep.next();

        Result<IterationStep<Token>> equalRes = postIdStream.consume(TokenType.EQUAL);
        if (!equalRes.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) equalRes).error());
        }

        IterationStep<Token> equalStep = ((CorrectResult<IterationStep<Token>>) equalRes).value();
        TokenStream postEqualStream = (TokenStream) equalStep.next();

        Result<IterationStep<ExpressionNode>> exprRes = expressionParser.parse(postEqualStream);
        if (!exprRes.isCorrect()) {
            return Result.failure(
                    ((IncorrectResult<IterationStep<ExpressionNode>>) exprRes).error());
        }

        IterationStep<ExpressionNode> exprStep =
                ((CorrectResult<IterationStep<ExpressionNode>>) exprRes).value();
        TokenStream postExprStream = (TokenStream) exprStep.next();

        Result<IterationStep<Token>> semiRes = postExprStream.consume(TokenType.SEMICOLON);
        if (!semiRes.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) semiRes).error());
        }

        IterationStep<Token> semiStep = ((CorrectResult<IterationStep<Token>>) semiRes).value();
        IdentifierNode idNode = idStep.value();

        AssignNode assignNode =
                new AssignNode(idNode, exprStep.value(), idNode.line(), idNode.column());

        return Result.success(new IterationStep<>(assignNode, (TokenStream) semiStep.next()));
    }
}
