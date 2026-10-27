# PrintScript - Guía Interna de Arquitectura y Desarrollo (README Privado)

Este documento es una guía técnica completa y detallada para desarrolladores del proyecto **PrintScript**. Explica la infraestructura de compilación con Gradle, la lógica de modularización, los plugins de convenciones, las reglas de calidad de código (Spotless, Checkstyle, PMD, SpotBugs, JaCoCo), el catálogo de tareas disponibles y la estrategia de versionado mediante Tags y Releases.

---

## 1. Arquitectura y Funcionamiento de Gradle

El proyecto utiliza **Gradle** estructurado bajo el patrón moderno de **Composite Builds / Convention Plugins** alojados en [`buildSrc`](file:///home/elchurro274/Faculty/ingsis/PrintScript/buildSrc). Esto desacopla la lógica de build de los submódulos individuales, asegurando configuración declarativa y DRY (Don't Repeat Yourself).

### 1.1. Jerarquía de Configuración

```text
PrintScript/
├── settings.gradle               # Declaración del root y registro de los 8 submódulos
├── build.gradle                  # Configuración root (JaCoCo unificado, githooks, allprojects)
├── buildSrc/                     # Plugins de convención reutilizables compilados automáticamente
│   ├── build.gradle              # Dependencias de buildSrc (Spotless, SpotBugs, Shadow)
│   └── src/main/groovy/
│       ├── PublishingConventionPlugin.groovy       # Configuración de maven-publish a GitHub Packages
│       ├── buildlogic.java-common-conventions.gradle # Linters, Formatter, Java 21, Cobertura
│       ├── buildlogic.java-library-conventions.gradle# Plugins para módulos biblioteca (java-library)
│       └── buildlogic.java-application-conventions.gradle # Plugins para el ejecutable CLI (application + shadow)
├── .checkstyle.xml               # Reglas de estilo de código Checkstyle
├── config/
│   ├── checkstyle/suppressions.xml # Exclusiones de reglas para tests
│   └── spotbugs/exclude.xml        # Filtros de exclusión para SpotBugs
└── com.ingsis.*/                 # Los 8 submódulos funcionales
```

### 1.2. ¿Qué se ejecuta en el `build.gradle` del Root?

El archivo [`build.gradle`](file:///home/elchurro274/Faculty/ingsis/PrintScript/build.gradle) en la raíz no compila código Java propio, sino que cumple tres propósitos clave:

1. **Gestión Unificada de Versiones y Grupos:**
   ```groovy
   allprojects {
       group = 'org.example'
       version = project.findProperty('version') ?: System.getenv('RELEASE_VERSION') ?: '1.0.0'
   }
   ```
   Permite inyectar la versión de forma dinámica tanto desde parámetros de línea de comandos (`-Pversion=1.2.0`) como desde variables de entorno (`RELEASE_VERSION`), defaulting a `1.0.0`.

2. **Consolidación y Reporte Agregado de Cobertura (JaCoCo):**
   Agrupa la ejecución de tests de todos los submódulos y construye un informe combinado:
   - Configura la tarea `jacocoTestReport` para recolectar los archivos `.exec` de cada submódulo (`executionData`).
   - Filtra clases generadas, de configuración o de pruebas (`**/config/**`, `**/*Application*`, `**/*Test*`).
   - Exporta el reporte HTML a `build/reports/jacoco/html` y XML a `build/reports/jacoco/jacoco.xml`.
   - Provee el alias `tasks.register('jacocoRootReport')`.

3. **Instalación de Git Hooks Versionados:**
   Define la tarea `installGitHooks`, la cual ejecuta:
   ```bash
   git config core.hooksPath .githooks
   ```
   Esto garantiza que los hooks de pre-commit y pre-push versionados en el repositorio se activen en la máquina del desarrollador.

---

## 2. Plugins de Convención (`buildSrc`)

En lugar de repetir dependencias y configuración en cada módulo, cada módulo aplica uno de los plugins de `buildSrc`:

### 2.1. `buildlogic.java-common-conventions`
Es el núcleo compartido por **todos** los submódulos. Aplica y configura:
- **`java`**: Configura Toolchain para **Java 21**.
- **`jacoco`** (versión 0.8.11): Exige un **mínimo de cobertura del 80%** en `jacocoTestCoverageVerification`.
- **`spotless`**: Formateo de código estricto.
- **`checkstyle`**: Validación sintáctica y buenas prácticas de código.
- **`pmd`**: Análisis estático de bugs y code smells.
- **`spotbugs`**: Análisis estático de bytecode a profundidad (esfuerzo `MAX`, reporte `LOW`).
- **`publishing-convention`**: Inyecta la publicación Maven a GitHub Packages.
- **Pipeline de `check`**: Configura que al correr `./gradlew check`, automáticamente se ejecuten:
  `spotlessCheck` $\rightarrow$ `spotbugsMain` $\rightarrow$ `checkstyleMain` $\rightarrow$ `pmdMain` $\rightarrow$ `jacocoTestCoverageVerification`.

### 2.2. `buildlogic.java-library-conventions`
Utilizado por los submódulos de biblioteca (`common`, `charstream`, `lexer`, `parser`, `interpreter`, `formatter`, `sca`).
Aplica `buildlogic.java-common-conventions` y añade `java-library` (habilitando `api` e `implementation`).

### 2.3. `buildlogic.java-application-conventions`
Utilizado exclusivamente por [`:com.ingsis.engine`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/build.gradle).
Aplica `buildlogic.java-common-conventions` y añade:
- Plugin `application` (punto de entrada `mainClass = 'engine.CliEngine'`).
- Plugin `shadow` (`com.github.johnrengelman.shadow`) para empaquetar un Fat JAR ejecutable autónomo.
- Dependencia con `info.picocli:picocli:4.7.7` para parseo de argumentos de línea de comandos.

---

## 3. Reglas de Calidad de Código: Formatter y Linters

### 3.1. Formatter: Spotless
Configurado en [`buildlogic.java-common-conventions.gradle`](file:///home/elchurro274/Faculty/ingsis/PrintScript/buildSrc/src/main/groovy/buildlogic.java-common-conventions.gradle):
- **Motor**: `googleJavaFormat('1.22.0').aosp().reflowLongStrings()`
  - Utiliza el estándar **AOSP (Android Open Source Project)**, que adopta una sangría de **4 espacios** (a diferencia de los 2 espacios del estándar Google clásico).
  - Ajusta y divide automáticamente strings demasiado largas (`reflowLongStrings`).
- **Limpieza de Imports**: Ejecuta `removeUnusedImports()` y ordena las declaraciones con `importOrder()`.
- **Header de Licencia**: Fuerza al inicio de cada archivo `.java` el encabezado:
  ```java
  /*
   * My Project
   */
  ```
- **Archivos Misceláneos**: Limpia espacios en blanco finales (`trimTrailingWhitespace`) e indenta con 2 espacios archivos `.md` y `.gitignore`.

### 3.2. Checkstyle
El archivo [`.checkstyle.xml`](file:///home/elchurro274/Faculty/ingsis/PrintScript/.checkstyle.xml) fija las directivas del linter de estilo:
1. **`OuterTypeFilename`**: El nombre del archivo debe corresponder exactamente con el nombre de la clase pública contenida.
2. **`MethodLength` (Máximo 20 líneas por método)**:
   ```xml
   <module name="MethodLength">
       <property name="max" value="20"/>
       <property name="countEmpty" value="false"/>
   </module>
   ```
   Ningún método puede superar las 20 líneas de código efectivo (las líneas en blanco no se contabilizan).
   *Excepción:* Mediante [`config/checkstyle/suppressions.xml`](file:///home/elchurro274/Faculty/ingsis/PrintScript/config/checkstyle/suppressions.xml), esta regla queda ignorada en clases de test (`.*Test\.java`).
3. **`OneStatementPerLine`**: Prohíbe escribir más de una sentencia por línea (evita sentencias compuestas ilegibles).
4. **`ParameterNumber` (Máximo 4 parámetros)**:
   ```xml
   <module name="ParameterNumber">
       <property name="max" value="4"/>
       <property name="ignoreOverriddenMethods" value="true"/>
   </module>
   ```
   Limita la cantidad de parámetros por método/constructor a un máximo de 4, ignorando métodos sobreescritos (`@Override`).
5. **`NestedIfDepth` (Profundidad máxima de `if` de 2)**:
   ```xml
   <module name="NestedIfDepth">
       <property name="max" value="2"/>
   </module>
   ```
   Evita el anidamiento excesivo de sentencias `if`, restringiendo la profundidad a un máximo de 2 niveles.

### 3.3. Git Hooks Automatizados
En la carpeta [`.githooks/`](file:///home/elchurro274/Faculty/ingsis/PrintScript/.githooks):
- **`pre-commit`**: Ejecuta `./gradlew spotlessApply` para auto-formatear los archivos modificados, los vuelve a agregar al staging de git (`git update-index --again`) y corre `./gradlew check -x test`.
- **`pre-push`**: Ejecuta la suite de pruebas completa `./gradlew test` antes de permitir el envío al remoto.

---

## 4. Tareas de Gradle (Tasks) Frecuentes

A continuación se detallan las tareas principales que se corren en el flujo diario:

| Comando | Descripción |
| :--- | :--- |
| `./gradlew test` | Ejecuta las pruebas unitarias e integración de todos los módulos y genera el reporte individual de cobertura. |
| `./gradlew jacocoRootReport` | Genera el reporte de cobertura HTML y XML consolidado de todo el proyecto en `build/reports/jacoco/html/index.html`. |
| `./gradlew spotlessCheck` | Verifica si el código fuente respeta las reglas de formateo sin modificar los archivos. |
| `./gradlew spotlessApply` | **Aplica el formateo automático:** sangría AOSP de 4 espacios, reordenamiento de imports y licencia. |
| `./gradlew checkstyleMain` | Ejecuta exclusivamente la verificación de Checkstyle sobre las clases principales. |
| `./gradlew check` | Ejecuta la batería de validación completa: Spotless + SpotBugs + Checkstyle + PMD + verificación de cobertura mínima (80%). |
| `./gradlew build` | Formatea el código (`spotlessApply`), compila todos los proyectos y ejecuta `check`. |
| `./gradlew run -p com.ingsis.engine` | Ejecuta la CLI interactiva del motor de PrintScript. |
| `./gradlew installGitHooks` | Configura Git para utilizar los hooks de validación local de `.githooks/`. |

---

## 5. Ejecución del Motor (PrintScript CLI Engine)

El módulo [`com.ingsis.engine`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine) expone la interfaz de línea de comandos integrada de PrintScript ([`CliEngine`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/src/main/java/engine/CliEngine.java)), soportando tanto ejecución por archivo como pipes desde `STDIN`, modo interactivo (REPL) y compatibilidad con las versiones `1.0` y `1.1`.

### 5.1. Formas de Ejecución

Existen dos vías principales para ejecutar la CLI localmente:

1. **Vía Gradle (desarrollo y pruebas rápidas):**
   ```bash
   ./gradlew :com.ingsis.engine:run --args="<operación> [opciones]"
   ```
2. **Vía Distribución Instalada (binario ejecutable standalone):**
   ```bash
   # 1. Generar la distribución ejecutable del engine
   ./gradlew :com.ingsis.engine:installDist

   # 2. Ejecutar el binario generado
   ./com.ingsis.engine/build/install/com.ingsis.engine/bin/com.ingsis.engine <operación> [opciones]
   ```

---

### 5.2. Opciones Globales de la CLI

| Opción | Descripción | Valor por Defecto |
| :--- | :--- | :--- |
| `-i, --input=<archivo>` | Archivo fuente PrintScript de entrada. | Si se omite, lee de `STDIN` (o entra al REPL). |
| `-o, --output=<archivo>` | Archivo de salida de resultados o código formateado. | `STDOUT` (consola) |
| `-c, --config=<archivo>` | Archivo de configuración YAML (para formateo o SCA). | Opcional |
| `-v, --version=<versión>` | Versión de especificación de PrintScript (`1.0` o `1.1`). | `1.0` |

---

### 5.3. Catálogo Completo de Comandos

#### 1. Ejecución de Código (`Execution` / `interpret` / `exec`)
Ejecuta el pipeline completo de PrintScript (Lexer $\rightarrow$ Parser $\rightarrow$ Semantic Check $\rightarrow$ Interpreter).

- **Ejecutar un archivo con PrintScript 1.0:**
  ```bash
  ./gradlew :com.ingsis.engine:run --args="Execution -v 1.0 -i ruta/al/archivo.ps"
  ```
- **Ejecutar un archivo con PrintScript 1.1 (soporta constantes y booleanos):**
  ```bash
  ./gradlew :com.ingsis.engine:run --args="Execution -v 1.1 -i ruta/al/archivo_v11.ps"
  ```
- **Ejecutar mediante Pipe (`STDIN`):**
  ```bash
  echo 'let x: string = "Hola Mundo"; println(x);' | ./gradlew :com.ingsis.engine:run --args="Execution -v 1.0"
  ```
- **Con binario compilado:**
  ```bash
  ./com.ingsis.engine/build/install/com.ingsis.engine/bin/com.ingsis.engine exec -v 1.0 -i archivo.ps
  ```
- **Aliases disponibles:** `Execution`, `interpret`, `exec`.

#### 2. Validación de Sintaxis y Semántica (`Validation` / `validate`)
Verifica que el código no tenga errores léxicos, sintácticos ni de tipos, sin llegar a ejecutarlo en runtime.

- **Validar archivo con versión 1.0:**
  ```bash
  ./gradlew :com.ingsis.engine:run --args="Validation -v 1.0 -i ruta/al/archivo.ps"
  ```
- **Validar archivo con versión 1.1:**
  ```bash
  ./gradlew :com.ingsis.engine:run --args="validate -v 1.1 -i ruta/al/archivo_v11.ps"
  ```
- **Validar leyendo desde Pipe (`STDIN`):**
  ```bash
  echo 'const x: boolean = true;' | ./gradlew :com.ingsis.engine:run --args="Validation -v 1.1"
  ```
- **Aliases disponibles:** `Validation`, `validate`.

#### 3. Formateo de Código (`Formatting` / `format` / `fmt`)
Aplica las reglas de espaciado, sangría y saltos de línea provistas en un archivo de configuración YAML.

- **Formatear e imprimir en consola (`STDOUT`):**
  ```bash
  ./gradlew :com.ingsis.engine:run --args="Formatting -v 1.0 -i ruta/al/codigo.ps -c config/formatter/rules.yaml"
  ```
- **Formatear y guardar en un archivo de salida (`-o`):**
  ```bash
  ./gradlew :com.ingsis.engine:run --args="format -v 1.0 -i ruta/al/codigo.ps -c config/formatter/rules.yaml -o formatted.ps"
  ```
- **Formatear leyendo desde `STDIN`:**
  ```bash
  cat codigo.ps | ./gradlew :com.ingsis.engine:run --args="fmt -v 1.0 -c config/formatter/rules.yaml -o formatted.ps"
  ```
- **Aliases disponibles:** `Formatting`, `format`, `fmt`.

#### 4. Análisis Estático / Linter (`Analyzing` / `analyze` / `lint`)
Analiza el código en busca de violaciones de estilo y buenas prácticas (SCA) definidas en un YAML (ej. convención de nomenclatura `camelCase` / `snake_case`, argumentos válidos en `println`, etc.).

- **Analizar archivo con reglas SCA:**
  ```bash
  ./gradlew :com.ingsis.engine:run --args="Analyzing -v 1.0 -i ruta/al/archivo.ps -c config/sca/rules.yaml"
  ```
- **Analizar con alias `lint` y versión 1.1:**
  ```bash
  ./gradlew :com.ingsis.engine:run --args="lint -v 1.1 -i ruta/al/archivo_v11.ps -c config/sca/rules.yaml"
  ```
- **Aliases disponibles:** `Analyzing`, `analyze`, `lint`.

#### 5. Modo REPL Interactivo
Si se ejecuta el motor sin proveer el parámetro `-i` ni enviar datos por `STDIN`, se inicia automáticamente la consola interactiva:

- **Iniciar REPL en versión 1.0:**
  ```bash
  ./gradlew :com.ingsis.engine:run
  ```
- **Iniciar REPL en versión 1.1:**
  ```bash
  ./gradlew :com.ingsis.engine:run --args="-v 1.1"
  ```
- **Uso en el REPL:**
  - Escribe sentencias de PrintScript normalmente.
  - Para ejecutar el bloque ingresado, presiona **Enter en una línea vacía**.
  - Para salir de la consola, escribe `exit` y presiona Enter.

---

## 6. Tags, Releases y Publicación de Paquetes

### 6.1. ¿Cómo funcionan las Tags y las Releases?

- **Git Tag:** Es un puntero inmutable en el historial de Git a un commit concreto. Se usa para fijar versiones semánticas (por ejemplo `v1.1.0` o `1.1.0`).
- **GitHub Release:** Es una capa sobre el tag que permite describir los cambios (*release notes*), adjuntar binarios y gatillar pipelines de CI/CD.

### 6.2. Pipeline de Publicación en GitHub Actions ([`.github/workflows/publish.yml`](file:///home/elchurro274/Faculty/ingsis/PrintScript/.github/workflows/publish.yml))

El workflow de publicación del proyecto cuenta con validación previa y resolución dinámica de versiones:

1. **Eventos Disparadores:**
   - Creación de Release en GitHub (`release: [published]`).
   - Push directo de un Tag de versión (`v*`, `*.*`, `[0-9]*`).
   - Push a la rama principal (`main`).
   - Disparo manual (`workflow_dispatch`) permitiendo ingresar la versión deseada.

2. **Control de Calidad Previo (`needs: ci`):**
   A diferencia de flujos simples, este workflow no publica ningún paquete sin antes ejecutar satisfactoriamente la verificación completa:
   - `spotlessCheck` (formato impecable).
   - `./gradlew check -x test` (Checkstyle, PMD, SpotBugs).
   - `./gradlew test` (todos los tests unitarios e integración).

3. **Determinación de Versión Inmutable:**
   - Si se publica desde un tag (ej: `v1.2.0`), elimina automáticamente la `v` inicial (`1.2.0`).
   - Si se hace push a `main`, autogenera `1.0.<run_number>`.
   - **Garantía de Inmutabilidad:** Falla explícitamente si la versión contiene `SNAPSHOT`. Los paquetes en GitHub Packages no deben reescribirse nunca.
   - Valida la nomenclatura de Versionado Semántico (SemVer).

4. **Publicación (`gradlew publish`):**
   Invoca el plugin [`PublishingConventionPlugin.groovy`](file:///home/elchurro274/Faculty/ingsis/PrintScript/buildSrc/src/main/groovy/PublishingConventionPlugin.groovy) que empaqueta cada submódulo en un artefacto Maven y lo sube al registro de **GitHub Packages** (`maven.pkg.github.com`).

---

### 6.3. Diferencia Crucial entre Git Tag y GitHub Release

- **`git push origin <tag>` (Git Tag):** Sube el puntero al repositorio de Git y dispara automáticamente la Action `publish.yml` para compilar y publicar los paquetes a **GitHub Packages**. Sin embargo, **no crea automáticamente una GitHub Release en la interfaz web de GitHub**.
- **GitHub Release:** Es la entidad visible en la sección *Releases* del repositorio. Contiene el título formal, el *Changelog* o notas de versión, los binarios adjuntos descargables y la marca de versión más reciente (*Latest*). Debe crearse apuntando al tag existente.

---

### 6.4. Guía Práctica: Crear una Tag y una Release desde la CLI

Todo el flujo se hace desde la terminal con `git` y la GitHub CLI (`gh`). Cada versión se publica **una sola vez**: los paquetes de GitHub Packages son inmutables y una versión ya subida no se puede volver a subir ni reutilizar.

#### 6.4.1. Requisitos (una sola vez)

```bash
gh --version          # GitHub CLI instalada (sudo dnf install gh / brew install gh)
gh auth login         # elegir GitHub.com, HTTPS y autenticar con el navegador
gh auth status        # debe mostrar la cuenta logueada y el scope "repo"
gh auth refresh -s read:packages   # permiso extra para listar los paquetes publicados (paso 7)
```

#### 6.4.2. Antes de versionar

```bash
# 1. Estar en main, sin cambios pendientes y al día con el remoto
git checkout main
git pull origin main
git status                       # debe decir "nothing to commit, working tree clean"

# 2. Verificar localmente lo mismo que corre el CI (incluye tests de contenedor si hay Docker)
./gradlew clean check

# 3. Confirmar que el último CI de main está en verde
gh run list --branch main --limit 3

# 4. Ver la última versión publicada para elegir la siguiente
git fetch --tags
git tag --sort=-v:refname | head -3
gh release list --limit 3
```

La versión sigue **SemVer** (`MAYOR.MENOR.PARCHE`): PARCHE para correcciones (`v1.2.1`), MENOR para funcionalidad nueva compatible (`v1.3.0`), MAYOR para cambios incompatibles (`v2.0.0`). El workflow quita la `v` inicial, así que el tag `v1.3.0` publica los paquetes con versión `1.3.0`.

#### 6.4.3. Flujo completo: Tag → Paquetes → Release

```bash
VERSION=1.3.0

# 1. Crear el tag ANOTADO sobre el último commit de main (guarda autor, fecha y mensaje)
git tag -a "v$VERSION" -m "Release $VERSION: <resumen de los cambios>"

# 2. Revisar a qué commit apunta antes de subirlo (todavía se puede borrar local: git tag -d "v$VERSION")
git show "v$VERSION" --stat | head -15

# 3. Subir SOLO ese tag. Esto dispara publish.yml, que corre los chequeos y publica en GitHub Packages
git push origin "v$VERSION"

# 4. Seguir la corrida hasta que termine (debe quedar en verde)
gh run list --workflow publish.yml --limit 3
gh run watch "$(gh run list --workflow publish.yml --limit 1 --json databaseId --jq '.[0].databaseId')"

# 5. Crear la GitHub Release sobre el tag ya existente, con notas generadas desde los commits y PRs
gh release create "v$VERSION" --verify-tag --title "Release $VERSION" --generate-notes

# 6. (Opcional) Adjuntar la CLI empaquetada para que se pueda descargar desde la release
./gradlew :com.ingsis.engine:distZip -Pversion="$VERSION"
gh release upload "v$VERSION" "com.ingsis.engine/build/distributions/com.ingsis.engine-$VERSION.zip"

# 7. Verificar
gh release view "v$VERSION"
gh api "/orgs/IngsisPrintScript2026/packages?package_type=maven" --jq '.[].name'
```

- `--verify-tag` hace que `gh` falle si el tag no existe en el remoto, en vez de crearlo solo sobre otro commit.
- Para escribir las notas a mano en vez de generarlas: `--notes-file CHANGELOG.md` o `--notes "texto"`.
- Para dejar la release como borrador y publicarla después: `--draft`, y luego `gh release edit "v$VERSION" --draft=false`.
- Para marcar una versión de prueba: `--prerelease`.

> **⚠️ Doble disparo de `publish.yml`.** El workflow escucha tanto el push del tag (paso 3) como la publicación de la release (paso 5), y las dos corridas intentan publicar la misma versión. La segunda falla con `Received status code 409 from server: Conflict`. Ya pasó con `v1.2.0` el 29/09/2026: la corrida del tag publicó bien y la de la release quedó en rojo. Los paquetes quedan publicados igual (por la primera corrida); la corrida en rojo de la release se puede ignorar. Para eliminarlo, ver 6.4.6.

#### 6.4.4. Publicar sin tag (disparo manual)

El workflow también acepta un disparo manual con la versión como parámetro, útil para re-publicar si la corrida del tag falló por un problema externo (red, credenciales) **antes** de subir paquetes:

```bash
gh workflow run publish.yml --ref main -f version=1.3.0
gh run watch "$(gh run list --workflow publish.yml --limit 1 --json databaseId --jq '.[0].databaseId')"
```

#### 6.4.5. Corregir errores

```bash
# El tag apunta al commit equivocado y TODAVÍA no se publicó nada:
git tag -d "v$VERSION"                    # borrar local
git push origin --delete "v$VERSION"      # borrar remoto
# ...y volver a 6.4.3 desde el paso 1.

# La release tiene mal el título o las notas:
gh release edit "v$VERSION" --title "Release $VERSION" --notes-file CHANGELOG.md

# Borrar la release y su tag remoto juntos:
gh release delete "v$VERSION" --cleanup-tag --yes
```

Si los paquetes de esa versión ya se publicaron, **no se reutiliza el número**: se corrige en `main` y se saca la siguiente versión de parche (`1.3.1`).

#### 6.4.6. Evitar el doble disparo (cambio sugerido, no aplicado)

Para que cada versión dispare una sola publicación, alcanza con que `publish.yml` no escuche las releases y deje que el tag sea el único disparador:

```yaml
on:
  push:
    branches: [ main ]
    tags:
      - 'v*'
      - '*.*'
      - '[0-9]*'
  # release:            ← quitar este disparador
  #   types: [ published ]
  workflow_dispatch:
```

Con ese cambio, el paso 5 de 6.4.3 solo crea la página de la release y no vuelve a publicar.

#### 6.4.7. Alternativa desde la interfaz web

1. Subir el tag desde la terminal (pasos 1 a 4 de 6.4.3).
2. Entrar a `https://github.com/IngsisPrintScript2026/PrintScript/releases` y presionar **"Draft a new release"**.
3. En **"Choose a tag"**, elegir el tag subido (ej: `v1.3.0`).
4. Completar el título, presionar **"Generate release notes"** y luego **"Publish release"**.

El aviso de doble disparo de 6.4.3 aplica igual: publicar la release desde la web también dispara `publish.yml`.

#### Resumen rápido

```bash
VERSION=1.3.0
git checkout main && git pull origin main && ./gradlew clean check
git tag -a "v$VERSION" -m "Release $VERSION"
git push origin "v$VERSION"                                   # publica los paquetes
gh release create "v$VERSION" --verify-tag --title "Release $VERSION" --generate-notes
```

---

## 7. Reportes Generados y Consumo de Paquetes

### 7.1. Ubicación de Reportes Locales
Cada tarea de análisis y verificación vuelca sus informes en carpetas personalizadas dentro del directorio `build/` de cada módulo:
- **Checkstyle:** `<subproject>/build/custom-reports/checkstyle/checkstyleMain.html`
- **PMD:** `<subproject>/build/custom-reports/pmd/pmdMain.html`
- **SpotBugs:** `<subproject>/build/custom-reports/spotbugs/spotbugsMain.html`
- **JaCoCo (Cobertura individual):** `<subproject>/build/reports/jacoco/test/html/index.html`
- **JaCoCo Agregado (Todo el proyecto):** `build/reports/jacoco/html/index.html` (generado vía `./gradlew jacocoRootReport`).

### 7.2. Consumo en Proyectos Externos
Para consumir los artefactos publicados en GitHub Packages desde otro proyecto (por ejemplo, una UI web o un servicio backend en Spring Boot):

```groovy
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/IngsisPrintScript2026/PrintScript")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gprUser")
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gprToken")
        }
    }
}

dependencies {
    implementation 'org.example:com.ingsis.engine:1.2.0'
}
```

