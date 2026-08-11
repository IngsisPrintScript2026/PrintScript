import tokenstream.TokenStream;

public sealed interface Parser {
    Result<ASTNode> parse(TokenStream stream);
}
