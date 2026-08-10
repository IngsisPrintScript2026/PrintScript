# Paquete: iterator

Define la API de **iteración perezosa (streaming)** para procesar fuentes de datos extensas que no caben en memoria.

## 📄 Clases y Métodos

### `SafeIterator<T>` (Interfaz)
* `Result<IterationStep<T>> next()`: Devuelve el resultado del siguiente paso de la iteración.
* `default void unread(T item)`: Permite devolver un elemento al buffer de lectura para operaciones de lookahead pushback sin romper la abstracción.

### `IterationStep<T>` (Record)
* `T value()`: Retorna el elemento parseado en el paso actual.
* `SafeIterator<T> next()`: Retorna el iterador avanzado al siguiente estado del stream.
