# Diagrama UML y Arquitectura del Sistema PrintScript

Este documento detalla la arquitectura integral de **PrintScript** mediante diagramas UML (en formato Mermaid) que cubren las diferentes capas del sistema, la descomposición modular, la jerarquía de clases, los patrones de diseño aplicados y la dinámica de ejecución.

---

## Tabla de Contenidos
1. [Vista Modular y Paquetes (Diagrama de Componentes)](#1-vista-modular-y-paquetes-diagrama-de-componentes)
2. [Capa Base y Streaming de Entrada (`common`, `charstream`, `lexer`)](#2-capa-base-y-streaming-de-entrada-common-charstream-lexer)
3. [Jerarquía AST y Patrón Visitor (`common.node`)](#3-jerarquía-ast-y-patrón-visitor-commonnode)
4. [Análisis Sintáctico y Semántico (`parser`)](#4-análisis-sintáctico-y-semántico-parser)
5. [Motor de Ejecución e Intérprete (`interpreter`)](#5-motor-de-ejecución-e-intérprete-interpreter)
6. [Fachada, Servicios y Herramientas (`engine`, `formatter`, `sca`)](#6-fachada-servicios-y-herramientas-engine-formatter-sca)
7. [Diagrama de Secuencia: Flujo de Ejecución de Sentencias](#7-diagrama-de-secuencia-flujo-de-ejecución-de-sentencias)
8. [Matriz de Patrones de Diseño Aplicados](#8-matriz-de-patrones-de-diseño-aplicados)

---

## 1. Vista Modular y Paquetes (Diagrama de Componentes)

El proyecto está descompuesto en 8 submódulos Gradle independientes, organizados para mantener un flujo de dependencias unidireccional y sin ciclos:

```mermaid
flowchart TD
    subgraph Core ["Núcleo y Modelos Comunes"]
        common["com.ingsis.common<br/>• Result&lt;T&gt;, SafeIterator&lt;T&gt;<br/>• Tokens, Positions<br/>• Jerarquía AST Node y Visitor"]
    end

    subgraph IO_Lexer ["Entrada y Tokenización"]
        charstream["com.ingsis.charstream<br/>• CharStream, CharReader<br/>• PositionTracker"]
        lexer["com.ingsis.lexer<br/>• Lexer (Maximal Munch)<br/>• PrintScriptTokenizer"]
    end

    subgraph Syntax_Semantic ["Análisis Sintáctico y Semántico"]
        parser["com.ingsis.parser<br/>• LazyTokenStream<br/>• SyntacticParser, Pratt Parser<br/>• SemanticChecker & Environment"]
    end

    subgraph Execution ["Ejecución y Herramientas"]
        interpreter["com.ingsis.interpreter<br/>• DefaultInterpreter<br/>• StatementExecutor<br/>• ExpressionEvaluator, BuiltIns"]
        formatter["com.ingsis.formatter<br/>• TokenStreamFormatter<br/>• ASTFormatter, Reglas YAML"]
        sca["com.ingsis.sca<br/>• ASTSca (Linter)<br/>• ScaNodeHandlers, Reglas YAML"]
    end

    subgraph Entrypoint ["Fachada y CLI"]
        engine["com.ingsis.engine<br/>• CliEngine (Picocli)<br/>• ExecuteService, ValidationService<br/>• FormatService, LintService"]
    end

    charstream --> common
    lexer --> common
    lexer --> charstream

    parser --> common
    parser --> charstream
    parser --> lexer

    interpreter --> common
    interpreter --> parser

    formatter --> common
    formatter --> charstream
    formatter --> lexer
    formatter --> parser

    sca --> common
    sca --> parser

    engine --> common
    engine --> charstream
    engine --> lexer
    engine --> parser
    engine --> interpreter
    engine --> formatter
    engine --> sca
```

---

## 2. Capa Base y Streaming de Entrada (`common`, `charstream`, `lexer`)

Modela la mónada `Result<T>` para el manejo funcional de fallos, el streaming perezoso con memoria constante $O(1)$ a través de `SafeIterator<T>`, el rastreo espacial de coordenadas (`PositionTracker`) y el tokenizador basado en *Maximal Munch*.

```mermaid
classDiagram
    direction TB

    class Result~T~ {
        <<sealed interface>>
        +boolean isCorrect()
        +success(T value)$ Result~T~
        +failure(String error)$ Result~T~
    }

    class CorrectResult~T~ {
        -T value
        +value() T
        +isCorrect() boolean
    }

    class IncorrectResult~T~ {
        -String error
        +error() String
        +isCorrect() boolean
    }

    Result <|.. CorrectResult
    Result <|.. IncorrectResult

    class SafeIterator~T~ {
        <<interface>>
        +next() Result~IterationStep~T~~
        +unread(T item) void
    }

    class IterationStep~T~ {
        -T value
        -SafeIterator~T~ next
        +value() T
        +next() SafeIterator~T~
    }

    SafeIterator ..> IterationStep : produce

    class CharReader {
        <<interface>>
        +read() int
        +unread(int ch) void
    }

    class StreamCharReader {
        -PushbackReader reader
        +read() int
        +unread(int ch) void
    }
    CharReader <|.. StreamCharReader

    class PositionTracker {
        -int line
        -int column
        +advance(int ch) Position
        +currentPosition() Position
    }

    class CharStream {
        -CharReader reader
        -PositionTracker tracker
        +next() Result~IterationStep~MetaCharacter~~
        +unread(MetaCharacter item) void
    }
    SafeIterator <|.. CharStream
    CharStream o-- CharReader
    CharStream o-- PositionTracker

    class Tokenizer {
        <<interface>>
        +tokenize(String lexeme) TokenizeResult
    }

    class PrintScriptTokenizer {
        -TokenTypeMatcher rootMatcher
        +tokenize(String lexeme) TokenizeResult
    }
    Tokenizer <|.. PrintScriptTokenizer

    class Lexer {
        -SafeIterator~MetaCharacter~ charStream
        -Tokenizer tokenizer
        +next() Result~IterationStep~Token~~
        +unread(Token item) void
    }
    SafeIterator <|.. Lexer
    Lexer o-- CharStream
    Lexer o-- Tokenizer
```

---

## 3. Jerarquía AST y Patrón Visitor (`common.node`)

Define el Árbol de Sintaxis Abstracta inmutable mediante Java Records y la interfaz `NodeVisitor<R, C>` para operaciones externas desacopladas (semántica, formato, linteo).

```mermaid
classDiagram
    direction TB

    class Node {
        <<interface>>
        +Integer line()
        +Integer column()
        +String symbol()
        +List~Node~ children()
        +accept(NodeVisitor~R, C~ visitor, C context) R
    }

    class ExpressionNode {
        <<interface>>
    }
    Node <|-- ExpressionNode

    class ProgramNode {
        -List~Node~ statements
        +children() List~Node~
    }
    Node <|.. ProgramNode

    class DeclarationKeywordNode {
        -DeclarationType type
        -String identifier
        -DataType dataType
        -ExpressionNode value
    }
    Node <|.. DeclarationKeywordNode

    class AssignNode {
        -String identifier
        -ExpressionNode value
    }
    Node <|.. AssignNode

    class IfKeywordNode {
        -ExpressionNode condition
        -Node thenBlock
        -Node elseBlock
    }
    Node <|.. IfKeywordNode

    class CallFunctionNode {
        -String functionName
        -List~ExpressionNode~ arguments
    }
    ExpressionNode <|.. CallFunctionNode

    class OperatorNode {
        -OperatorType operator
        -ExpressionNode left
        -ExpressionNode right
    }
    ExpressionNode <|.. OperatorNode

    class LiteralNode {
        <<interface>>
        +DataType getDataType()
    }
    ExpressionNode <|-- LiteralNode

    class NumberLiteralNode {
        -Double value
    }
    class StringLiteralNode {
        -String value
    }
    class BooleanLiteralNode {
        -Boolean value
    }
    LiteralNode <|.. NumberLiteralNode
    LiteralNode <|.. StringLiteralNode
    LiteralNode <|.. BooleanLiteralNode

    class NodeVisitor~R, C~ {
        <<interface>>
        +visit(DeclarationKeywordNode decl, C ctx) R
        +visit(AssignNode assign, C ctx) R
        +visit(IfKeywordNode ifNode, C ctx) R
        +visit(CallFunctionNode call, C ctx) R
        +visit(ProgramNode program, C ctx) R
        +visitDefault(Node node, C ctx) R
    }

    Node ..> NodeVisitor : acepta
```

---

## 4. Análisis Sintáctico y Semántico (`parser`)

Estructura el parsing compuesto de sentencias, el algoritmo **Pratt Parser** (`OperatorParser`) para expresiones aritmético-lógicas y el chequeo semántico de tipos y ámbitos (`SemanticChecker` y `SemanticEnvironment`).

```mermaid
classDiagram
    direction TB

    class TokenStream {
        <<interface>>
        +peek(int offset) Token
        +consume() Token
        +isEmpty() boolean
    }

    class LazyTokenStream {
        -SafeIterator~Token~ lexer
        -Deque~Token~ buffer
        +peek(int offset) Token
        +consume() Token
        +isEmpty() boolean
    }
    TokenStream <|.. LazyTokenStream

    class ParserFactory {
        +createParser(Version version)$ SyntacticParser
    }

    class SyntacticParser {
        -List~StatementParser~ statementParsers
        +parse(TokenStream stream) Result~Node~
    }

    class StatementParser {
        <<interface>>
        +parse(TokenStream stream) Result~Node~
    }

    class DeclarationParser
    class AssignParser
    class ConditionalParser
    class LineExpressionParser
    StatementParser <|.. DeclarationParser
    StatementParser <|.. AssignParser
    StatementParser <|.. ConditionalParser
    StatementParser <|.. LineExpressionParser

    class OperatorParser {
        +parseExpression(TokenStream stream, int minPrecedence) Result~ExpressionNode~
    }

    SyntacticParser o-- StatementParser
    LineExpressionParser o-- OperatorParser
    ParserFactory ..> SyntacticParser : construye

    class SemanticChecker {
        -Map~Class, SemanticNodeHandler~ handlers
        +visit(...) Result~SemanticStep~
    }
    NodeVisitor <|.. SemanticChecker

    class SemanticEnvironment {
        -SemanticEnvironment parent
        -Map~String, SymbolInfo~ table
        +define(String id, DataType type, boolean isConst)
        +lookup(String id) Optional~SymbolInfo~
    }
    SemanticChecker o-- SemanticEnvironment
```

---

## 5. Motor de Ejecución e Intérprete (`interpreter`)

Modela el runtime en memoria: despacho y ejecución de sentencias (`StatementExecutor`), evaluación de expresiones (`ExpressionEvaluator`), ámbito de variables (`Environment`) y registro de funciones nativas (`BuiltInFunction`).

```mermaid
classDiagram
    direction TB

    class Interpreter {
        <<interface>>
        +interpret(TokenStream stream, SemanticEnvironment semEnv, Environment runEnv) Result~SemanticEnvironment~
        +interpret(ProgramNode program) Result~Void~
    }

    class DefaultInterpreter {
        -StatementExecutor executor
        -SemanticChecker semanticChecker
        -SyntacticParser parser
        +interpret(...) Result~SemanticEnvironment~
    }
    Interpreter <|.. DefaultInterpreter

    class StatementExecutor {
        <<interface>>
        +execute(Node node, Environment env) Result~Environment~
    }

    class DefaultStatementExecutor {
        -ExpressionEvaluator evaluator
        +execute(Node node, Environment env) Result~Environment~
    }
    StatementExecutor <|.. DefaultStatementExecutor

    class ExpressionEvaluator {
        <<interface>>
        +evaluate(ExpressionNode expr, Environment env) Result~Object~
    }

    class DefaultExpressionEvaluator {
        -FunctionRegistry functionRegistry
        +evaluate(ExpressionNode expr, Environment env) Result~Object~
    }
    ExpressionEvaluator <|.. DefaultExpressionEvaluator

    class Environment {
        -Environment parent
        -Map~String, Object~ values
        -Set~String~ constants
        +get(String name) Object
        +assign(String name, Object val) void
        +declare(String name, Object val, boolean isConst) void
    }

    class FunctionRegistry {
        <<interface>>
        +find(String name) Optional~BuiltInFunction~
    }

    class BuiltInFunction {
        <<interface>>
        +execute(List~Object~ args) Result~Object~
    }

    class PrintlnFunction
    class ReadInputFunction
    class ReadEnvFunction
    BuiltInFunction <|.. PrintlnFunction
    BuiltInFunction <|.. ReadInputFunction
    BuiltInFunction <|.. ReadEnvFunction

    DefaultInterpreter o-- StatementExecutor
    DefaultInterpreter o-- SemanticChecker
    DefaultStatementExecutor o-- ExpressionEvaluator
    DefaultExpressionEvaluator o-- FunctionRegistry
    FunctionRegistry o-- BuiltInFunction
    DefaultStatementExecutor ..> Environment
```

---

## 6. Fachada, Servicios y Herramientas (`engine`, `formatter`, `sca`)

El módulo `engine` actúa como interfaz unificada (CLI mediante Picocli y servicios delegados). Además, se muestran las herramientas de formateo y análisis estático (`sca`).

```mermaid
classDiagram
    direction TB

    class Engine {
        <<interface>>
        +validate(Version v, InputStream in) Result~String~
        +interpret(Version v, OutputEmitter out, InputSupplier in, InputStream src) Result~String~
        +format(Version v, InputStream in, InputStream config, Writer out) Result~String~
        +analyze(Version v, InputStream in, InputStream config) Result~String~
    }

    class CliEngine {
        -Engine engine
        +main(String[] args)$ void
        +call() Integer
    }

    class ExecuteService {
        -ProgressListener progress
        +execute(...) Result~String~
    }
    class ValidationService {
        +validate(...) Result~String~
    }
    class FormatService {
        +format(...) Result~String~
    }
    class LintService {
        +analyze(...) Result~String~
    }

    Engine <|.. ExecuteService
    CliEngine o-- Engine
    ExecuteService ..> ValidationService
    ExecuteService ..> FormatService
    ExecuteService ..> LintService

    class ASTFormatter {
        -FormatContext context
        +format(ProgramNode program) String
    }
    NodeVisitor <|.. ASTFormatter

    class ASTSca {
        -ScaContext context
        +analyze(ProgramNode program) List~ScaReport~
    }
    NodeVisitor <|.. ASTSca

    FormatService ..> ASTFormatter
    LintService ..> ASTSca
```

---

## 7. Diagrama de Secuencia: Flujo de Ejecución de Sentencias

Representa la interacción paso a paso en tiempo de ejecución:

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant CLI as CliEngine
    participant Svc as ExecuteService
    participant Lex as Lexer
    participant Stream as LazyTokenStream
    participant Parser as SyntacticParser
    participant Pratt as OperatorParser
    participant Sem as SemanticChecker
    participant Exec as DefaultStatementExecutor
    participant Out as OutputEmitter

    Usuario->>CLI: Invocación CLI (ej: Execution -v 1.1 -i main.ps)
    CLI->>Svc: interpret(version, emitter, supplier, inputStream)
    Svc->>Lex: inicializa Lexer sobre CharStream
    Svc->>Stream: inicializa LazyTokenStream(Lexer)
    Svc->>Parser: ParserFactory.createParser(version)

    loop Mientras existan tokens en Stream (Evaluación Perezosa)
        Svc->>Parser: parse(Stream)
        Parser->>Stream: peek() / consume()
        Stream->>Lex: next() (Maximal Munch bajo demanda)
        opt Expresión con Operadores
            Parser->>Pratt: parseExpression(stream, minPrecedence)
            Pratt-->>Parser: ExpressionNode
        end
        Parser-->>Svc: Statement AST Node

        Svc->>Sem: node.accept(semanticChecker, semanticEnv)
        Sem-->>Svc: CorrectResult (Tipos y Scope válidos)

        Svc->>Exec: execute(node, runtimeEnv)
        opt Llamada a Función Built-in (ej: println)
            Exec->>Out: print("Mensaje evaluado")
        end
        Exec-->>Svc: CorrectResult(Environment actualizado)
    end

    Svc-->>CLI: Result.success("Ejecución finalizada")
    CLI-->>Usuario: Salida en consola y código 0
```

---

## 8. Matriz de Patrones de Diseño Aplicados

| Patrón de Diseño | Componentes Clave | Propósito Arquitectónico |
|---|---|---|
| **Result / Monad** | `result.Result<T>`, `CorrectResult`, `IncorrectResult` | Elimina el uso de excepciones (`throw`) para control de flujo, obligando al manejo exhaustivo de casos de error en tiempo de compilación. |
| **Iterator / Streaming** | `SafeIterator<T>`, `IterationStep<T>`, `CharStream`, `Lexer` | Lectura y tokenización perezosa bajo demanda con consumo de memoria constante $O(1)$. |
| **Maximal Munch** | `Lexer`, `PrintScriptTokenizer` | Resolución determinista de tokens con prefijos compartidos (`=` vs `==`, `let` vs identificadores). |
| **Pratt Parsing** | `OperatorParser` | Parser Top-Down con precedencia de operadores y asociatividad sin caer en recursión por izquierda. |
| **Visitor** | `NodeVisitor<R, C>`, `SemanticChecker`, `ASTFormatter`, `ASTSca` | Doble despacho polimórfico (`accept`/`visit`) que desacopla la estructura del AST de las operaciones de validación, formato y análisis. |
| **Strategy & Factory** | `ParserFactory`, `VersionStrategy`, `ConditionalElseStrategy` | Soporte multi-versión limpio (v1.0 y v1.1) inyectando parsers y reglas específicas sin sentencias condicionales intrusivas. |
| **Composite** | `Node`, `ProgramNode`, `ExpressionNode` | Modela la jerarquía sintáctica como estructuras en árbol que pueden ser tratadas uniforme y recursivamente. |
| **Facade** | `CliEngine`, `ExecuteService`, `Engine` | Punto de entrada unificado que oculta la complejidad y orquestación interna a clientes y CLI. |
