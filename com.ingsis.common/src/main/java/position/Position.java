package position;

public record Position(Integer line, Integer column) {
    @Override
    public String toString() {
        return "[" + line + ":" + column + "]";
    }

}
