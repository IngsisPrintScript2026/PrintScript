package formatter;

import node.Node;
import node.ProgramNode;
import result.Result;

public interface Formatter {
    Result<String> format(ProgramNode program);
    Result<String> formatNode(Node node);
}
