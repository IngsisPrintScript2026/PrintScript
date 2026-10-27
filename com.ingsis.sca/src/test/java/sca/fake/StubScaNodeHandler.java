/*
 * My Project
 */

package sca.fake;

import java.util.ArrayList;
import java.util.List;
import node.Node;
import sca.ASTSca;
import sca.ScaContext;
import sca.handler.ScaNodeHandler;
import semantic.environment.SemanticEnvironment;

/** Handler stub that answers a fixed list of violations and records the contexts it received. */
public final class StubScaNodeHandler<T extends Node> implements ScaNodeHandler<T> {
    private final Class<T> type;
    private final List<String> answer;
    private final List<ScaContext> receivedContexts = new ArrayList<>();

    public StubScaNodeHandler(Class<T> type, List<String> answer) {
        this.type = type;
        this.answer = List.copyOf(answer);
    }

    @Override
    public Class<T> nodeType() {
        return type;
    }

    @Override
    public List<String> check(T node, SemanticEnvironment env, ScaContext context, ASTSca sca) {
        receivedContexts.add(context);
        return new ArrayList<>(answer);
    }

    public List<ScaContext> receivedContexts() {
        return List.copyOf(receivedContexts);
    }
}
