# Paquete: lexer

Contiene el orquestador principal del análisis léxico.

## 📄 Clases y Métodos

### `PrintScriptTokenizer` (Clase)
* Implementa `Tokenizer`.
* `TokenizeResult tokenize(MetaCharStringBuilder sb)`: Procesa el acumulador de caracteres.
* `evaluateMatch()`: Traduce una coincidencia a `TokenizeResult.Complete`.
* `evaluateFailure()`: Clasifica un fallo como `Prefix` o `Invalid`.
* `isPrefix()`, `isUnfinishedString()`: Lógica para identificar prefijos de strings o números decimales incompletos.

### `Lexer` (Clase Principal)
* Implementa `SafeIterator<Token>`.
* `Result<IterationStep<Token>> next()`: Punto de entrada para obtener el siguiente token de forma perezosa.
* `maximalMunchOf()`: Ignora espacios iniciales e inicia el bucle de Maximal Munch.
* `runMaximalMunchLoop()`: Ejecuta el bucle de acumulación y devuelve caracteres sobrantes.
* `executeMunch()`: Lee caracteres uno a uno mientras la tokenización no sea `Invalid`.
* `processTokenizeStep()`: Guarda la coincidencia válida más reciente (`lastValidToken`).
* `unreadLookaheadBuffer()`: Retorna al stream los caracteres de lookahead excedentes.
* `buildResult()`: Construye el `Result<IterationStep<Token>>` final.
