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

## 5. Tags, Releases y Publicación de Paquetes

### 5.1. ¿Cómo funcionan las Tags y las Releases?

- **Git Tag:** Es un puntero inmutable en el historial de Git a un commit concreto. Se usa para fijar versiones semánticas (por ejemplo `v1.1.0` o `1.1.0`).
- **GitHub Release:** Es una capa sobre el tag que permite describir los cambios (*release notes*), adjuntar binarios y gatillar pipelines de CI/CD.

### 5.2. Pipeline de Publicación en GitHub Actions ([`.github/workflows/publish.yml`](file:///home/elchurro274/Faculty/ingsis/PrintScript/.github/workflows/publish.yml))

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

### 5.3. Guía Práctica para Publicar una Nueva Versión

#### Método 1: Por Terminal con Git Tags (Recomendado)
```bash
# 1. Asegurar estado sincronizado con main
git checkout main
git pull origin main

# 2. Crear el tag anotado siguiendo SemVer (Major.Minor.Patch)
git tag -a v1.2.0 -m "Release 1.2.0: Soporte PrintScript 1.1 y mejoras del linter"

# 3. Subir el tag al remoto
git push origin v1.2.0
```
*El workflow detectará el tag `v1.2.0`, correrá los tests y publicará la versión `1.2.0` en GitHub Packages.*

#### Método 2: Por la Interfaz de GitHub (Releases)
1. Entrar al repositorio en GitHub $\rightarrow$ **Releases** $\rightarrow$ **Draft a new release**.
2. Crear un nuevo tag (ej: `v1.2.0`) sobre la rama `main`.
3. Asignar un título y redactar el *Changelog* o notas de la versión.
4. Presionar **Publish release**.

---

## 6. Reportes Generados y Consumo de Paquetes

### 6.1. Ubicación de Reportes Locales
Cada tarea de análisis y verificación vuelca sus informes en carpetas personalizadas dentro del directorio `build/` de cada módulo:
- **Checkstyle:** `<subproject>/build/custom-reports/checkstyle/checkstyleMain.html`
- **PMD:** `<subproject>/build/custom-reports/pmd/pmdMain.html`
- **SpotBugs:** `<subproject>/build/custom-reports/spotbugs/spotbugsMain.html`
- **JaCoCo (Cobertura individual):** `<subproject>/build/reports/jacoco/test/html/index.html`
- **JaCoCo Agregado (Todo el proyecto):** `build/reports/jacoco/html/index.html` (generado vía `./gradlew jacocoRootReport`).

### 6.2. Consumo en Proyectos Externos
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

