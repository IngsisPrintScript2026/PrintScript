/*
 * My Project
 */

package position;

public record SourceRange(Position start, Position end) {
    public static SourceRange at(Position position) {
        return new SourceRange(position, position);
    }

    @Override
    public String toString() {
        if (start == null || end == null) {
            return "at an unknown position";
        }
        return "from " + start.describe() + " to " + end.describe();
    }
}
