# Paquete: token

Contiene la definición de tokens y la **Cadena de Responsabilidad (Chain of Responsibility)** para clasificarlos.

## 📄 Clases y Métodos

### `TokenType` (Enum)
Lista de palabras reservadas, tipos de datos, operadores y delimitadores.

### `Token` (Record)
Implementa `TokenInterface` encapsulando `type()`, `value()`, `startPosition()` y `endPosition()`.

### `AbstractTokenTypeMatcher` (Clase Abstracta)
* `linkWith(AbstractTokenTypeMatcher next)`: Enlaza el siguiente matcher en la cadena.
* `match(String input)`: Ejecuta la validación llamando a `doMatch` o a `passToNext`.
* `passToNext(String input, String reason)`: Reenvía la petición al siguiente eslabón.
* `doMatch(String input)`: Método abstracto que implementa cada matcher específico (`LexemeMatcher`, `NumberMatcher`, `BooleanMatcher`, `StringMatcher`, `IdentifierMatcher`).

### `ChainTokenTypeMatcher` (Clase)
* `static TokenTypeMatcher defaultChain()`: Método fábrica que retorna la cadena de coincidencia por defecto.

### `TokenMatcher` (Clase)
* `static Result<TokenType> match(String input)`: Facade estático para clasificar cualquier texto mediante la cadena.
