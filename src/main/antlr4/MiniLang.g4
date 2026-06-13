grammar MiniLang;

program
    : statement* EOF
    ;

statement
    : varDecl
    | assignment
    | printStmt
    | ifStmt
    | doWhileStmt
    | block
    ;

varDecl
    : 'var' ID ':' type ('=' expr)? ';'
    ;

assignment
    : ID '=' expr ';'
    ;

printStmt
    : 'print' '(' expr ')' ';'
    ;

ifStmt
    : 'if' '(' expr ')' block 'else' block
    ;

doWhileStmt
    : 'do' block 'while' '(' expr ')' ';'
    ;

block
    : '{' statement* '}'
    ;

type
    : 'int'
    | 'real'
    | 'string'
    | 'bool'
    ;

expr
    : logicalOr
    ;

logicalOr
    : logicalAnd ('||' logicalAnd)*
    ;

logicalAnd
    : equality ('&&' equality)*
    ;

equality
    : comparison (('==' | '!=') comparison)*
    ;

comparison
    : additive (('<' | '<=' | '>' | '>=') additive)*
    ;

additive
    : multiplicative (('+' | '-') multiplicative)*
    ;

multiplicative
    : unary (('*' | '/') unary)*
    ;

unary
    : ('!' | '-') unary
    | primary
    ;

primary
    : INT
    | REAL
    | STRING
    | BOOL
    | ID
    | '(' expr ')'
    ;

BOOL
    : 'true'
    | 'false'
    ;

ID
    : [a-zA-Z_] [a-zA-Z_0-9]*
    ;

REAL
    : [0-9]+ '.' [0-9]+
    ;

INT
    : [0-9]+
    ;

STRING
    : '"' (ESC | ~["\\\r\n])* '"'
    ;

fragment ESC
    : '\\' ["\\/bfnrt]
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
