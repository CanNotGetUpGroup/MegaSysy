// Sysy.g4
grammar Sysy;

// calc.g4
MULTIP_ANNOTATION: '/*' .*? '*/' -> skip;//多行注释
SINGLE_ANNOTATION: ('//') ~[\r\n]* -> skip;//单行注释
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACKET: '[';
RBRACKET: ']';
ADD: '+';
SUB: '-';
EXC: '!';
MUL: '*';
DIV: '/';
MOD: '%';
EQ: '=';
EEQ: '==';
UEQ: '!=';
SLT: '<';
SLE: '<=';
SGT: '>';
SGE: '>=';
AND: '&&';
OR: '||';
SEMICOLON: ';';
COMMA: ',';
CONST: 'const';
INT: 'int';
VOID: 'void';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
BREAK: 'break';
CONTINUE: 'continue';
RETURN: 'return';
IDENT: Nondigit (Nondigit|Digit)*;
NUMBER: [1-9][0-9]*|'0'[0-7]*|('0x'|'0X')[0-9a-fA-F]+;
Nondigit: [_a-zA-Z];
Digit:  [0-9];
WHITE_SPACE: [ \t\n\r] -> skip; // : skip 表示解析时跳过该规则

// calc.g4
program : compUnit;
compUnit : compUnit(decl|funcDef)|(decl|funcDef);
decl : constDecl | varDecl ;
constDecl : CONST bType constDef ( COMMA constDef )* SEMICOLON;
bType : INT;
constDef : IDENT (LBRACKET constExp RBRACKET )* EQ constInitVal;
constInitVal :
    constExp | LBRACE(constInitVal (COMMA constInitVal)*)?RBRACE;
constExp : addExp ;
varDecl : bType varDef ( COMMA varDef )* SEMICOLON;
varDef : IDENT (LBRACKET constExp RBRACKET )* | IDENT (LBRACKET constExp RBRACKET)*  EQ initVal;
initVal : exp |LBRACE (initVal (COMMA initVal)*)? RBRACE;
funcDef : funcType IDENT LPAREN (funcFParams)? RPAREN  block;
funcType : INT|VOID;
funcFParams : funcFParam ( COMMA funcFParam )*;
funcFParam : bType IDENT  (LBRACKET RBRACKET ( LBRACKET exp RBRACKET )*)?;
block : LBRACE ( blockItem )* RBRACE;
blockItem: decl | stmt;
stmt:lVal EQ exp SEMICOLON
    | block
    | (exp)? SEMICOLON
    | IF LPAREN cond RPAREN stmt ( ELSE stmt)?
    | WHILE LPAREN  cond RPAREN  stmt
    | BREAK SEMICOLON
    | CONTINUE SEMICOLON
    | RETURN (exp)? SEMICOLON ;
exp : addExp;
cond : lOrExp; // [new]
lVal : IDENT (LBRACKET exp RBRACKET )*;
primaryExp : LPAREN exp RPAREN | lVal | NUMBER ;
unaryExp  : primaryExp | IDENT LPAREN(funcRParams)?RPAREN | unaryOp unaryExp;
unaryOp : ADD | SUB | EXC;  // 保证 '!' 只出现在 cond 中 [changed]
funcRParams    : exp ( COMMA exp )*;
mulExp  : unaryExp | mulExp (MUL | DIV | MOD) unaryExp;
addExp  : mulExp | addExp (ADD | SUB)  mulExp;
relExp  : addExp | relExp (SLT | SGT | SLE | SGE)  addExp; // [new]
eqExp : relExp | eqExp (EEQ | UEQ)  relExp;  // [new]
lAndExp : eqExp | lAndExp AND eqExp;  // [new]
lOrExp  : lAndExp | lOrExp OR lAndExp;  // [new]