/*
 * My Project
 */

package sca.handler;

import java.util.ArrayList;
import java.util.List;
import node.ProgramNode;
import node.StatementNode;
import sca.ASTSca;
import sca.ScaContext;
import semantic.environment.SemanticEnvironment;

public class ProgramScaHandler implements ScaNodeHandler<ProgramNode> {
    @Override
    public Class<ProgramNode> nodeType() {
        return ProgramNode.class;
    }

    @Override
    public List<String> check(
            ProgramNode program, SemanticEnvironment env, ScaContext context, ASTSca sca) {
        List<String> violations = new ArrayList<>();
        for (StatementNode stmt : program.statements()) {
            violations.addAll(sca.analyzeStatement(stmt, env, context));
        }
        return violations;
    }
}
