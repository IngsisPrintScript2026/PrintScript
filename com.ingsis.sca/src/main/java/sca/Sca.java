package sca;

import node.Node;
import node.ProgramNode;
import result.Result;
import semantic.environment.SemanticEnvironment;

import java.util.List;

public interface Sca {
    Result<List<String>> analyze(ProgramNode program, SemanticEnvironment env);
    Result<List<String>> analyzeNode(Node node, SemanticEnvironment env);
}
