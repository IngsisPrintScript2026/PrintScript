/*
 * My Project
 */

package sca;

import java.util.List;
import node.Node;
import node.ProgramNode;
import result.Result;
import semantic.environment.SemanticEnvironment;

public interface Sca {
    Result<List<String>> analyze(ProgramNode program, SemanticEnvironment env);

    Result<List<String>> analyzeNode(Node node, SemanticEnvironment env);
}
