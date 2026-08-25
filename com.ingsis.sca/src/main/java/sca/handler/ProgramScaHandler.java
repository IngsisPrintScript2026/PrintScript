package sca.handler;

import node.Node;
import node.ProgramNode;
import sca.ASTSca;
import sca.ScaContext;
import semantic.environment.SemanticEnvironment;

import java.util.ArrayList;
import java.util.List;

public class ProgramScaHandler implements ScaNodeHandler<ProgramNode> {
    @Override
    public Class<ProgramNode> nodeType() {
        return ProgramNode.class;
    }

    @Override
    public List<String> check(ProgramNode program, SemanticEnvironment env, ScaContext context, ASTSca sca) {
        List<String> violations = new ArrayList<>();
        for (Node stmt : program.statements()) {
            violations.addAll(sca.analyzeStatement(stmt, env, context));
        }
        return violations;
    }
}
