package sca.handler;

import node.expression.Identifier.IdentifierNode;
import node.keyword.DeclarationKeywordNode;
import sca.ASTSca;
import sca.ScaContext;
import semantic.environment.SemanticEnvironment;

import java.util.ArrayList;
import java.util.List;

public class DeclarationScaHandler implements ScaNodeHandler<DeclarationKeywordNode> {
    @Override
    public Class<DeclarationKeywordNode> nodeType() {
        return DeclarationKeywordNode.class;
    }

    @Override
    public List<String> check(DeclarationKeywordNode decl, SemanticEnvironment env, ScaContext context, ASTSca sca) {
        List<String> violations = new ArrayList<>();
        if (context.identifierFormat() != null && !context.identifierFormat().trim().isEmpty()) {
            IdentifierNode idNode = decl.identifierNode();
            String name = idNode.name();
            String format = context.identifierFormat().trim().toLowerCase();

            boolean matches = true;
            if ("camel case".equals(format) || "camelcase".equals(format)) {
                matches = name.matches("^[a-z]+(?:[A-Z][a-z0-9]*)*$");
            } else if ("snake case".equals(format) || "snake_case".equals(format)) {
                matches = name.matches("^[a-z]+(?:_[a-z0-9]+)*$");
            }

            if (!matches) {
                violations.add(String.format(
                        "Identifier '%s' does not respect %s naming convention at line %d, column %d",
                        name, context.identifierFormat(), idNode.line(), idNode.column()));
            }
        }
        return violations;
    }
}
