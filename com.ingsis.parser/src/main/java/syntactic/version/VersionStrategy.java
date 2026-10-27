/*
 * My Project
 */

package syntactic.version;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import node.Node;
import node.expression.ExpressionNode;
import syntactic.Parser;
import syntactic.parser.root.AssignParser;
import syntactic.parser.root.ConditionalParser;
import syntactic.parser.root.DeclarationParser;
import syntactic.parser.root.LineExpressionParser;
import syntactic.parser.root.StatementParser;
import token.Token;
import version.Version;

public interface VersionStrategy {
    Version version();

    Predicate<Token> declarationKeywords();

    Predicate<Token> supportedDataTypes();

    Set<String> builtInFunctions();

    List<Parser<? extends ExpressionNode>> primaryParsers(
            LiteralParsers literalParsers,
            Parser<? extends ExpressionNode> functionParser,
            Parser<node.expression.Identifier.IdentifierNode> identifierParser);

    List<StatementParser<? extends Node>> statementParsers(
            DeclarationParser declarationParser,
            AssignParser assignParser,
            ConditionalParser conditionalParser,
            LineExpressionParser lineExprParser);
}
