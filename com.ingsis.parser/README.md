# Modulo com.ingsis.parser

## Descripcion General

El modulo com.ingsis.parser es el componente responsable del analisis sintactico y semantico del lenguaje PrintScript. Su proposito principal consiste en consumir un flujo de tokens (generado por el modulo lexer) y estructurarlo jerarquicamente en un Arbol de Sintaxis Abstracta (AST - Abstract Syntax Tree), validando a su vez la correccion gramatical, la coherencia de tipos y las reglas de ambito (scope) del lenguaje segun la version especificada (1.0 o 1.1).

El modulo integra dos etapas fundamentales del pipeline de compilacion:
1. Analisis Sintactico (Parsing): Descenso recursivo combinado con Pratt Parsing (Top-Down Operator Precedence) para resolver la precedencia asociativa de operadores en expresiones binarias, junto a estrategias configurables por version.
2. Analisis Semantico (Type & Scope Checking): Recorrido del AST mediante el patron Visitor y manejadores especializados para verificar que las variables esten declaradas, no existan redeclaraciones ilegales, se respete la inmutabilidad de constantes, los tipos en operaciones y asignaciones coincidan y las estructuras de control posean condiciones booleanas validas.

---

## Dependencias del Modulo

El modulo parser interactua con las siguientes dependencias internas definidas en build.gradle:

| Modulo | Tipo de Dependencia | Uso e Insumos Consumidos |
| :--- | :--- | :--- |
| com.ingsis.common | implementation | Nodos del AST (Node, ProgramNode, DeclarationKeywordNode, AssignNode, IfKeywordNode, CallFunctionNode, OperatorNode, IdentifierNode, NumberLiteralNode, StringLiteralNode, BooleanLiteralNode, NilExpressionNode), Enums (TokenType, SymbolType, DataType, OperatorType, DeclarationType), Monada Result (Result, CorrectResult, IncorrectResult), Iteradores (SafeIterator, IterationStep, TokenStream), Posicionamiento (Position) y Version del lenguaje (Version). |
| com.ingsis.charstream | implementation | Abstraccion del flujo de caracteres de entrada. |
| com.ingsis.lexer | implementation | Abstraccion y generador de tokens a partir del flujo de caracteres. |

---

## Arquitectura del Modulo

El modulo esta dividido en tres capas principales:

1. tokenstream: Abstraccion y consumo del flujo de tokens (modo perezoso o en memoria, reglas de coincidencia y gramaticas por version).
2. syntactic: Analizadores sintacticos que transforman la secuencia de tokens en nodos del AST.
3. semantic: Analizador semantico y entorno de tipos que valida la correccion logica del AST.

```
com.ingsis.parser/
├── build.gradle
├── README.md
└── src/
    ├── main/java/
    │   ├── Parser.java                                # Interfaz raiz del analizador
    │   ├── tokenstream/
    │   │   ├── LazyTokenStream.java                   # Flujo perezoso evaluado sobre SafeIterator
    │   │   ├── TokenStreamAdapter.java                # Adaptador inmutable sobre List<Token>
    │   │   ├── rules/
    │   │   │   └── TokenMatchers.java                 # Predicados auxiliares para matching de tokens
    │   │   └── version/
    │   │       └── GrammarRules.java                  # Definicion de reglas y predicados por version
    │   ├── syntactic/
    │   │   ├── Parser.java                            # Interfaz generica de sub-parsers (Node)
    │   │   ├── SyntacticParser.java                   # Orquestador sintactico principal (ProgramNode)
    │   │   ├── parser/
    │   │   │   ├── ParserFactory.java                 # Ensamblador de cadenas de parsers segun version
    │   │   │   ├── literal/
    │   │   │   │   ├── BooleanLiteralParser.java      # Parser de literales booleanos
    │   │   │   │   ├── IdentifierParser.java          # Parser de nombres de variables e identificadores
    │   │   │   │   ├── NumberLiteralParser.java       # Parser de numeros a BigDecimal
    │   │   │   │   └── StringLiteralParser.java       # Parser de strings sanitizando delimitadores
    │   │   │   ├── operator/
    │   │   │   │   └── OperatorParser.java            # Algoritmo Pratt para precedencia de operadores
    │   │   │   └── root/
    │   │   │       ├── AssignParser.java              # Parser de sentencias de reasignacion
    │   │   │       ├── ConditionalParser.java         # Parser de estructuras if / else
    │   │   │       ├── DeclarationParser.java         # Parser de declaraciones let / const
    │   │   │       ├── FunctionParser.java            # Parser de llamadas a funcion con argumentos
    │   │   │       └── LineExpressionParser.java      # Parser de expresiones independientes con ';'
    │   │   ├── strategy/
    │   │   │   ├── DeclarationSymbolStrategy.java     # Interfaz para continuacion de declaracion
    │   │   │   ├── AssignmentSymbolStrategy.java      # Estrategia para declaraciones inicializadas '='
    │   │   │   ├── EmptyDeclarationSymbolStrategy.java# Estrategia para declaraciones no inicializadas ';'
    │   │   │   ├── ConditionalElseStrategy.java       # Interfaz para resolucion de clausula else
    │   │   │   ├── WithElseStrategy.java              # Estrategia para rama 'else { ... }'
    │   │   │   └── WithoutElseStrategy.java           # Estrategia cuando no hay rama else
    │   │   ├── util/
    │   │   │   ├── ArgumentsParserUtils.java          # Utilidad para parsear listas separadas por comas
    │   │   │   └── BlockParserUtils.java              # Utilidad para parsear bloques entre llaves '{ }'
    │   │   └── version/
    │   │       ├── VersionStrategy.java               # Contrato de configuracion de gramatica por version
    │   │       ├── VersionStrategyRegistry.java       # Registro y resolucion de VersionStrategy
    │   │       ├── Version10Strategy.java             # Reglas para PrintScript version 1.0
    │   │       └── Version11Strategy.java             # Reglas para PrintScript version 1.1
    │   └── semantic/
    │       ├── SemanticChecker.java                   # Visitor que orquesta la verificacion semantica
    │       ├── SemanticStep.java                      # Registro del estado semantico intermedio
    │       ├── environment/
    │       │   └── SemanticEnvironment.java           # Tabla de simbolos con soporte de scopes jerarquicos
    │       ├── evaluator/
    │       │   └── ExpressionTypeInference.java       # Evaluador e inferidor estatico de tipos
    │       └── handler/
    │           ├── SemanticNodeHandler.java           # Interfaz generica para handlers por nodo
    │           ├── DeclarationNodeSemanticHandler.java# Validacion de declaraciones y mutabilidad
    │           ├── AssignNodeSemanticHandler.java     # Validacion de asignaciones y compatibilidad de tipos
    │           ├── IfNodeSemanticHandler.java         # Validacion de condiciones booleanas y ramas
    │           └── CallFunctionNodeSemanticHandler.java# Validacion de argumentos en llamadas a funciones
    └── test/java/
        ├── syntactic/
        │   ├── ExpressionParserTest.java              # Tests de precedencia de operadores y expresiones
        │   ├── SyntacticParserTest.java               # Tests del orquestador sintactico general
        │   └── SyntacticGrammarAndParsersTest.java    # Tests exhaustivos de componentes y estrategias
        ├── tokenstream/
        │   └── LazyTokenStreamTest.java               # Tests de evaluacion diferida y memoizacion del stream
        └── semantic/
            └── SemanticCheckerTest.java               # Tests de validacion semantica y analisis de tipos
```

---

## Capa 1: Flujo de Tokens (TokenStream)

Provee una abstraccion segura e inmutable para recorrer los tokens emitidos por el lexer sin perdida de estado, permitiendo avanzar (consume) o inspeccionar hacia adelante (peek) sin alterar el puntero de manera destructiva.

### 1. TokenStreamAdapter
- Proposito: Implementa TokenStream utilizando una lista inmutable estatica de tokens (List.copyOf).
- Mecanismo: Mantiene un indice entero (pointer). Cada llamada a consume() retorna una nueva instancia de TokenStreamAdapter con pointer + 1.
- Uso tipico: Pruebas unitarias o analisis de scripts completos cargados en memoria.

### 2. LazyTokenStream
- Proposito: Implementacion de evaluacion perezosa (lazy evaluation) que consume un SafeIterator<Token> bajo demanda.
- Mecanismo: Modela una lista simplemente enlazada de nodos memoizados (Node) de forma segura y concurrente. Solo solicita el siguiente token al iterador subyacente cuando se llama a consume() o peek(offset). Si varios caminos sintacticos inspeccionan los mismos tokens, el resultado queda almacenado en cache en los nodos ya construidos, evitando re-tokenizaciones o avances destructivos.
- Manejo de fin de archivo y errores: Detecta mensajes de "EOF" e interrupciones lexicas, propagando fallos a traves de Result.failure(...) sin lanzar excepciones.

### 3. TokenMatchers
Provee metodos de factoria estaticos para crear predicados de emparejamiento (Predicate<Token>):
- isType(TokenType type): Verifica coincidencia exacta del tipo de token.
- isOneOf(TokenType... types): Verifica si el tipo del token pertenece a un conjunto admitido.
- isTypeAndValue(TokenType type, String expectedValue): Verifica simultaneamente tipo y valor textual.

### 4. GrammarRules
Record inmutable que define que tokens son admitidos para:
- declarationKeywords: Palabras clave de declaracion (let en 1.0; let y const en 1.1).
- supportedDataTypes: Tipos de datos soportados (string, number en 1.0; string, number, boolean en 1.1).
- binaryOperators: Operadores binarios validos (+, -, *, /).

---

## Capa 2: Analisis Sintactico (Syntactic Parsing)

La capa sintactica transforma el flujo plano de tokens en una estructura arborescente (AST).

### Interfaces Base
- Parser (en paquete raiz): Result<Node> parse(TokenStream stream)
- syntactic.Parser<T extends Node>: Subinterfaz tipada que extiende parser.Parser<T> de common, retornando Result<IterationStep<T>>.

### Orquestador: SyntacticParser
Implementa Parser<ProgramNode>:
- parseStatement(TokenStream stream): Intenta parsear una unica sentencia delegando en la cadena configurada por ParserFactory.
- parseProgram(TokenStream stream): Itera sobre el flujo ejecutando parseStatement mientras no este vacio. Acumula las sentencias y retorna un ProgramNode(statements, 1, 1). Si alguna sentencia falla, interrumpe el proceso de inmediato retornando el error de forma determinista.

### Factoria y Ensamblado: ParserFactory
Construye la jerarquia completa de analizadores sintacticos desacoplados mediante el patron Factory y Dependency Injection:
- Resuelve dependencias ciclicas (por ejemplo, expresiones dentro de sentencias y sentencias dentro de bloques condicionales) empleando contenedores AtomicReference y proveedores Supplier.
- Conecta los analizadores de sentencias (DeclarationParser, AssignParser, ConditionalParser, LineExpressionParser) evaluandolos en orden secuencial como una cadena de responsabilidad: el primer parser que reconoce la sintaxis consume el flujo y genera el nodo.
- Ensambla el parser de expresiones configurando la precedencia de operadores con OperatorParser y los parsers primarios.

### Versionado de Sintaxis
Permite extender la gramatica sin romper retrocompatibilidad mediante el patron Strategy:
- VersionStrategy: Define version(), declarationKeywords(), supportedDataTypes(), primaryParsers(...) y statementParsers(...).
- VersionStrategyRegistry: Mapea cada enum Version a su estrategia respectiva.
- Version10Strategy:
  - Declaraciones: Solo let.
  - Tipos: string y number.
  - Literales primarios: number, string, llamadas a funcion, identificadores.
  - Sentencias: Declaracion, asignacion, expresiones en linea. (No soporta if ni booleanos).
- Version11Strategy:
  - Declaraciones: let y const.
  - Tipos: string, number y boolean.
  - Literales primarios: number, string, boolean, llamadas a funcion, identificadores.
  - Sentencias: Declaracion, asignacion, condicionales if / else, expresiones en linea.

### Parsers de Sentencias (syntactic.parser.root)
1. DeclarationParser:
   - Secuencia: Keyword de declaracion (let / const) -> Identificador -> Dos puntos (:) -> Tipo de dato (string / number / boolean).
   - Delegacion por estrategia: Utiliza un mapa de DeclarationSymbolStrategy segun el siguiente token inspeccionado:
     - AssignmentSymbolStrategy: Consume '=', parsea la expresion asignada, consume ';' y produce un DeclarationKeywordNode inicializado.
     - EmptyDeclarationSymbolStrategy: Consume ';' y produce un DeclarationKeywordNode con un NilExpressionNode como expresion (no inicializado).
2. AssignParser:
   - Secuencia: Identificador -> '=' -> Expresion -> ';'.
   - Produce un AssignNode vinculando la variable existente con la nueva expresion calculada.
3. ConditionalParser:
   - Secuencia: 'if' -> '(' -> Expresion de condicion -> ')' -> '{' -> Cuerpo then -> '}'.
   - Utiliza BlockParserUtils para el parseo del bloque then.
   - Resuelve la rama opcional else mediante ConditionalElseStrategy:
     - WithElseStrategy: Si el siguiente token es 'else', consume 'else' y parsea el bloque '{ ... }' retornando un IfKeywordNode con thenBody y elseBody.
     - WithoutElseStrategy: Si no hay token 'else', construye el IfKeywordNode con un elseBody vacio.
4. LineExpressionParser:
   - Parsea una expresion valida seguida obligatoriamente por un punto y coma (';').
5. FunctionParser:
   - Parsea la invocacion a una funcion: Identificador '(' argumentos ')'.
   - Utiliza ArgumentsParserUtils para parsear los argumentos separados por comas y produce un CallFunctionNode.

### Parsers de Expresiones y Precedencia (syntactic.parser.operator y literal)
1. OperatorParser (Pratt Parsing):
   - Implementa el algoritmo de precedencia de operadores descendente (Top-Down Operator Precedence).
   - Inicia consumiendo la expresion primaria izquierda (left).
   - Mientras el siguiente token sea un operador binario cuyo lBindingPower sea superior al rightBindingPower del contexto actual:
     - Consume el operador.
     - Parsea recursivamente el lado derecho con el rBindingPower del operador.
     - Construye un OperatorNode.
     - Continua evaluando la cola de la expresion asociando operadores segun su peso matematico (por ejemplo, '*' y '/' tienen mayor prioridad que '+' y '-').
2. Parsers de Literales (Leaf Parsers):
   - IdentifierParser: Valida y extrae tokens IDENTIFIER o PRINTLN creando un IdentifierNode.
   - NumberLiteralParser: Consume NUMBER_LITERAL y crea un NumberLiteralNode conteniendo un BigDecimal para garantizar precision aritmetica arbitraria.
   - StringLiteralParser: Consume STRING_LITERAL, sanitiza las comillas delimitadoras circundantes (simples o dobles) y crea un StringLiteralNode.
   - BooleanLiteralParser: Consume BOOLEAN_LITERAL, convierte el valor textual a boolean primitivo y crea un BooleanLiteralNode.

### Utilidades Sintacticas (syntactic.util)
- BlockParserUtils: Encapsula la apertura y cierre de llaves ('{' y '}'), parseando iterativamente las sentencias internas mediante el parser provisto hasta encontrar el delimitador de cierre o EOF.
- ArgumentsParserUtils: Encapsula el consumo de listas de expresiones delimitadas por parentesis '(' y ')' y separadas por comas ',', manejando correctamente listas vacias o con multiples parametros.

---

## Capa 3: Analisis Semantico (Semantic Analysis)

La capa semantica se encarga de validar el AST antes de su envio al interprete, garantizando que el programa tenga sentido logico y satisfaga el sistema de tipos estatico de PrintScript.

### 1. SemanticEnvironment (Tabla de Simbolos y Scopes)
Modela los ambitos lexicos de forma inmutable y jerarquica:
- Estructura: Cada entorno posee una referencia a su padre (parent: SemanticEnvironment) y un mapa local de simbolos (Map<String, VariableSymbol>).
- VariableSymbol: Record que almacena:
  - type: DataType (NUMBER, STRING, BOOLEAN).
  - isMutable: boolean (true para let, false para const).
  - isInitialized: boolean (true si posee valor inicial).
- Operaciones:
  - define(name, type, isMutable, isInitialized): Retorna una nueva instancia de SemanticEnvironment con la variable agregada en el ambito actual.
  - lookup(name): Busca recursivamente la variable en el mapa local; si no existe, delega en el entorno padre, soportando anidamiento de bloques (shadowing).

### 2. ExpressionTypeInference (Inferidor de Tipos)
Determina estaticamente el tipo resultante (DataType) de cualquier nodo de expresion:
- Literales:
  - NumberLiteralNode -> DataType.NUMBER
  - StringLiteralNode -> DataType.STRING
  - BooleanLiteralNode -> DataType.BOOLEAN
  - NilExpressionNode -> Falla indicando que no se puede inferir tipo de una expresion nula.
- Identificadores (IdentifierNode):
  - Consulta lookup en el SemanticEnvironment. Si la variable no existe, emite un error semantico indicando que la variable no esta declarada.
- Operadores Binarios (OperatorNode):
  - Operador '+':
    - Si al menos uno de los operandos es DataType.STRING, el resultado es DataType.STRING (concatenacion).
    - Si ambos son DataType.NUMBER, el resultado es DataType.NUMBER (suma numerica).
    - Cualquier otra combinacion de tipos falla con error de operandos incompatibles.
  - Operadores '-', '*', '/':
    - Requieren estrictamente que ambos operandos sean DataType.NUMBER. De lo contrario, emite error de incompatibilidad numerica.
- Funciones (CallFunctionNode):
  - Para funciones del sistema como readInput o readEnv retorna null (tipo abierto que se resuelve segun el contexto de asignacion). Para el resto, infiere DataType.STRING por defecto.

### 3. SemanticChecker (Orquestador Visitor)
Implementa NodeVisitor<Result<SemanticEnvironment>, SemanticEnvironment> y coordina la ejecucion de manejadores dedicados (SemanticNodeHandler) para cada tipo de nodo:
- check(ProgramNode program): Ejecuta el chequeo semantico de todo el programa partiendo de un SemanticEnvironment global vacio.
- Recorre secuencialmente cada sentencia, pasando el entorno actualizado de una instruccion a la siguiente. Si una instruccion falla, el analisis se interrumpe y propaga el fallo de inmediato.

### 4. Manejadores Semanticos (semantic.handler)
- DeclarationNodeSemanticHandler:
  - Verifica que la variable no haya sido declarada previamente en el entorno visible (previene doble declaracion).
  - Si no tiene expresion asignada (declaracion vacia), la define como no inicializada.
  - Si posee expresion, infiere su tipo con ExpressionTypeInference y comprueba que coincida con el tipo explicito declarado (por ejemplo, let x: number = "texto"; genera error semantico de tipos incompatibles).
  - Registra si la variable es mutable o inmutable (const).
- AssignNodeSemanticHandler:
  - Verifica que la variable a reasignar exista en el entorno.
  - Verifica que no sea una constante inmutable previamente inicializada (previene reasignacion a const).
  - Infiere el tipo de la nueva expresion y valida que coincida con el tipo original de la variable.
  - Actualiza el estado de la variable a inicializada = true.
- IfNodeSemanticHandler:
  - Infiere el tipo de la expresion condicional y verifica obligatoriamente que sea DataType.BOOLEAN.
  - Crea entornos derivados e independientes para el cuerpo then (thenEnv) y el cuerpo else (elseEnv), aislando las declaraciones locales de cada rama y validando sus sentencias internas.
- CallFunctionNodeSemanticHandler:
  - Valida que todas las expresiones pasadas como argumentos a la llamada a funcion sean computables y semanticamente validas.

---

## Gramatica Formal Soportada

A continuacion se presenta la gramatica en formato EBNF implementada por el modulo:

```ebnf
Program         ::= Statement*

Statement       ::= DeclarationStatement
                  | AssignmentStatement
                  | ConditionalStatement    (* Version 1.1 *)
                  | LineExpressionStatement

DeclarationStatement ::= DeclKeyword IDENTIFIER ':' DataType ('=' Expression)? ';'
DeclKeyword          ::= 'let' | 'const'    (* 'const' solo en Version 1.1 *)
DataType             ::= 'string' | 'number' | 'boolean' (* 'boolean' solo en Version 1.1 *)

AssignmentStatement  ::= IDENTIFIER '=' Expression ';'

ConditionalStatement ::= 'if' '(' Expression ')' '{' Statement* '}' ('else' '{' Statement* '}')?

LineExpressionStatement ::= Expression ';'

Expression      ::= Equality
Equality        ::= Term (('+' | '-') Term)*
Term            ::= Factor (('*' | '/') Factor)*
Factor          ::= Primary

Primary         ::= NUMBER_LITERAL
                  | STRING_LITERAL
                  | BOOLEAN_LITERAL         (* Version 1.1 *)
                  | IDENTIFIER
                  | FunctionCall
                  | '(' Expression ')'

FunctionCall    ::= IDENTIFIER '(' (Expression (',' Expression)*)? ')'
```

---

## Patrones de Diseno Aplicados

| Patron | Aplicacion en el Modulo |
| :--- | :--- |
| Factory Method | ParserFactory.createParser() centraliza la configuracion y cableado de analizadores segun la version del lenguaje. |
| Strategy | VersionStrategy (Version10Strategy, Version11Strategy), DeclarationSymbolStrategy y ConditionalElseStrategy permiten variar las reglas sintacticas sin modificar los parsers principales. |
| Pratt Parsing | OperatorParser resuelve la precedencia asociativa de operadores matematicos mediante valores numericos de fuerza de union (binding power). |
| Chain of Responsibility | ParserFactory combina los parsers de sentencias y primarios en secuencias donde cada componente evalua si le corresponde procesar el flujo o delegar al siguiente. |
| Visitor | SemanticChecker implementa NodeVisitor para recorrer la jerarquia del AST sin acoplar la logica semantica a las clases de nodos. |
| Adapter | TokenStreamAdapter adapta una lista estatica de tokens al contrato funcional de TokenStream. |
| Monad Result | Result<T> (CorrectResult / IncorrectResult) se utiliza de manera ubicua para el manejo funcional de errores sin lanzar excepciones no controladas. |
| Null Object | NilExpressionNode representa la ausencia de expresion inicial en declaraciones no inicializadas. |
| Lazy Evaluation | LazyTokenStream computa y memoiza tokens a traves de una estructura de nodos enlazados sincronizados a medida que el parser los solicita. |

---

## Cobertura y Casos de Prueba

El modulo cuenta con una suite completa de pruebas unitarias que cubren todas sus capas:

1. SyntacticParserTest:
   - Verifica el parseo de programas completos de multiples lineas.
   - Comprueba la integracion de declaraciones, asignaciones y llamadas a funciones.
   - Valida el corte determinista ante errores sintacticos y fin de flujo (EOF).
2. ExpressionParserTest:
   - Verifica la precedencia de operadores matematicos (ejemplo: '5 + 3 * 2' agrupa la multiplicacion antes de la suma).
   - Verifica la asociatividad izquierda y expresiones complejas con multiples terminos.
3. SyntacticGrammarAndParsersTest:
   - Verifica cada analizador individualmente: DeclarationParser, AssignParser, ConditionalParser (con y sin else), FunctionParser, BlockParserUtils, ArgumentsParserUtils.
   - Verifica el comportamiento diferencial entre Version 1.0 y Version 1.1 (rechazo de const o if en 1.0, aceptacion en 1.1).
   - Valida el tratamiento de strings con comillas escapadas o simples/dobles.
4. LazyTokenStreamTest:
   - Comprueba que el stream no consuma tokens antes de tiempo.
   - Verifica peeking a distancias arbitrarias (peek(0), peek(1), etc.) sin avanzar el puntero principal.
   - Valida la propagacion de errores lexicos y la deteccion precisa de fin de archivo.
5. SemanticCheckerTest:
   - Verifica declaracion y asignacion con tipos compatibles e incompatibles.
   - Valida que no se pueda reasignar una constante (const).
   - Valida la deteccion de variables no declaradas o redeclaradas.
   - Verifica que las condiciones en sentencias if sean estrictamente booleanas.
   - Valida la inferencia de tipos con operadores mixtos (concatenacion string + number vs suma number + number) y aislamiento de ambitos lexicos.
