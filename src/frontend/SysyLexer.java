// Generated from D:/JavaProject/MegaSysy\Sysy.g4 by ANTLR 4.9.2
package frontend;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SysyLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		MULTIP_ANNOTATION=1, SINGLE_ANNOTATION=2, LPAREN=3, RPAREN=4, LBRACE=5, 
		RBRACE=6, LBRACKET=7, RBRACKET=8, ADD=9, SUB=10, EXC=11, MUL=12, DIV=13, 
		MOD=14, EQ=15, EEQ=16, UEQ=17, SLT=18, SLE=19, SGT=20, SGE=21, AND=22, 
		OR=23, SEMICOLON=24, COMMA=25, CONST=26, INT=27, FLOAT=28, VOID=29, IF=30, 
		ELSE=31, WHILE=32, BREAK=33, CONTINUE=34, RETURN=35, IDENT=36, Decimal_floating_constant=37, 
		Hexadecimal_floating_constant=38, DEC_INT_CONST=39, OCT_INT_CONST=40, 
		HEX_INT_CONST=41, Fractional_constant=42, Exponent_part=43, Digit_sequence=44, 
		Hexadecimal_fractional_constant=45, Binary_exponent_part=46, Hexadecimal_digit_sequence=47, 
		Nondigit=48, Digit=49, WHITE_SPACE=50;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"MULTIP_ANNOTATION", "SINGLE_ANNOTATION", "LPAREN", "RPAREN", "LBRACE", 
			"RBRACE", "LBRACKET", "RBRACKET", "ADD", "SUB", "EXC", "MUL", "DIV", 
			"MOD", "EQ", "EEQ", "UEQ", "SLT", "SLE", "SGT", "SGE", "AND", "OR", "SEMICOLON", 
			"COMMA", "CONST", "INT", "FLOAT", "VOID", "IF", "ELSE", "WHILE", "BREAK", 
			"CONTINUE", "RETURN", "IDENT", "Decimal_floating_constant", "Hexadecimal_floating_constant", 
			"DEC_INT_CONST", "OCT_INT_CONST", "HEX_INT_CONST", "Fractional_constant", 
			"Exponent_part", "Digit_sequence", "Hexadecimal_fractional_constant", 
			"Binary_exponent_part", "Hexadecimal_digit_sequence", "Nondigit", "Digit", 
			"WHITE_SPACE"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, "'('", "')'", "'{'", "'}'", "'['", "']'", "'+'", "'-'", 
			"'!'", "'*'", "'/'", "'%'", "'='", "'=='", "'!='", "'<'", "'<='", "'>'", 
			"'>='", "'&&'", "'||'", "';'", "','", "'const'", "'int'", "'float'", 
			"'void'", "'if'", "'else'", "'while'", "'break'", "'continue'", "'return'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "MULTIP_ANNOTATION", "SINGLE_ANNOTATION", "LPAREN", "RPAREN", "LBRACE", 
			"RBRACE", "LBRACKET", "RBRACKET", "ADD", "SUB", "EXC", "MUL", "DIV", 
			"MOD", "EQ", "EEQ", "UEQ", "SLT", "SLE", "SGT", "SGE", "AND", "OR", "SEMICOLON", 
			"COMMA", "CONST", "INT", "FLOAT", "VOID", "IF", "ELSE", "WHILE", "BREAK", 
			"CONTINUE", "RETURN", "IDENT", "Decimal_floating_constant", "Hexadecimal_floating_constant", 
			"DEC_INT_CONST", "OCT_INT_CONST", "HEX_INT_CONST", "Fractional_constant", 
			"Exponent_part", "Digit_sequence", "Hexadecimal_fractional_constant", 
			"Binary_exponent_part", "Hexadecimal_digit_sequence", "Nondigit", "Digit", 
			"WHITE_SPACE"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public SysyLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Sysy.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\64\u0162\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\3\2"+
		"\3\2\3\2\3\2\7\2l\n\2\f\2\16\2o\13\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3"+
		"\3\7\3z\n\3\f\3\16\3}\13\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b"+
		"\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20"+
		"\3\20\3\21\3\21\3\21\3\22\3\22\3\22\3\23\3\23\3\24\3\24\3\24\3\25\3\25"+
		"\3\26\3\26\3\26\3\27\3\27\3\27\3\30\3\30\3\30\3\31\3\31\3\32\3\32\3\33"+
		"\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35"+
		"\3\35\3\36\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3 \3 \3 \3 \3 \3!\3!\3!"+
		"\3!\3!\3!\3\"\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3#\3#\3$\3$\3$"+
		"\3$\3$\3$\3$\3%\3%\3%\7%\u00f1\n%\f%\16%\u00f4\13%\3&\3&\5&\u00f8\n&\3"+
		"&\3&\3&\5&\u00fd\n&\3\'\3\'\3\'\3\'\5\'\u0103\n\'\3\'\3\'\3\'\3\'\3\'"+
		"\3\'\3\'\5\'\u010c\n\'\3\'\3\'\3\'\5\'\u0111\n\'\3(\3(\7(\u0115\n(\f("+
		"\16(\u0118\13(\3)\3)\7)\u011c\n)\f)\16)\u011f\13)\3*\3*\3*\3*\5*\u0125"+
		"\n*\3*\6*\u0128\n*\r*\16*\u0129\3+\5+\u012d\n+\3+\3+\3+\3+\3+\5+\u0134"+
		"\n+\3,\3,\5,\u0138\n,\3,\3,\3-\6-\u013d\n-\r-\16-\u013e\3.\5.\u0142\n"+
		".\3.\3.\3.\3.\3.\5.\u0149\n.\3/\3/\5/\u014d\n/\3/\3/\3/\5/\u0152\n/\5"+
		"/\u0154\n/\3\60\6\60\u0157\n\60\r\60\16\60\u0158\3\61\3\61\3\62\3\62\3"+
		"\63\3\63\3\63\3\63\3m\2\64\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25"+
		"\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32"+
		"\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\60_\61a"+
		"\62c\63e\64\3\2\13\4\2\f\f\17\17\3\2\63;\3\2\62;\3\2\629\5\2\62;CHch\4"+
		"\2GGgg\4\2--//\5\2C\\aac|\5\2\13\f\17\17\"\"\2\u0178\2\3\3\2\2\2\2\5\3"+
		"\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2"+
		"\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3"+
		"\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'"+
		"\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63"+
		"\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2"+
		"?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3"+
		"\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2"+
		"\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2\2c\3\2\2\2\2"+
		"e\3\2\2\2\3g\3\2\2\2\5u\3\2\2\2\7\u0080\3\2\2\2\t\u0082\3\2\2\2\13\u0084"+
		"\3\2\2\2\r\u0086\3\2\2\2\17\u0088\3\2\2\2\21\u008a\3\2\2\2\23\u008c\3"+
		"\2\2\2\25\u008e\3\2\2\2\27\u0090\3\2\2\2\31\u0092\3\2\2\2\33\u0094\3\2"+
		"\2\2\35\u0096\3\2\2\2\37\u0098\3\2\2\2!\u009a\3\2\2\2#\u009d\3\2\2\2%"+
		"\u00a0\3\2\2\2\'\u00a2\3\2\2\2)\u00a5\3\2\2\2+\u00a7\3\2\2\2-\u00aa\3"+
		"\2\2\2/\u00ad\3\2\2\2\61\u00b0\3\2\2\2\63\u00b2\3\2\2\2\65\u00b4\3\2\2"+
		"\2\67\u00ba\3\2\2\29\u00be\3\2\2\2;\u00c4\3\2\2\2=\u00c9\3\2\2\2?\u00cc"+
		"\3\2\2\2A\u00d1\3\2\2\2C\u00d7\3\2\2\2E\u00dd\3\2\2\2G\u00e6\3\2\2\2I"+
		"\u00ed\3\2\2\2K\u00fc\3\2\2\2M\u0110\3\2\2\2O\u0112\3\2\2\2Q\u0119\3\2"+
		"\2\2S\u0124\3\2\2\2U\u0133\3\2\2\2W\u0135\3\2\2\2Y\u013c\3\2\2\2[\u0148"+
		"\3\2\2\2]\u0153\3\2\2\2_\u0156\3\2\2\2a\u015a\3\2\2\2c\u015c\3\2\2\2e"+
		"\u015e\3\2\2\2gh\7\61\2\2hi\7,\2\2im\3\2\2\2jl\13\2\2\2kj\3\2\2\2lo\3"+
		"\2\2\2mn\3\2\2\2mk\3\2\2\2np\3\2\2\2om\3\2\2\2pq\7,\2\2qr\7\61\2\2rs\3"+
		"\2\2\2st\b\2\2\2t\4\3\2\2\2uv\7\61\2\2vw\7\61\2\2w{\3\2\2\2xz\n\2\2\2"+
		"yx\3\2\2\2z}\3\2\2\2{y\3\2\2\2{|\3\2\2\2|~\3\2\2\2}{\3\2\2\2~\177\b\3"+
		"\2\2\177\6\3\2\2\2\u0080\u0081\7*\2\2\u0081\b\3\2\2\2\u0082\u0083\7+\2"+
		"\2\u0083\n\3\2\2\2\u0084\u0085\7}\2\2\u0085\f\3\2\2\2\u0086\u0087\7\177"+
		"\2\2\u0087\16\3\2\2\2\u0088\u0089\7]\2\2\u0089\20\3\2\2\2\u008a\u008b"+
		"\7_\2\2\u008b\22\3\2\2\2\u008c\u008d\7-\2\2\u008d\24\3\2\2\2\u008e\u008f"+
		"\7/\2\2\u008f\26\3\2\2\2\u0090\u0091\7#\2\2\u0091\30\3\2\2\2\u0092\u0093"+
		"\7,\2\2\u0093\32\3\2\2\2\u0094\u0095\7\61\2\2\u0095\34\3\2\2\2\u0096\u0097"+
		"\7\'\2\2\u0097\36\3\2\2\2\u0098\u0099\7?\2\2\u0099 \3\2\2\2\u009a\u009b"+
		"\7?\2\2\u009b\u009c\7?\2\2\u009c\"\3\2\2\2\u009d\u009e\7#\2\2\u009e\u009f"+
		"\7?\2\2\u009f$\3\2\2\2\u00a0\u00a1\7>\2\2\u00a1&\3\2\2\2\u00a2\u00a3\7"+
		">\2\2\u00a3\u00a4\7?\2\2\u00a4(\3\2\2\2\u00a5\u00a6\7@\2\2\u00a6*\3\2"+
		"\2\2\u00a7\u00a8\7@\2\2\u00a8\u00a9\7?\2\2\u00a9,\3\2\2\2\u00aa\u00ab"+
		"\7(\2\2\u00ab\u00ac\7(\2\2\u00ac.\3\2\2\2\u00ad\u00ae\7~\2\2\u00ae\u00af"+
		"\7~\2\2\u00af\60\3\2\2\2\u00b0\u00b1\7=\2\2\u00b1\62\3\2\2\2\u00b2\u00b3"+
		"\7.\2\2\u00b3\64\3\2\2\2\u00b4\u00b5\7e\2\2\u00b5\u00b6\7q\2\2\u00b6\u00b7"+
		"\7p\2\2\u00b7\u00b8\7u\2\2\u00b8\u00b9\7v\2\2\u00b9\66\3\2\2\2\u00ba\u00bb"+
		"\7k\2\2\u00bb\u00bc\7p\2\2\u00bc\u00bd\7v\2\2\u00bd8\3\2\2\2\u00be\u00bf"+
		"\7h\2\2\u00bf\u00c0\7n\2\2\u00c0\u00c1\7q\2\2\u00c1\u00c2\7c\2\2\u00c2"+
		"\u00c3\7v\2\2\u00c3:\3\2\2\2\u00c4\u00c5\7x\2\2\u00c5\u00c6\7q\2\2\u00c6"+
		"\u00c7\7k\2\2\u00c7\u00c8\7f\2\2\u00c8<\3\2\2\2\u00c9\u00ca\7k\2\2\u00ca"+
		"\u00cb\7h\2\2\u00cb>\3\2\2\2\u00cc\u00cd\7g\2\2\u00cd\u00ce\7n\2\2\u00ce"+
		"\u00cf\7u\2\2\u00cf\u00d0\7g\2\2\u00d0@\3\2\2\2\u00d1\u00d2\7y\2\2\u00d2"+
		"\u00d3\7j\2\2\u00d3\u00d4\7k\2\2\u00d4\u00d5\7n\2\2\u00d5\u00d6\7g\2\2"+
		"\u00d6B\3\2\2\2\u00d7\u00d8\7d\2\2\u00d8\u00d9\7t\2\2\u00d9\u00da\7g\2"+
		"\2\u00da\u00db\7c\2\2\u00db\u00dc\7m\2\2\u00dcD\3\2\2\2\u00dd\u00de\7"+
		"e\2\2\u00de\u00df\7q\2\2\u00df\u00e0\7p\2\2\u00e0\u00e1\7v\2\2\u00e1\u00e2"+
		"\7k\2\2\u00e2\u00e3\7p\2\2\u00e3\u00e4\7w\2\2\u00e4\u00e5\7g\2\2\u00e5"+
		"F\3\2\2\2\u00e6\u00e7\7t\2\2\u00e7\u00e8\7g\2\2\u00e8\u00e9\7v\2\2\u00e9"+
		"\u00ea\7w\2\2\u00ea\u00eb\7t\2\2\u00eb\u00ec\7p\2\2\u00ecH\3\2\2\2\u00ed"+
		"\u00f2\5a\61\2\u00ee\u00f1\5a\61\2\u00ef\u00f1\5c\62\2\u00f0\u00ee\3\2"+
		"\2\2\u00f0\u00ef\3\2\2\2\u00f1\u00f4\3\2\2\2\u00f2\u00f0\3\2\2\2\u00f2"+
		"\u00f3\3\2\2\2\u00f3J\3\2\2\2\u00f4\u00f2\3\2\2\2\u00f5\u00f7\5U+\2\u00f6"+
		"\u00f8\5W,\2\u00f7\u00f6\3\2\2\2\u00f7\u00f8\3\2\2\2\u00f8\u00fd\3\2\2"+
		"\2\u00f9\u00fa\5Y-\2\u00fa\u00fb\5W,\2\u00fb\u00fd\3\2\2\2\u00fc\u00f5"+
		"\3\2\2\2\u00fc\u00f9\3\2\2\2\u00fdL\3\2\2\2\u00fe\u00ff\7\62\2\2\u00ff"+
		"\u0103\7z\2\2\u0100\u0101\7\62\2\2\u0101\u0103\7Z\2\2\u0102\u00fe\3\2"+
		"\2\2\u0102\u0100\3\2\2\2\u0103\u0104\3\2\2\2\u0104\u0105\5[.\2\u0105\u0106"+
		"\5]/\2\u0106\u0111\3\2\2\2\u0107\u0108\7\62\2\2\u0108\u010c\7z\2\2\u0109"+
		"\u010a\7\62\2\2\u010a\u010c\7Z\2\2\u010b\u0107\3\2\2\2\u010b\u0109\3\2"+
		"\2\2\u010c\u010d\3\2\2\2\u010d\u010e\5_\60\2\u010e\u010f\5]/\2\u010f\u0111"+
		"\3\2\2\2\u0110\u0102\3\2\2\2\u0110\u010b\3\2\2\2\u0111N\3\2\2\2\u0112"+
		"\u0116\t\3\2\2\u0113\u0115\t\4\2\2\u0114\u0113\3\2\2\2\u0115\u0118\3\2"+
		"\2\2\u0116\u0114\3\2\2\2\u0116\u0117\3\2\2\2\u0117P\3\2\2\2\u0118\u0116"+
		"\3\2\2\2\u0119\u011d\7\62\2\2\u011a\u011c\t\5\2\2\u011b\u011a\3\2\2\2"+
		"\u011c\u011f\3\2\2\2\u011d\u011b\3\2\2\2\u011d\u011e\3\2\2\2\u011eR\3"+
		"\2\2\2\u011f\u011d\3\2\2\2\u0120\u0121\7\62\2\2\u0121\u0125\7z\2\2\u0122"+
		"\u0123\7\62\2\2\u0123\u0125\7Z\2\2\u0124\u0120\3\2\2\2\u0124\u0122\3\2"+
		"\2\2\u0125\u0127\3\2\2\2\u0126\u0128\t\6\2\2\u0127\u0126\3\2\2\2\u0128"+
		"\u0129\3\2\2\2\u0129\u0127\3\2\2\2\u0129\u012a\3\2\2\2\u012aT\3\2\2\2"+
		"\u012b\u012d\5Y-\2\u012c\u012b\3\2\2\2\u012c\u012d\3\2\2\2\u012d\u012e"+
		"\3\2\2\2\u012e\u012f\7\60\2\2\u012f\u0134\5Y-\2\u0130\u0131\5Y-\2\u0131"+
		"\u0132\7\60\2\2\u0132\u0134\3\2\2\2\u0133\u012c\3\2\2\2\u0133\u0130\3"+
		"\2\2\2\u0134V\3\2\2\2\u0135\u0137\t\7\2\2\u0136\u0138\t\b\2\2\u0137\u0136"+
		"\3\2\2\2\u0137\u0138\3\2\2\2\u0138\u0139\3\2\2\2\u0139\u013a\5Y-\2\u013a"+
		"X\3\2\2\2\u013b\u013d\t\4\2\2\u013c\u013b\3\2\2\2\u013d\u013e\3\2\2\2"+
		"\u013e\u013c\3\2\2\2\u013e\u013f\3\2\2\2\u013fZ\3\2\2\2\u0140\u0142\5"+
		"_\60\2\u0141\u0140\3\2\2\2\u0141\u0142\3\2\2\2\u0142\u0143\3\2\2\2\u0143"+
		"\u0144\7\60\2\2\u0144\u0149\5_\60\2\u0145\u0146\5_\60\2\u0146\u0147\7"+
		"\60\2\2\u0147\u0149\3\2\2\2\u0148\u0141\3\2\2\2\u0148\u0145\3\2\2\2\u0149"+
		"\\\3\2\2\2\u014a\u014c\7r\2\2\u014b\u014d\t\b\2\2\u014c\u014b\3\2\2\2"+
		"\u014c\u014d\3\2\2\2\u014d\u014e\3\2\2\2\u014e\u0154\5Y-\2\u014f\u0151"+
		"\7R\2\2\u0150\u0152\t\b\2\2\u0151\u0150\3\2\2\2\u0151\u0152\3\2\2\2\u0152"+
		"\u0154\3\2\2\2\u0153\u014a\3\2\2\2\u0153\u014f\3\2\2\2\u0154^\3\2\2\2"+
		"\u0155\u0157\t\6\2\2\u0156\u0155\3\2\2\2\u0157\u0158\3\2\2\2\u0158\u0156"+
		"\3\2\2\2\u0158\u0159\3\2\2\2\u0159`\3\2\2\2\u015a\u015b\t\t\2\2\u015b"+
		"b\3\2\2\2\u015c\u015d\t\4\2\2\u015dd\3\2\2\2\u015e\u015f\t\n\2\2\u015f"+
		"\u0160\3\2\2\2\u0160\u0161\b\63\2\2\u0161f\3\2\2\2\32\2m{\u00f0\u00f2"+
		"\u00f7\u00fc\u0102\u010b\u0110\u0116\u011d\u0124\u0129\u012c\u0133\u0137"+
		"\u013e\u0141\u0148\u014c\u0151\u0153\u0158\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}