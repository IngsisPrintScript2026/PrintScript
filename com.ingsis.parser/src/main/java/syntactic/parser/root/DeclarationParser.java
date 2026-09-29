/*
 * My Project
 */

package syntactic.parser.root;

import iterator.IterationStep;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.DataType;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.strategy.AssignmentSymbolStrategy;
import syntactic.strategy.DeclarationSymbolStrategy;
import syntactic.strategy.EmptyDeclarationSymbolStrategy;
import token.SymbolType;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

public class DeclarationParser implements Parser<DeclarationKeywordNode> {
    private final Parser<IdentifierNode> identifierParser;
    private final Parser<ExpressionNode> expressionParser;
    private final Predicate<Token> declarationKeywordMatcher;
    private final Predicate<Token> literalMatcher;
    private final Map<SymbolType, DeclarationSymbolStrategy> symbolStrategies;

    public DeclarationParser(
            Parser<IdentifierNode> identifierParser,
            Parser<ExpressionNode> expressionParser,
            Predicate<Token> declarationKeywordMatcher,
            Predicate<Token> literalMatcher) {
        this.identifierParser = identifierParser;
        this.expressionParser = expressionParser;
        this.declarationKeywordMatcher = declarationKeywordMatcher;
        this.literalMatcher = literalMatcher;
        this.symbolStrategies =
                java.util.stream.Stream.of(
                                new AssignmentSymbolStrategy(),
                                new EmptyDeclarationSymbolStrategy())
                        .collect(Collectors.toMap(DeclarationSymbolStrategy::targetSymbol, s -> s));
    }

    public DeclarationParser(
            Parser<IdentifierNode> identifierParser, Parser<ExpressionNode> expressionParser) {
        this(identifierParser, expressionParser, DeclarationType::exists, DataType::exists);
    }

    @Override
    public Result<IterationStep<DeclarationKeywordNode>> parse(TokenStream stream) {
        Result<IterationStep<Token>> declResult = stream.consume(declarationKeywordMatcher);
        if (!declResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) declResult).error());
        }
        IterationStep<Token> step = ((CorrectResult<IterationStep<Token>>) declResult).value();
        Token keywordToken = step.value();
        DeclarationType declType = DeclarationType.fromToken(keywordToken).orElseThrow();
        return parseIdentifier(keywordToken, declType, (TokenStream) step.next());
    }

    private Result<IterationStep<DeclarationKeywordNode>> parseIdentifier(
            Token keywordToken, DeclarationType declType, TokenStream stream) {
        return switch (identifierParser.parse(stream)) {
            case CorrectResult<IterationStep<IdentifierNode>>(
                            IterationStep<IdentifierNode> idStep) ->
                    parseColon(keywordToken, declType, idStep.value(), (TokenStream) idStep.next());
            case IncorrectResult<IterationStep<IdentifierNode>>(String err) -> Result.failure(err);
        };
    }

    private Result<IterationStep<DeclarationKeywordNode>> parseColon(
            Token keywordToken,
            DeclarationType declType,
            IdentifierNode identifier,
            TokenStream stream) {
        return switch (stream.consume(TokenType.COLON)) {
            case CorrectResult<IterationStep<Token>>(IterationStep<Token> colonStep) ->
                    parseType(keywordToken, declType, identifier, (TokenStream) colonStep.next());
            case IncorrectResult<IterationStep<Token>>(String err) -> Result.failure(err);
        };
    }

    private Result<IterationStep<DeclarationKeywordNode>> parseType(
            Token keywordToken,
            DeclarationType declType,
            IdentifierNode identifier,
            TokenStream stream) {
        Result<IterationStep<Token>> typeResult = stream.consume(literalMatcher);

        return switch (typeResult) {
            case CorrectResult<IterationStep<Token>>(IterationStep<Token> typeStep) -> {
                DataType declaredType =
                        DataType.fromTokenType(typeStep.value().type())
                                .or(() -> DataType.fromKeyword(typeStep.value().value()))
                                .orElse(null);
                node.keyword.declaration.DeclarationInfo info =
                        new node.keyword.declaration.DeclarationInfo(
                                declType, declaredType, keywordToken);
                yield parseStrategy(info, identifier, (TokenStream) typeStep.next());
            }
            case IncorrectResult<IterationStep<Token>>(String err) -> Result.failure(err);
        };
    }

    private Result<IterationStep<DeclarationKeywordNode>> parseStrategy(
            node.keyword.declaration.DeclarationInfo info,
            IdentifierNode identifier,
            TokenStream stream) {
        Result<Token> peekResult = stream.peek(0);
        if (!peekResult.isCorrect()) {
            return Result.failure("Fin de archivo inesperado al parsear declaración");
        }
        Token peekToken = ((CorrectResult<Token>) peekResult).value();
        Optional<SymbolType> symbolOpt = SymbolType.fromToken(peekToken);
        if (symbolOpt.isPresent() && symbolStrategies.containsKey(symbolOpt.get())) {
            DeclarationSymbolStrategy strategy = symbolStrategies.get(symbolOpt.get());
            return strategy.parse(info, identifier, stream, expressionParser);
        }
        return Result.failure("Símbolo inesperado en la declaración: " + peekToken.value());
    }
}
