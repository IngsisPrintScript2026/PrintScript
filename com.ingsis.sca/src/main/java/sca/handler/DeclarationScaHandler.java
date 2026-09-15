/*
 * My Project
 */

package sca.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import node.expression.Identifier.IdentifierNode;
import node.keyword.DeclarationKeywordNode;
import sca.ASTSca;
import sca.ScaContext;
import semantic.environment.SemanticEnvironment;

public class DeclarationScaHandler implements ScaNodeHandler<DeclarationKeywordNode> {
    @Override
    public Class<DeclarationKeywordNode> nodeType() {
        return DeclarationKeywordNode.class;
    }

    @Override
    public List<String> check(
            DeclarationKeywordNode decl, SemanticEnvironment env, ScaContext context, ASTSca sca) {
        List<String> violations = new ArrayList<>();
        checkNamingConvention(decl.identifierNode(), context.identifierFormat(), violations);
        checkExpression(decl, env, context, sca, violations);
        return violations;
    }

    private void checkNamingConvention(
            IdentifierNode idNode, String format, List<String> violations) {
        if (format == null || format.isBlank()) {
            return;
        }
        if (!matchesConvention(idNode.name(), format.trim().toLowerCase(Locale.ROOT))) {
            violations.add(
                    String.format(
                            "Identifier '%s' does not respect %s naming convention at line %d,"
                                    + " column %d",
                            idNode.name(), format, idNode.line(), idNode.column()));
        }
    }

    private boolean matchesConvention(String name, String format) {
        return switch (format) {
            case "camel case", "camelcase" -> name.matches("^[a-z]+(?:[A-Z][a-z0-9]*)*$");
            case "snake case", "snake_case" -> name.matches("^[a-z]+(?:_[a-z0-9]+)*$");
            default -> true;
        };
    }

    private void checkExpression(
            DeclarationKeywordNode decl,
            SemanticEnvironment env,
            ScaContext context,
            ASTSca sca,
            List<String> violations) {
        if (decl.expressionNode() != null) {
            violations.addAll(sca.analyzeStatement(decl.expressionNode(), env, context));
        }
    }
}
