/*
 * My Project
 */

package sca.handler;

import java.util.ArrayList;
import java.util.List;
import node.Node;
import node.keyword.IfKeywordNode;
import sca.ASTSca;
import sca.ScaContext;
import semantic.environment.SemanticEnvironment;

public class IfScaHandler implements ScaNodeHandler<IfKeywordNode> {
    @Override
    public Class<IfKeywordNode> nodeType() {
        return IfKeywordNode.class;
    }

    @Override
    public List<String> check(
            IfKeywordNode ifNode, SemanticEnvironment env, ScaContext context, ASTSca sca) {
        List<String> violations = new ArrayList<>();
        for (Node stmt : ifNode.thenBody()) {
            violations.addAll(sca.analyzeStatement(stmt, env, context));
        }
        for (Node stmt : ifNode.elseBody()) {
            violations.addAll(sca.analyzeStatement(stmt, env, context));
        }
        return violations;
    }
}
