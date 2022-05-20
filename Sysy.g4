// Sysy.g4
grammar Sysy;

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
FLOAT: 'float';
VOID: 'void';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
BREAK: 'break';
CONTINUE: 'continue';
RETURN: 'return';
IDENT: Nondigit (Nondigit|Digit)*;
Decimal_floating_constant:
    Fractional_constant (Exponent_part)? |
    Digit_sequence Exponent_part;
Hexadecimal_floating_constant:
    ('0x'|'0X') Hexadecimal_fractional_constant Binary_exponent_part |
    ('0x'|'0X') Hexadecimal_digit_sequence Binary_exponent_part;
DEC_INT_CONST: [1-9][0-9]*;
OCT_INT_CONST: '0'[0-7]*;
HEX_INT_CONST: ('0x'|'0X')[0-9a-fA-F]+;
Fractional_constant:
    (Digit_sequence)? '.' Digit_sequence |
    Digit_sequence '.';
Exponent_part:
    ('e'|'E') ('+'|'-')? Digit_sequence;
Digit_sequence: [0-9]+;
Hexadecimal_fractional_constant:
    (Hexadecimal_digit_sequence)? '.' Hexadecimal_digit_sequence |
    Hexadecimal_digit_sequence '.';
Binary_exponent_part:
    'p' ('+'|'-')? Digit_sequence |
    'P' ('+'|'-')? ;
Hexadecimal_digit_sequence: [0-9a-fA-F]+;
Nondigit: [_a-zA-Z];
Digit:  [0-9];
WHITE_SPACE: [ \t\n\r] -> skip; // : skip 表示解析时跳过该规则

program : compUnit;
compUnit : compUnit(decl|funcDef)|(decl|funcDef);
decl : constDecl | varDecl ;
constDecl : CONST bType constDef ( COMMA constDef )* SEMICOLON;
bType : INT | FLOAT;
constDef : IDENT (LBRACKET constExp RBRACKET )* EQ constInitVal;
constInitVal returns [ir.Type arrayType] :
    constExp | LBRACE(constInitVal (COMMA constInitVal)*)?RBRACE;
constExp : addExp ;
varDecl : bType varDef ( COMMA varDef )* SEMICOLON;
varDef : IDENT (LBRACKET constExp RBRACKET )* | IDENT (LBRACKET constExp RBRACKET)*  EQ initVal;
initVal returns [ir.Type arrayType] : exp | LBRACE (initVal (COMMA initVal)*)? RBRACE;
funcDef : funcType IDENT LPAREN (funcFParams)? RPAREN  block;
funcType : INT|VOID|FLOAT;
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
cond returns [ir.BasicBlock trueBlock, ir.BasicBlock falseBlock] : lOrExp;
lVal : IDENT (LBRACKET exp RBRACKET )*;
primaryExp : LPAREN exp RPAREN | lVal | number ;
number : iNT_CONST|fLOAT_CONST;
iNT_CONST: DEC_INT_CONST | OCT_INT_CONST | HEX_INT_CONST;
fLOAT_CONST: Decimal_floating_constant | Hexadecimal_floating_constant;
unaryExp  : primaryExp | IDENT LPAREN(funcRParams)?RPAREN | unaryOp unaryExp;
unaryOp : ADD | SUB | EXC;
funcRParams    : exp ( COMMA exp )*;
mulExp  : unaryExp | mulExp (MUL | DIV | MOD) unaryExp;
addExp  : mulExp | addExp (ADD | SUB)  mulExp;
relExp  : addExp | relExp (SLT | SGT | SLE | SGE)  addExp;
eqExp : relExp | eqExp (EEQ | UEQ)  relExp;
lAndExp returns [ir.BasicBlock trueBlock, ir.BasicBlock falseBlock] :
    eqExp | lAndExp AND eqExp;
lOrExp returns [ir.BasicBlock trueBlock, ir.BasicBlock falseBlock]  :
    lAndExp | lOrExp OR lAndExp;