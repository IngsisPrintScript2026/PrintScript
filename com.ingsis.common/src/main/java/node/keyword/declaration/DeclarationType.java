package node.keyword.declaration;

public enum DeclarationType {
    LET("let", true),
    CONST("const", false);

    private final String keyword;
    private final boolean isMutable;

    DeclarationType(String keyword, boolean isMutable) {
        this.keyword = keyword;
        this.isMutable = isMutable;
    }

    public String keyword() { return keyword; }
    public boolean isMutable() { return isMutable; }
}
