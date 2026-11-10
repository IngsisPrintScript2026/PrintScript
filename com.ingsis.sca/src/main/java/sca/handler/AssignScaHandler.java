/*
 * My Project
 */

package sca.handler;

import java.util.List;
import node.keyword.AssignNode;
import sca.ASTSca;
import sca.ScaContext;
import semantic.environment.SemanticEnvironment;

public class AssignScaHandler implements ScaNodeHandler<AssignNode> {
    @Override
    public Class<AssignNode> nodeType() {
        return AssignNode.class;
    }

    @Override
    public List<String> check(
            AssignNode assign, SemanticEnvironment env, ScaContext context, ASTSca sca) {
        return sca.analyzeStatement(assign.expressionNode(), env, context);
    }
}
