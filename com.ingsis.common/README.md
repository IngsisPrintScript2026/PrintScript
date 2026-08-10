# Módulo: com.ingsis.common

Este módulo contiene los componentes básicos y compartidos del compilador/intérprete **PrintScript**.

## 📁 Estructura de Paquetes

* **`iterator`**: Abstracciones para iteración perezosa y streaming (`SafeIterator`, `IterationStep`).
* **`result`**: Sistema monádico de resultados (`Result`, `CorrectResult`, `IncorrectResult`) sin excepciones en runtime ni Null Object.
* **`position`**: Modelado inmutable de coordenadas de archivo `(line, column)`.
* **`metaChar`**: Wrappers para caracteres con posición (`MetaCharacter`) y acumulador mutable de tokens (`MetaCharStringBuilder`).
* **`token`**: Representación de tokens (`Token`, `TokenType`), interfaces de tokenización (`Tokenizer`, `TokenizeResult`) y la **Cadena de Responsabilidad** para clasificar tokens (`TokenTypeMatcher`, `AbstractTokenTypeMatcher`).
