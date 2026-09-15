# Ejemplos y Pruebas de PrintScript (`samples/`)

Este directorio contiene programas de muestra (`.ps`) y archivos de configuración de reglas (`.yaml`) diseñados para verificar y demostrar el funcionamiento del motor de PrintScript en sus versiones `1.0` y `1.1`.

---

## Contenido del Directorio

### 1. Archivos de Código Fuente (`.ps`)

| Archivo | Versión | Propósito / Descripción |
| :--- | :--- | :--- |
| [`valid_v10.ps`](./valid_v10.ps) | `1.0` | Programa válido de la versión 1.0: declaraciones `let`, operaciones aritméticas (`/`, `+`), concatenación de strings y `println`. |
| [`valid_v11.ps`](./valid_v11.ps) | `1.1` | Programa válido de la versión 1.1: declaraciones `const`, tipo `boolean`, y bloques condicionales `if-else`. |
| [`unformatted.ps`](./unformatted.ps) | `1.0` / `1.1` | Código con espaciado no estándar (`let a :number=10+20;`) utilizado para demostrar el formateador. |
| [`formatted_output.ps`](./formatted_output.ps) | `1.0` / `1.1` | Resultado esperado tras aplicar las reglas de [`format_rules.yaml`](./format_rules.yaml) sobre [`unformatted.ps`](./unformatted.ps). |
| [`sca_clean.ps`](./sca_clean.ps) | `1.0` / `1.1` | Código limpio que cumple al 100% las reglas de [`sca_rules.yaml`](./sca_rules.yaml) (variables en `camelCase`, argumento simple en `println`). |
| [`sca_test.ps`](./sca_test.ps) | `1.0` / `1.1` | Código que infringe reglas de SCA: variable en `snake_case` (`my_variable`) y expresión compuesta dentro de `println(my_variable + 5)`. |
| [`syntax_error.ps`](./syntax_error.ps) | `1.0` / `1.1` | Código con error de sintaxis intencional (`let b number = 20;`, falta `:`), útil para pruebas de validación y reporte de errores sintácticos. |
| [`semantic_error.ps`](./semantic_error.ps) | `1.0` / `1.1` | Código con error semántico intencional (referencia a variable no declarada `undeclaredVar`). |

---

### 2. Archivos de Configuración (`.yaml`)

#### [`format_rules.yaml`](./format_rules.yaml)
Reglas de estilo y espaciado para el formateador de código:
```yaml
spaceBeforeColon: false
spaceAfterColon: true
spaceAroundOperators: true
indentSize: 4
lineBreaksAfterPrintln: 1
```

#### [`sca_rules.yaml`](./sca_rules.yaml)
Reglas de análisis estático para el linter de código:
```yaml
identifier_format: "camel case"
mandatory-variable-or-literal-in-println: true
mandatory-variable-or-literal-in-readInput: true
```

---

## Cómo Ejecutar los Ejemplos

Puedes probar estos ejemplos directamente utilizando el script [`run.sh`](../run.sh) en la raíz del repositorio:

### Ejecución de Programas Válidos
```bash
# Versión 1.0
./run.sh exec 1.0 samples/valid_v10.ps

# Versión 1.1
./run.sh exec 1.1 samples/valid_v11.ps
```

### Formateo de Código
```bash
# Imprimir código formateado en consola
./run.sh format 1.0 samples/unformatted.ps samples/format_rules.yaml

# Guardar código formateado en un nuevo archivo
./run.sh format 1.0 samples/unformatted.ps samples/format_rules.yaml mi_formateado.ps
```

### Análisis Estático (SCA / Lint)
```bash
# Código limpio (no produce advertencias)
./run.sh sca 1.0 samples/sca_clean.ps samples/sca_rules.yaml

# Código con advertencias de estilo (detecta snake_case y expresión en println)
./run.sh sca 1.0 samples/sca_test.ps samples/sca_rules.yaml
```

### Validación de Errores
```bash
# Error de sintaxis
./run.sh exec 1.0 samples/syntax_error.ps

# Error semántico
./run.sh exec 1.0 samples/semantic_error.ps
```
