/*
 * My Project
 */

import node.Node;
import result.Result;
import tokenstream.TokenStream;

public interface Parser {
    Result<Node> parse(TokenStream stream);
}
