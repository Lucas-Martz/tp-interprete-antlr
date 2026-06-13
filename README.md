# Trabajo Practico ANTLR - Interprete MiniLang

Variante asignada: **3 - do-while**.

Este proyecto implementa un interprete simple en Java usando **ANTLR4** y el patron **Visitor**. El lenguaje implementado se llama MiniLang y permite declarar variables, asignar valores, evaluar expresiones, imprimir por consola, usar condicionales `if-else` y ejecutar la estructura diferencial `do-while`.

## Integrantes

- Mansilla Santiago - 41921928
- Martinez Lucas - 43049098

## Lenguaje implementado

MiniLang soporta:

- Tipos de datos: `int`, `real`, `string` y `bool`.
- Declaracion de variables:

```minilang
var nombre : int = 10;
var precio : real = 25.5;
var texto : string = "hola";
var activo : bool = true;
```

- Asignacion:

```minilang
nombre = nombre + 1;
```

- Impresion por consola:

```minilang
print(nombre);
print("mensaje");
```

- Condicional:

```minilang
if (nombre > 5) {
    print("mayor");
} else {
    print("menor o igual");
}
```

- Variante diferencial `do-while`:

```minilang
do {
    print(nombre);
    nombre = nombre + 1;
} while (nombre < 15);
```

- Comentarios:

```minilang
// comentario de linea

/*
   comentario de bloque
*/
```

## Operadores soportados

Operadores aritmeticos:

```text
+  -  *  /
```

Operadores relacionales:

```text
<  <=  >  >=  ==  !=
```

Operadores logicos:

```text
&&  ||  !
```

## Estructura del proyecto

```text
tp-interprete-antlr/
|-- pom.xml
|-- README.md
|-- examples/
|   |-- do_while.ml
|   `-- error_semantico.ml
`-- src/
    `-- main/
        |-- antlr4/
        |   `-- MiniLang.g4
        `-- java/
            |-- Main.java
            |-- Interpreter.java
            |-- SemanticAnalyzer.java
            |-- SemanticException.java
            |-- SymbolTable.java
            |-- ThrowingErrorListener.java
            |-- Type.java
            `-- Value.java
```
## Decisiones de diseno

- Se definio una sintaxis simple y cercana a lenguajes imperativos conocidos, usando bloques con `{ }`, instrucciones terminadas en `;` y condiciones entre parentesis.

- Las variables se declaran con la forma `var nombre : tipo = expresion;` para que el tipo quede explicito y sea mas simple realizar el analisis semantico.

- La inicializacion de variables es opcional. Si una variable se declara sin valor inicial, el interprete le asigna un valor por defecto segun su tipo: `0` para `int`, `0.0` para `real`, cadena vacia para `string` y `false` para `bool`.

- Se implemento tipado estatico basico. Antes de ejecutar el programa, el analizador semantico verifica que las variables existan, que no se redeclaren y que las operaciones usen tipos compatibles.

- Se permite promocion de `int` a `real`, por ejemplo al asignar un entero a una variable real o al operar un entero con un real.

- Se decidio usar ANTLR con Visitor porque permite recorrer el arbol sintactico de forma ordenada, separando el analisis semantico de la ejecucion.

- El proyecto separa responsabilidades en distintas clases: `SemanticAnalyzer` valida el programa, `Interpreter` lo ejecuta, `SymbolTable` guarda variables y tipos, y `Value` representa valores en tiempo de ejecucion.

- La variante asignada fue `do-while`, por lo que se implemento una estructura que ejecuta primero el bloque y luego evalua la condicion. La condicion debe ser booleana.

- Los mensajes de error se muestran de forma clara para distinguir errores sintacticos, errores semanticos y errores de ejecucion.

## Como esta construido

### `pom.xml`

Configura el proyecto Maven. Incluye:

- Java 17 como version de compilacion.
- Dependencia `antlr4-runtime`.
- Plugin `antlr4-maven-plugin`, encargado de generar las clases del parser desde la gramatica.
- Plugin `exec-maven-plugin`, usado para ejecutar la clase `Main`.

Cuando se ejecuta Maven, ANTLR genera automaticamente clases como:

```text
MiniLangLexer
MiniLangParser
MiniLangBaseVisitor
MiniLangVisitor
```

Estas clases se generan dentro de `target/`, por eso no se escriben manualmente ni se entregan.

### `MiniLang.g4`

Es la gramatica del lenguaje. Define:

- La estructura general del programa.
- Las instrucciones validas.
- Declaraciones y asignaciones.
- `print`.
- `if-else`.
- `do-while`.
- Expresiones aritmeticas, relacionales y logicas.
- Tokens para enteros, reales, strings, booleanos, identificadores y comentarios.

La regla principal es:

```antlr
program
    : statement* EOF
    ;
```

La variante `do-while` esta definida por:

```antlr
doWhileStmt
    : 'do' block 'while' '(' expr ')' ';'
    ;
```

### `Main.java`

Es el punto de entrada del programa.

Su flujo es:

1. Recibe por argumento la ruta de un archivo `.ml`.
2. Lee el archivo fuente.
3. Crea el lexer de ANTLR.
4. Crea el parser de ANTLR.
5. Genera el arbol sintactico.
6. Ejecuta el analisis semantico.
7. Ejecuta el interprete.

Tambien captura errores de lectura, errores sintacticos y errores semanticos para mostrarlos de forma clara por consola.

### `ThrowingErrorListener.java`

Reemplaza el manejo de errores por defecto de ANTLR.

Si hay un error sintactico, lanza una excepcion con un mensaje como:

```text
Error sintactico en linea X:Y - mensaje
```

### `SemanticAnalyzer.java`

Realiza el analisis semantico antes de ejecutar el programa.

Valida:

- Uso de variables no declaradas.
- Redeclaracion de variables.
- Compatibilidad de tipos en declaraciones.
- Compatibilidad de tipos en asignaciones.
- Operaciones invalidas entre tipos incompatibles.
- Condiciones booleanas en `if`.
- Condiciones booleanas en `do-while`.
- Division por cero cuando el cero aparece de forma literal.

Ejemplo de error detectado:

```minilang
var n : int = 0;

do {
    print(n);
} while (n);
```

La condicion del `do-while` debe ser `bool`, pero `n` es `int`.

### `Interpreter.java`

Ejecuta el programa una vez que ya paso el analisis semantico.

Se encarga de:

- Ejecutar declaraciones.
- Ejecutar asignaciones.
- Evaluar expresiones.
- Imprimir valores.
- Ejecutar bloques.
- Ejecutar `if-else`.
- Ejecutar `do-while`.
- Detectar division por cero durante la ejecucion cuando el valor se calcula en tiempo de ejecucion.

El `do-while` se ejecuta asi:

1. Ejecuta primero el bloque del `do`.
2. Evalua la condicion del `while`.
3. Si la condicion es verdadera, repite.
4. Si la condicion es falsa, termina.

Esto garantiza que el bloque se ejecute al menos una vez.

### `SymbolTable.java`

Es la tabla de simbolos.

Guarda:

- El tipo declarado de cada variable.
- El valor actual de cada variable.

Permite:

- Declarar variables.
- Consultar si una variable existe.
- Obtener el tipo de una variable.
- Obtener el valor de una variable.
- Asignar nuevos valores validando compatibilidad de tipos.

### `Type.java`

Enumera los tipos del lenguaje:

```java
INT, REAL, STRING, BOOL, VOID
```

`VOID` se usa internamente para instrucciones que no devuelven valor.

### `Value.java`

Representa un valor en tiempo de ejecucion.

Guarda:

- El tipo del valor.
- El dato concreto.

Tambien contiene metodos para convertir o leer valores como:

- `asInt()`
- `asReal()`
- `asBool()`
- `asString()`
- `castTo(...)`

### `SemanticException.java`

Es una excepcion propia del proyecto para informar errores semanticos.

## Flujo completo del interprete

El funcionamiento general es:

```text
Archivo .ml
   |
   v
Lexer de ANTLR
   |
   v
Parser de ANTLR
   |
   v
Arbol sintactico
   |
   v
SemanticAnalyzer
   |
   v
Interpreter
   |
   v
Salida por consola
```

## Requisitos

- Java 17 o superior.
- Eclipse con soporte Maven.
- Maven disponible desde Eclipse.

En consola, si Maven esta instalado y agregado al PATH, tambien se puede usar `mvn`.

## Como hacerlo funcionar en Eclipse

### 1. Importar el proyecto

Abrir Eclipse y elegir:

```text
File > Import...
```

Despues seleccionar:

```text
Maven > Existing Maven Projects
```

En `Root Directory`, seleccionar la carpeta del proyecto:

```text
F:\Lucas\Eclipse\tp-interprete-antlr
```

Eclipse deberia detectar el archivo `pom.xml`. Marcar el proyecto y presionar `Finish`.

### 2. Actualizar Maven

Hacer clic derecho sobre el proyecto:

```text
Maven > Update Project...
```

Luego presionar `OK`.

Esto hace que Eclipse lea el `pom.xml`, descargue o use las dependencias necesarias y configure las fuentes generadas por ANTLR.

### 3. Generar fuentes y compilar

Hacer clic derecho sobre el proyecto:

```text
Run As > Maven build...
```

En el campo `Goals`, escribir:

```text
clean generate-sources compile
```

Presionar `Run`.
 
Si todo esta correcto, Maven genera las clases de ANTLR y compila el proyecto.

### 4. Ejecutar el ejemplo correcto

Hacer clic derecho sobre el proyecto:

```text
Run As > Maven build...
```

En `Goals`, escribir:

```text
exec:java -Dexec.args="examples/do_while.ml"
```

Salida esperada:

```text
0
1
2
3
4
Suma correcta
```

### 5. Ejecutar el ejemplo con error semantico

Hacer clic derecho sobre el proyecto:

```text
Run As > Maven build...
```

En `Goals`, escribir:

```text
exec:java -Dexec.args="examples/error_semantico.ml"
```

Salida esperada:

```text
Error semantico en linea 3: La condicion del do-while debe ser booleana. Tipo recibido: INT.
```

## Como ejecutarlo desde consola

Si Maven esta instalado en consola:

```bash
mvn clean generate-sources compile
mvn exec:java -Dexec.args="examples/do_while.ml"
```

Para probar el error semantico:

```bash
mvn exec:java -Dexec.args="examples/error_semantico.ml"
```

## Ejemplo principal

Archivo: `examples/do_while.ml`

```minilang
// Variante asignada: do-while.
var n : int = 0;
var suma : int = 0;

do {
    print(n);
    suma = suma + n;
    n = n + 1;
} while (n < 5);

if (suma == 10) {
    print("Suma correcta");
} else {
    print("Suma incorrecta");
}
```

Este ejemplo:

1. Declara `n` y `suma`.
2. Ejecuta un `do-while`.
3. Imprime los valores de `n` desde `0` hasta `4`.
4. Suma esos valores.
5. Verifica con un `if-else` que la suma sea `10`.

## Archivos que no se entregan

No incluir:

```text
target/
bin/
.settings/
*.class
```

Estos archivos son generados por Maven, Eclipse o el compilador Java.

El codigo fuente real del trabajo esta en:

```text
src/main/antlr4/
src/main/java/
examples/
pom.xml
README.md
```
