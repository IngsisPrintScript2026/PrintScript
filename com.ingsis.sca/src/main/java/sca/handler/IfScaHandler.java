/*
 * My Project
 */

package sca.handler;

import java.util.ArrayList;
import java.util.List;
import node.IfKeywordNode;
import node.StatementNode;
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
        for (StatementNode stmt : ifNode.thenBody()) {
            violations.addAll(sca.analyzeStatement(stmt, env, context));
        }
        for (StatementNode stmt : ifNode.elseBody()) {
            violations.addAll(sca.analyzeStatement(stmt, env, context));
        }
        return violations;
    }
}
