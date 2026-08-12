package syntactic.parser.root;

import iterator.IterationStep;
import node.expression.ExpressionNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

public class LineExpressionParser implements Parser<ExpressionNode> {
    private final Parser<ExpressionNode> expressionParser;

    public LineExpressionParser(Parser<ExpressionNode> expressionParser) {
        this.expressionParser = expressionParser;
    }

    @Override
    public Result<IterationStep<ExpressionNode>> parse(TokenStream stream) {
        return switch (expressionParser.parse(stream)) {
            case CorrectResult<IterationStep<ExpressionNode>>(IterationStep<ExpressionNode> exprStep) -> {
                TokenStream nextStream = (TokenStream) exprStep.next();
                yield switch (nextStream.consume(TokenType.SEMICOLON)) {
                    case CorrectResult<IterationStep<Token>>(IterationStep<Token> semiStep) ->
                            Result.success(new IterationStep<>(exprStep.value(), (TokenStream) semiStep.next()));
                    case IncorrectResult<IterationStep<Token>>(String err) -> Result.failure(err);
                };
            }
            case IncorrectResult<IterationStep<ExpressionNode>>(String err) -> Result.failure(err);
        };
    }
}