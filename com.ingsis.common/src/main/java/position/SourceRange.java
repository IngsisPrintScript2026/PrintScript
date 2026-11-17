/*
 * My Project
 */

package position;

public record SourceRange(Position start, Position end) {
    public static SourceRange at(Position position) {
        return new SourceRange(position, position);
    }

    /**
     * Rango que ocupa {@code text} en el código fuente si empieza en {@code start}. El fin es la
     * posición del último carácter (inclusive) y contempla saltos de línea dentro del texto.
     */
    public static SourceRange spanning(Position start, String text) {
        if (start == null || start.line() == null || start.column() == null) {
            return new SourceRange(null, null);
        }
        if (text == null || text.isEmpty()) {
            return at(start);
        }
        int line = start.line();
        int column = start.column() - 1;
        for (int i = 0; i < text.length(); i++) {
            if (i > 0 && text.charAt(i - 1) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new SourceRange(start, new Position(line, column));
    }

    @Override
    public String toString() {
        if (start == null || end == null) {
            return "at an unknown position";
        }
        return "from " + start.describe() + " to " + end.describe();
    }
}
