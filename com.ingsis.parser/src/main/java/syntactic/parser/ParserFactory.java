package syntactic.parser;

import iterator.IterationStep;
import node.Node;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import result.CorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.parser.literal.BooleanLiteralParser;
import syntactic.parser.literal.IdentifierParser;
import syntactic.parser.literal.NumberLiteralParser;
import syntactic.parser.literal.StringLiteralParser;
import syntactic.parser.operator.OperatorParser;
import syntactic.parser.root.ConditionalParser;
import syntactic.parser.root.DeclarationParser;
import syntactic.parser.root.FunctionParser;
import syntactic.parser.root.LineExpressionParser;
import version.printscript.PrintScriptVersion;

import java.util.concurrent.atomic.AtomicReference;

public final class ParserFactory {
    private ParserFactory() {}

    public static Parser<Node> createParser(PrintScriptVersion version) {
        Parser<IdentifierNode> identifierParser = new IdentifierParser();
        Parser<NumberLiteralNode> numberLiteralParser = new NumberLiteralParser();
        Parser<StringLiteralNode> stringLiteralParser = new StringLiteralParser();
        Parser<BooleanLiteralNode> booleanLiteralParser = new BooleanLiteralParser();

        AtomicReference<Parser<ExpressionNode>> exprParserRef = new AtomicReference<>();

        Parser<ExpressionNode> primaryParser = stream -> {
            Result<IterationStep<NumberLiteralNode>> numRes = numberLiteralParser.parse(stream);
            if (numRes.isCorrect()) {
                IterationStep<NumberLiteralNode> step = ((CorrectResult<IterationStep<NumberLiteralNode>>) numRes).value();
                return Result.success(new IterationStep<>(step.value(), step.next()));
            }

            Result<IterationStep<StringLiteralNode>> strRes = stringLiteralParser.parse(stream);
            if (strRes.isCorrect()) {
                IterationStep<StringLiteralNode> step = ((CorrectResult<IterationStep<StringLiteralNode>>) strRes).value();
                return Result.success(new IterationStep<>(step.value(), step.next()));
            }

            Result<IterationStep<BooleanLiteralNode>> boolRes = booleanLiteralParser.parse(stream);
            if (boolRes.isCorrect()) {
                IterationStep<BooleanLiteralNode> step = ((CorrectResult<IterationStep<BooleanLiteralNode>>) boolRes).value();
                return Result.success(new IterationStep<>(step.value(), step.next()));
            }

            FunctionParser functionParser = new FunctionParser(identifierParser, exprParserRef.get());
            Result<IterationStep<node.expression.function.CallFunctionNode>> fnRes = functionParser.parse(stream);
            if (fnRes.isCorrect()) {
                IterationStep<node.expression.function.CallFunctionNode> step = ((CorrectResult<IterationStep<node.expression.function.CallFunctionNode>>) fnRes).value();
                return Result.success(new IterationStep<>(step.value(), step.next()));
            }

            Result<IterationStep<IdentifierNode>> idRes = identifierParser.parse(stream);
            if (idRes.isCorrect()) {
                IterationStep<IdentifierNode> step = ((CorrectResult<IterationStep<IdentifierNode>>) idRes).value();
                return Result.success(new IterationStep<>(step.value(), step.next()));
            }

            return Result.failure("Unrecognized primary expression token stream");
        };

        Parser<ExpressionNode> prattParser = new OperatorParser(() -> primaryParser);
        exprParserRef.set(prattParser);

        DeclarationParser declarationParser = new DeclarationParser(identifierParser, prattParser);
        LineExpressionParser lineExprParser = new LineExpressionParser(prattParser);

        AtomicReference<Parser<Node>> stmtParserRef = new AtomicReference<>();
        ConditionalParser conditionalParser = new ConditionalParser(prattParser, stream -> stmtParserRef.get().parse(stream));

        Parser<Node> statementParser = stream -> {
            Result<IterationStep<node.keyword.DeclarationKeywordNode>> declRes = declarationParser.parse(stream);
            if (declRes.isCorrect()) {
                IterationStep<node.keyword.DeclarationKeywordNode> step = ((CorrectResult<IterationStep<node.keyword.DeclarationKeywordNode>>) declRes).value();
                return Result.success(new IterationStep<>(step.value(), step.next()));
            }

            Result<IterationStep<node.keyword.IfKeywordNode>> ifRes = conditionalParser.parse(stream);
            if (ifRes.isCorrect()) {
                IterationStep<node.keyword.IfKeywordNode> step = ((CorrectResult<IterationStep<node.keyword.IfKeywordNode>>) ifRes).value();
                return Result.success(new IterationStep<>(step.value(), step.next()));
            }

            Result<IterationStep<ExpressionNode>> exprRes = lineExprParser.parse(stream);
            if (exprRes.isCorrect()) {
                IterationStep<ExpressionNode> step = ((CorrectResult<IterationStep<ExpressionNode>>) exprRes).value();
                return Result.success(new IterationStep<>(step.value(), step.next()));
            }

            return Result.failure("Failed to parse statement at token stream");
        };

        stmtParserRef.set(statementParser);
        return statementParser;
    }
}
