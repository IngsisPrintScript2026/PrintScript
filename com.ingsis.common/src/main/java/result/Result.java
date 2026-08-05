package result;

public sealed interface Result<T> permits CorrectResult, IncorrectResult {
    boolean isCorrect();

    static <T> Result<T> success(T value) {
        return new CorrectResult<>(value);
    }

    static <T> Result<T> failure(String error) {
        return new IncorrectResult<>(error);
    }
}