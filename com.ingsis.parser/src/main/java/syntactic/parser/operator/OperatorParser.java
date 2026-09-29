/*
 * My Project
 */

package syntactic.parser.operator;

import iterator.IterationStep;
import java.util.Optional;
import java.util.function.Supplier;
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

public final class OperatorParser implements Parser<ExpressionNode> {
    private final Supplier<Parser<ExpressionNode>> primaryParserSupplier;

    public OperatorParser(Supplier<Parser<ExpressionNode>> primaryParserSupplier) {
        this.primaryParserSupplier = primaryParserSupplier;
    }

    @Override
    public Result<IterationStep<ExpressionNode>> parse(TokenStream stream) {
        int baseCollector = 0;
        return parseExpression(stream, baseCollector);
    }

    private Result<IterationStep<ExpressionNode>> parseExpression(
            TokenStream stream, int rightBindingPower) {
        return switch (primaryParserSupplier.get().parse(stream)) {
            case CorrectResult<IterationStep<ExpressionNode>>(
                            IterationStep<ExpressionNode> leftStep) ->
                    parseTail(leftStep.value(), (TokenStream) leftStep.next(), rightBindingPower);
            case IncorrectResult<IterationStep<ExpressionNode>>(String err) -> Result.failure(err);
        };
    }

    private Result<IterationStep<ExpressionNode>> parseTail(
            ExpressionNode left, TokenStream stream, int rightBindingPower) {
        Optional<OperatorType> opOpt = getValidOperator(stream, rightBindingPower);
        if (opOpt.isEmpty()) {
            return Result.success(new IterationStep<>(left, stream));
        }
        return parseRightAndContinue(left, stream, opOpt.get(), rightBindingPower);
    }

    private Optional<OperatorType> getValidOperator(TokenStream stream, int rightBindingPower) {
        if (stream.isEmpty()) {
            return Optional.empty();
        }
        Result<Token> peek = stream.peek(0);
        if (!peek.isCorrect()) {
            return Optional.empty();
        }
        Result<OperatorType> opRes =
                OperatorType.fromSymbol(((CorrectResult<Token>) peek).value().value());
        if (!opRes.isCorrect()) {
            return Optional.empty();
        }
        OperatorType op = ((CorrectResult<OperatorType>) opRes).value();
        return (op.lBindingPower() > rightBindingPower) ? Optional.of(op) : Optional.empty();
    }

    private Result<IterationStep<ExpressionNode>> parseRightAndContinue(
            ExpressionNode left, TokenStream stream, OperatorType opType, int rightBindingPower) {
        return switch (stream.consume()) {
            case CorrectResult<IterationStep<Token>>(IterationStep<Token> opStep) -> {
                Token opToken = opStep.value();
                TokenStream nextStream = (TokenStream) opStep.next();
                yield switch (parseExpression(nextStream, opType.rBindingPower())) {
                    case CorrectResult<IterationStep<ExpressionNode>>(
                            IterationStep<ExpressionNode> rightStep) -> {
                        OperatorNode opNode =
                                NodeFactory.createOperator(
                                        opType, left, rightStep.value(), opToken);
                        yield parseTail(opNode, (TokenStream) rightStep.next(), rightBindingPower);
                    }
                    case IncorrectResult<IterationStep<ExpressionNode>>(String err) ->
                            Result.failure(err);
                };
            }
            case IncorrectResult<IterationStep<Token>>(String err) -> Result.failure(err);
        };
    }
}
