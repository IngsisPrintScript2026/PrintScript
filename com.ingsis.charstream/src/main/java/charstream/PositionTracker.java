/*
 * My Project
 */

package charstream;

public final class PositionTracker {
    private final int line;
    private final int column;
    private final char lastChar;

    public PositionTracker() {
        this(1, 1, '\0');
    }

    private PositionTracker(int line, int column, char lastChar) {
        this.line = line;
        this.column = column;
        this.lastChar = lastChar;
    }

    public PositionTracker advance(char currentChar) {
        if (currentChar == '\n') {
            if (lastChar == '\r') {
                return new PositionTracker(line, 1, currentChar);
            }
            return new PositionTracker(line + 1, 1, currentChar);
        } else if (currentChar == '\r') {
            return new PositionTracker(line + 1, 1, currentChar);
        } else {
            return new PositionTracker(line, column + 1, currentChar);
        }
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
