/*
 * My Project
 */

package position;

public record Position(Integer line, Integer column) {
    public String describe() {
        return "line " + line + ", column " + column;
    }

    @Override
    public String toString() {
        return "[" + line + ":" + column + "]";
    }
}
