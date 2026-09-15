# Modulo buildSrc (Logica de Construccion y Calidad)

## Descripcion General

El directorio `buildSrc` contiene la logica de construccion compartida y los plugins de convencion (*Convention Plugins*) utilizados por todos los submodulos del proyecto PrintScript. 

Al centralizar la configuracion en `buildSrc`, se asegura que todos los modulos compartan la misma version del compilador, directivas de calidad, dependencias de prueba y plugins estandares sin duplicar scripts en cada `build.gradle`.

---

## Plugins de Convencion Disponibles

```
buildSrc/src/main/groovy/
├── buildlogic.java-common-conventions.gradle        # Convenciones comunes de Java y calidad
├── buildlogic.java-library-conventions.gradle       # Convenciones para librerias (java-library)
├── buildlogic.java-application-conventions.gradle   # Convenciones para modulos ejecutables (application)
├── com.ingsis.java-conventions.gradle              # Convencion basica heredada
└── PublishingConventionPlugin.groovy                # Plugin para publicacion de paquetes y versionado
```

### 1. `buildlogic.java-common-conventions`
Aplica y configura:
* **Toolchain de Java**: Fijado en **Java 21**.
* **Motor de Pruebas**: JUnit Jupiter 5.10.2 con ejecucion sobre la plataforma JUnit.
* **Cobertura de Codigo (JaCoCo 0.8.11)**:
  * Generacion automatica de reportes XML y HTML en `build/custom-reports/jacoco/`.
  * Verificacion estricta de cobertura minima del **80%** en lineas.
* **Formateador (Spotless 6.25.0)**:
  * Formato Google Java Format (modo AOSP, 4 espacios, ajuste de cadenas largas).
  * Limpieza de imports no utilizados y ordenamiento de imports.
  * Encabezado de licencia estandar.
* **Linter de Estilo (Checkstyle 10.15.0)**:
  * Longitud maxima de 15 lineas por metodo (`MethodLength`).
  * Prohibicion de tipos calificados inline (`MatchXpath`).
  * Supresion de `MethodLength` en tests (`suppressions.xml`).
* **Analisis Estatico de Bugs (SpotBugs 4.8.3)**:
  * Esfuerzo maximo (`MAX`) y reporte de baja confianza (`LOW`).
* **Linter de Errores Comunes (PMD)**:
  * Conjunto de reglas `category/java/errorprone.xml`.

### 2. `buildlogic.java-library-conventions`
* Aplica `buildlogic.java-common-conventions` y el plugin estandar `java-library` de Gradle para soportar separacion entre `api` e `implementation`.

### 3. `buildlogic.java-application-conventions`
* Aplica `buildlogic.java-common-conventions` y el plugin `application` de Gradle para empaquetar modulos con clase principal ejecutable (`mainClass`).
