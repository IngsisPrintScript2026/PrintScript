package syntactic.parser.operator;

import iterator.IterationStep;
import node.expression.ExpressionNode;
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import node.factory.NodeFactory;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import token.Token;
import tokenstream.TokenStream;

import java.util.function.Supplier;

public final class OperatorParser implements Parser<ExpressionNode> {
    private final Supplier<Parser<ExpressionNode>> primaryParserSupplier;

    public OperatorParser(Supplier<Parser<ExpressionNode>> primaryParserSupplier) {
        this.primaryParserSupplier = primaryParserSupplier;
    }

    @Override
    public Result<IterationStep<ExpressionNode>> parse(TokenStream stream) {
        return parseExpression(stream, 0);
    }

    private Result<IterationStep<ExpressionNode>> parseExpression(TokenStream stream, int rightBindingPower) {
        return switch (primaryParserSupplier.get().parse(stream)) {
            case CorrectResult<IterationStep<ExpressionNode>>(IterationStep<ExpressionNode> leftStep) ->
                    parseTail(leftStep.value(), (TokenStream) leftStep.next(), rightBindingPower);
            case IncorrectResult<IterationStep<ExpressionNode>>(String err) -> Result.failure(err);
        };
    }

    private Result<IterationStep<ExpressionNode>> parseTail(ExpressionNode left, TokenStream stream, int rightBindingPower) {
        if (stream.isEmpty()) {
            return Result.success(new IterationStep<>(left, stream));
        }

        Result<Token> peekResult = stream.peek(0);
        if (!peekResult.isCorrect()) {
            return Result.success(new IterationStep<>(left, stream));
        }

        Token currentToken = ((CorrectResult<Token>) peekResult).value();
        Result<OperatorType> opTypeResult = OperatorType.fromSymbol(currentToken.value());
        if (!opTypeResult.isCorrect()) {
            return Result.success(new IterationStep<>(left, stream));
        }

        OperatorType operatorType = ((CorrectResult<OperatorType>) opTypeResult).value();
        if (operatorType.lBindingPower() <= rightBindingPower) {
            return Result.success(new IterationStep<>(left, stream));
        }

        // Consume operator token
        return switch (stream.consume()) {
            case CorrectResult<IterationStep<Token>>(IterationStep<Token> opStep) -> {
                Token opToken = opStep.value();
                TokenStream nextStream = (TokenStream) opStep.next();

                yield switch (parseExpression(nextStream, operatorType.rBindingPower())) {
                    case CorrectResult<IterationStep<ExpressionNode>>(IterationStep<ExpressionNode> rightStep) -> {
                        OperatorNode opNode = NodeFactory.createOperator(
                                operatorType,
                                left,
                                rightStep.value(),
                                opToken
                        );
                        yield parseTail(opNode, (TokenStream) rightStep.next(), rightBindingPower);
                    }
                    case IncorrectResult<IterationStep<ExpressionNode>>(String err) -> Result.failure(err);
                };
            }
            case IncorrectResult<IterationStep<Token>>(String err) -> Result.failure(err);
        };
    }
}