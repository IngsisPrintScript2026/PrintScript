package syntactic;

import iterator.IterationStep;
import node.Node;
import result.Result;
import tokenstream.TokenStream;

public interface Parser<T extends Node> {
    Result<IterationStep<T>> parse(TokenStream stream);
}

