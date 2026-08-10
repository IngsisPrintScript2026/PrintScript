# Paquete: metaChar

Contiene wrappers de caracteres asociando su valor y su `Position` exacta en el archivo fuente.

## 📄 Clases y Métodos

### `MetaCharacter` (Record)
* `Character character()`: Retorna el caracter leído.
* `Position position()`: Retorna la posición del caracter.

### `MetaCharStringBuilder` (Clase)
* `void append(MetaCharacter mc)`: Agrega un caracter al buffer y registra la posición del primer caracter añadido.
* `String buildString()`: Devuelve la cadena acumulada.
* `Position getStartPosition()`: Retorna la posición inicial donde comenzó la acumulación.
