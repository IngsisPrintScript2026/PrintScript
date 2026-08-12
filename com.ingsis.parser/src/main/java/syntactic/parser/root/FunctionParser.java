package syntactic.parser.root;

import iterator.IterationStep;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.factory.NodeFactory;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.util.ArgumentsParserUtils;
import token.SymbolType;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

import java.util.List;

public class FunctionParser implements Parser<CallFunctionNode> {
    private final Parser<IdentifierNode> identifierParser;
    private final Parser<ExpressionNode> expressionParser;

    public FunctionParser(Parser<IdentifierNode> identifierParser, Parser<ExpressionNode> expressionParser) {
        this.identifierParser = identifierParser;
        this.expressionParser = expressionParser;
    }

    @Override
    public Result<IterationStep<CallFunctionNode>> parse(TokenStream stream) {
        Result<IterationStep<IdentifierNode>> idResult = identifierParser.parse(stream);
        if (!idResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<IdentifierNode>>) idResult).error());
        }

        IterationStep<IdentifierNode> idStep = ((CorrectResult<IterationStep<IdentifierNode>>) idResult).value();
        return parseArguments(idStep.value(), (TokenStream) idStep.next());
    }

    private Result<IterationStep<CallFunctionNode>> parseArguments(IdentifierNode idNode, TokenStream stream) {
        Result<IterationStep<List<ExpressionNode>>> argsResult = ArgumentsParserUtils.parseSeparatedList(
                stream, expressionParser, SymbolType.LPAREN, SymbolType.RPAREN, SymbolType.COMMA);

        if (!argsResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<List<ExpressionNode>>>) argsResult).error());
        }

        IterationStep<List<ExpressionNode>> argsStep = ((CorrectResult<IterationStep<List<ExpressionNode>>>) argsResult).value();
        return createCallNode(idNode, argsStep.value(), (TokenStream) argsStep.next());
    }

    private Result<IterationStep<CallFunctionNode>> createCallNode(
            IdentifierNode idNode, List<ExpressionNode> args, TokenStream nextStream) {

        Token token = new Token(
                TokenType.IDENTIFIER,
                idNode.name(),
                new Position(idNode.line(), idNode.column())
        );

        CallFunctionNode callNode = NodeFactory.createCall(idNode.name(), args, token);
        return Result.success(new IterationStep<>(callNode, nextStream));
    }
}