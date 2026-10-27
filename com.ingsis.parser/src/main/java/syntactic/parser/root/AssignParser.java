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
import tokenstream.rules.TokenMatchers;

public class AssignParser implements StatementParser<AssignNode> {
    private final Parser<IdentifierNode> identifierParser;
    private final Parser<ExpressionNode> expressionParser;

    public AssignParser(
            Parser<IdentifierNode> identifierParser, Parser<ExpressionNode> expressionParser) {
        this.identifierParser = identifierParser;
        this.expressionParser = expressionParser;
    }

    @Override
    public boolean canStart(TokenStream stream) {
        return StatementParser.peekMatches(stream, 0, TokenMatchers.isType(TokenType.IDENTIFIER))
                && StatementParser.peekMatches(stream, 1, TokenMatchers.isType(TokenType.EQUAL));
    }

    @Override
    public Result<IterationStep<AssignNode>> parse(TokenStream stream) {
        Result<IterationStep<IdentifierNode>> idRes = identifierParser.parse(stream);
        if (!idRes.isCorrect()) {
            return Result.failure(((IncorrectResult<?>) idRes).error());
        }
        IterationStep<IdentifierNode> idStep =
                ((CorrectResult<IterationStep<IdentifierNode>>) idRes).value();
        return parseAssignmentBody(idStep.value(), (TokenStream) idStep.next());
    }

    private Result<IterationStep<AssignNode>> parseAssignmentBody(
            IdentifierNode idNode, TokenStream postIdStream) {
        Result<IterationStep<Token>> equalRes = postIdStream.consume(TokenType.EQUAL);
        if (!equalRes.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) equalRes).error());
        }
        TokenStream postEqual =
                (TokenStream) ((CorrectResult<IterationStep<Token>>) equalRes).value().next();
        return parseExprAndSemicolon(idNode, postEqual);
    }

    private Result<IterationStep<AssignNode>> parseExprAndSemicolon(
            IdentifierNode idNode, TokenStream postEqual) {
        Result<IterationStep<ExpressionNode>> exprRes = expressionParser.parse(postEqual);
        if (!exprRes.isCorrect()) {
            return Result.failure(
                    ((IncorrectResult<IterationStep<ExpressionNode>>) exprRes).error());
        }
        IterationStep<ExpressionNode> exprStep =
                ((CorrectResult<IterationStep<ExpressionNode>>) exprRes).value();
        Result<IterationStep<Token>> semiRes =
                ((TokenStream) exprStep.next()).consume(TokenType.SEMICOLON);
        if (!semiRes.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) semiRes).error());
        }
        AssignNode node = new AssignNode(idNode, exprStep.value(), idNode.line(), idNode.column());
        TokenStream postSemi =
                (TokenStream) ((CorrectResult<IterationStep<Token>>) semiRes).value().next();
        return Result.success(new IterationStep<>(node, postSemi));
    }
}
