/*
 * My Project
 */

package sca.handler;

import java.util.ArrayList;
import java.util.List;
import node.Node;
import node.keyword.WhileKeywordNode;
import sca.ASTSca;
import sca.ScaContext;
import semantic.environment.SemanticEnvironment;

public class WhileScaHandler implements ScaNodeHandler<WhileKeywordNode> {
    @Override
    public Class<WhileKeywordNode> nodeType() {
        return WhileKeywordNode.class;
    }

    @Override
    public List<String> check(
            WhileKeywordNode whileNode, SemanticEnvironment env, ScaContext context, ASTSca sca) {
        List<String> violations = new ArrayList<>();
        for (Node stmt : whileNode.body()) {
            violations.addAll(sca.analyzeStatement(stmt, env, context));
        }
        return violations;
    }
}
