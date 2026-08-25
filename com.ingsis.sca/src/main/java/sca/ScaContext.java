package sca;

public record ScaContext(
        String identifierFormat,
        boolean mandatoryLiteralOrIdentifierInPrintln,
        boolean mandatoryLiteralOrIdentifierInReadInput) {

    public ScaContext() {
        this(null, false, false);
    }
}
