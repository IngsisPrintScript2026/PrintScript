/*
 * My Project
 */

package sca.handler;

import java.util.List;
import node.keyword.AssignNode;
import sca.ASTSca;
import sca.ScaContext;
import semantic.environment.SemanticEnvironment;

/** Analyzes the assigned expression, e.g. a readInput call in {@code x = readInput(...);}. */
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
