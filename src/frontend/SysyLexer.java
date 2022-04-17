// Generated from D:/JavaProject/AntlrMega\Sysy.g4 by ANTLR 4.9.2
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
		OR=23, SEMICOLON=24, COMMA=25, CONST=26, INT=27, VOID=28, IF=29, ELSE=30, 
		WHILE=31, BREAK=32, CONTINUE=33, RETURN=34, IDENT=35, NUMBER=36, Nondigit=37, 
		Digit=38, WHITE_SPACE=39;
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
			"COMMA", "CONST", "INT", "VOID", "IF", "ELSE", "WHILE", "BREAK", "CONTINUE", 
			"RETURN", "IDENT", "NUMBER", "Nondigit", "Digit", "WHITE_SPACE"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, "'('", "')'", "'{'", "'}'", "'['", "']'", "'+'", "'-'", 
			"'!'", "'*'", "'/'", "'%'", "'='", "'=='", "'!='", "'<'", "'<='", "'>'", 
			"'>='", "'&&'", "'||'", "';'", "','", "'const'", "'int'", "'void'", "'if'", 
			"'else'", "'while'", "'break'", "'continue'", "'return'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "MULTIP_ANNOTATION", "SINGLE_ANNOTATION", "LPAREN", "RPAREN", "LBRACE", 
			"RBRACE", "LBRACKET", "RBRACKET", "ADD", "SUB", "EXC", "MUL", "DIV", 
			"MOD", "EQ", "EEQ", "UEQ", "SLT", "SLE", "SGT", "SGE", "AND", "OR", "SEMICOLON", 
			"COMMA", "CONST", "INT", "VOID", "IF", "ELSE", "WHILE", "BREAK", "CONTINUE", 
			"RETURN", "IDENT", "NUMBER", "Nondigit", "Digit", "WHITE_SPACE"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2)\u00fc\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\3\2\3\2\3\2\3\2\7\2"+
		"V\n\2\f\2\16\2Y\13\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\7\3d\n\3\f\3"+
		"\16\3g\13\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3"+
		"\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3"+
		"\21\3\21\3\22\3\22\3\22\3\23\3\23\3\24\3\24\3\24\3\25\3\25\3\26\3\26\3"+
		"\26\3\27\3\27\3\27\3\30\3\30\3\30\3\31\3\31\3\32\3\32\3\33\3\33\3\33\3"+
		"\33\3\33\3\33\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3"+
		"\36\3\37\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3 \3 \3!\3!\3!\3!\3!\3!\3\"\3"+
		"\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3$\3$\3$\7$\u00d5"+
		"\n$\f$\16$\u00d8\13$\3%\3%\7%\u00dc\n%\f%\16%\u00df\13%\3%\3%\7%\u00e3"+
		"\n%\f%\16%\u00e6\13%\3%\3%\3%\3%\5%\u00ec\n%\3%\6%\u00ef\n%\r%\16%\u00f0"+
		"\5%\u00f3\n%\3&\3&\3\'\3\'\3(\3(\3(\3(\3W\2)\3\3\5\4\7\5\t\6\13\7\r\b"+
		"\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26"+
		"+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O)\3\2"+
		"\t\4\2\f\f\17\17\3\2\63;\3\2\62;\3\2\629\5\2\62;CHch\5\2C\\aac|\5\2\13"+
		"\f\17\17\"\"\2\u0105\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2"+
		"\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3"+
		"\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2"+
		"\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2"+
		"\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2"+
		"\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2"+
		"\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\3Q"+
		"\3\2\2\2\5_\3\2\2\2\7j\3\2\2\2\tl\3\2\2\2\13n\3\2\2\2\rp\3\2\2\2\17r\3"+
		"\2\2\2\21t\3\2\2\2\23v\3\2\2\2\25x\3\2\2\2\27z\3\2\2\2\31|\3\2\2\2\33"+
		"~\3\2\2\2\35\u0080\3\2\2\2\37\u0082\3\2\2\2!\u0084\3\2\2\2#\u0087\3\2"+
		"\2\2%\u008a\3\2\2\2\'\u008c\3\2\2\2)\u008f\3\2\2\2+\u0091\3\2\2\2-\u0094"+
		"\3\2\2\2/\u0097\3\2\2\2\61\u009a\3\2\2\2\63\u009c\3\2\2\2\65\u009e\3\2"+
		"\2\2\67\u00a4\3\2\2\29\u00a8\3\2\2\2;\u00ad\3\2\2\2=\u00b0\3\2\2\2?\u00b5"+
		"\3\2\2\2A\u00bb\3\2\2\2C\u00c1\3\2\2\2E\u00ca\3\2\2\2G\u00d1\3\2\2\2I"+
		"\u00f2\3\2\2\2K\u00f4\3\2\2\2M\u00f6\3\2\2\2O\u00f8\3\2\2\2QR\7\61\2\2"+
		"RS\7,\2\2SW\3\2\2\2TV\13\2\2\2UT\3\2\2\2VY\3\2\2\2WX\3\2\2\2WU\3\2\2\2"+
		"XZ\3\2\2\2YW\3\2\2\2Z[\7,\2\2[\\\7\61\2\2\\]\3\2\2\2]^\b\2\2\2^\4\3\2"+
		"\2\2_`\7\61\2\2`a\7\61\2\2ae\3\2\2\2bd\n\2\2\2cb\3\2\2\2dg\3\2\2\2ec\3"+
		"\2\2\2ef\3\2\2\2fh\3\2\2\2ge\3\2\2\2hi\b\3\2\2i\6\3\2\2\2jk\7*\2\2k\b"+
		"\3\2\2\2lm\7+\2\2m\n\3\2\2\2no\7}\2\2o\f\3\2\2\2pq\7\177\2\2q\16\3\2\2"+
		"\2rs\7]\2\2s\20\3\2\2\2tu\7_\2\2u\22\3\2\2\2vw\7-\2\2w\24\3\2\2\2xy\7"+
		"/\2\2y\26\3\2\2\2z{\7#\2\2{\30\3\2\2\2|}\7,\2\2}\32\3\2\2\2~\177\7\61"+
		"\2\2\177\34\3\2\2\2\u0080\u0081\7\'\2\2\u0081\36\3\2\2\2\u0082\u0083\7"+
		"?\2\2\u0083 \3\2\2\2\u0084\u0085\7?\2\2\u0085\u0086\7?\2\2\u0086\"\3\2"+
		"\2\2\u0087\u0088\7#\2\2\u0088\u0089\7?\2\2\u0089$\3\2\2\2\u008a\u008b"+
		"\7>\2\2\u008b&\3\2\2\2\u008c\u008d\7>\2\2\u008d\u008e\7?\2\2\u008e(\3"+
		"\2\2\2\u008f\u0090\7@\2\2\u0090*\3\2\2\2\u0091\u0092\7@\2\2\u0092\u0093"+
		"\7?\2\2\u0093,\3\2\2\2\u0094\u0095\7(\2\2\u0095\u0096\7(\2\2\u0096.\3"+
		"\2\2\2\u0097\u0098\7~\2\2\u0098\u0099\7~\2\2\u0099\60\3\2\2\2\u009a\u009b"+
		"\7=\2\2\u009b\62\3\2\2\2\u009c\u009d\7.\2\2\u009d\64\3\2\2\2\u009e\u009f"+
		"\7e\2\2\u009f\u00a0\7q\2\2\u00a0\u00a1\7p\2\2\u00a1\u00a2\7u\2\2\u00a2"+
		"\u00a3\7v\2\2\u00a3\66\3\2\2\2\u00a4\u00a5\7k\2\2\u00a5\u00a6\7p\2\2\u00a6"+
		"\u00a7\7v\2\2\u00a78\3\2\2\2\u00a8\u00a9\7x\2\2\u00a9\u00aa\7q\2\2\u00aa"+
		"\u00ab\7k\2\2\u00ab\u00ac\7f\2\2\u00ac:\3\2\2\2\u00ad\u00ae\7k\2\2\u00ae"+
		"\u00af\7h\2\2\u00af<\3\2\2\2\u00b0\u00b1\7g\2\2\u00b1\u00b2\7n\2\2\u00b2"+
		"\u00b3\7u\2\2\u00b3\u00b4\7g\2\2\u00b4>\3\2\2\2\u00b5\u00b6\7y\2\2\u00b6"+
		"\u00b7\7j\2\2\u00b7\u00b8\7k\2\2\u00b8\u00b9\7n\2\2\u00b9\u00ba\7g\2\2"+
		"\u00ba@\3\2\2\2\u00bb\u00bc\7d\2\2\u00bc\u00bd\7t\2\2\u00bd\u00be\7g\2"+
		"\2\u00be\u00bf\7c\2\2\u00bf\u00c0\7m\2\2\u00c0B\3\2\2\2\u00c1\u00c2\7"+
		"e\2\2\u00c2\u00c3\7q\2\2\u00c3\u00c4\7p\2\2\u00c4\u00c5\7v\2\2\u00c5\u00c6"+
		"\7k\2\2\u00c6\u00c7\7p\2\2\u00c7\u00c8\7w\2\2\u00c8\u00c9\7g\2\2\u00c9"+
		"D\3\2\2\2\u00ca\u00cb\7t\2\2\u00cb\u00cc\7g\2\2\u00cc\u00cd\7v\2\2\u00cd"+
		"\u00ce\7w\2\2\u00ce\u00cf\7t\2\2\u00cf\u00d0\7p\2\2\u00d0F\3\2\2\2\u00d1"+
		"\u00d6\5K&\2\u00d2\u00d5\5K&\2\u00d3\u00d5\5M\'\2\u00d4\u00d2\3\2\2\2"+
		"\u00d4\u00d3\3\2\2\2\u00d5\u00d8\3\2\2\2\u00d6\u00d4\3\2\2\2\u00d6\u00d7"+
		"\3\2\2\2\u00d7H\3\2\2\2\u00d8\u00d6\3\2\2\2\u00d9\u00dd\t\3\2\2\u00da"+
		"\u00dc\t\4\2\2\u00db\u00da\3\2\2\2\u00dc\u00df\3\2\2\2\u00dd\u00db\3\2"+
		"\2\2\u00dd\u00de\3\2\2\2\u00de\u00f3\3\2\2\2\u00df\u00dd\3\2\2\2\u00e0"+
		"\u00e4\7\62\2\2\u00e1\u00e3\t\5\2\2\u00e2\u00e1\3\2\2\2\u00e3\u00e6\3"+
		"\2\2\2\u00e4\u00e2\3\2\2\2\u00e4\u00e5\3\2\2\2\u00e5\u00f3\3\2\2\2\u00e6"+
		"\u00e4\3\2\2\2\u00e7\u00e8\7\62\2\2\u00e8\u00ec\7z\2\2\u00e9\u00ea\7\62"+
		"\2\2\u00ea\u00ec\7Z\2\2\u00eb\u00e7\3\2\2\2\u00eb\u00e9\3\2\2\2\u00ec"+
		"\u00ee\3\2\2\2\u00ed\u00ef\t\6\2\2\u00ee\u00ed\3\2\2\2\u00ef\u00f0\3\2"+
		"\2\2\u00f0\u00ee\3\2\2\2\u00f0\u00f1\3\2\2\2\u00f1\u00f3\3\2\2\2\u00f2"+
		"\u00d9\3\2\2\2\u00f2\u00e0\3\2\2\2\u00f2\u00eb\3\2\2\2\u00f3J\3\2\2\2"+
		"\u00f4\u00f5\t\7\2\2\u00f5L\3\2\2\2\u00f6\u00f7\t\4\2\2\u00f7N\3\2\2\2"+
		"\u00f8\u00f9\t\b\2\2\u00f9\u00fa\3\2\2\2\u00fa\u00fb\b(\2\2\u00fbP\3\2"+
		"\2\2\f\2We\u00d4\u00d6\u00dd\u00e4\u00eb\u00f0\u00f2\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}