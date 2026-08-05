package result;

public record IncorrectResult<T>(String error) implements Result<T> {
    @Override
    public boolean isCorrect() {
        return false;
    }
}