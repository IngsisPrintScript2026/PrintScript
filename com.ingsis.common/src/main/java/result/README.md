# Paquete: result

Implementa el patrón de diseño **Mónada de Resultado (`Result<T>`)** para eliminar el retorno de `null`, las excepciones en runtime y el patrón Null Object.

## 📄 Clases y Métodos

### `Result<T>` (Sealed Interface)
* `boolean isCorrect()`: Retorna `true` si el resultado es exitoso (`CorrectResult`) o `false` si es un error (`IncorrectResult`).

### `CorrectResult<T>` (Record)
* `T value()`: Contiene el valor de retorno exitoso.

### `IncorrectResult<T>` (Record)
* `String error()`: Contiene la razón o mensaje de error descriptivo del fallo.