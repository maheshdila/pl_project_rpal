import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

enum TokenType {
    IDENTIFIER,
    INTEGER,
    STRING,
    OPERATOR,
    DELETE,
    L_PAREN,
    R_PAREN,
    SEMICOLON,
    COMMA,
    RESERVED; // This is a special token type for reserved words, such as "let" and "in"
}

class Token {
    private TokenType type;
    private String value;
    private int sourceLineNumber;
    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getSourceLineNumber() {
        return sourceLineNumber;
    }

    public void setSourceLineNumber(int sourceLineNumber) {
        this.sourceLineNumber = sourceLineNumber;
    }



    @Override
    public String toString() {
        return "Token[type=" + type + ", value='" + value + "', line=" + sourceLineNumber + "]";
    }
}





class LexicalRegexPatterns {
    // Regular expressions that define patterns for different types of tokens in the RPAL
    // language.
    private static final String LETTER_REGEX = "a-zA-Z";
    private static final String DIGIT_REGEX = "\\d";
    private static final String SPACE_REGEX = "[\\s\\t\\n]";
    private static final String PUNCTUATION_REGEX = "();,";
    private static final String OPSYMBOL_REGEX = "+-/~:=|!#%_{}\"*<>.&$^\\[\\]?@";
    private static final String OPSYMBOL_TO_ESCAPE = "([*<>.&$^?])";

    // These lines of code define regular expression patterns using the `Pattern` class from the
    // `java.util.regex` package. These patterns are used by the scanner to tokenize the input
    // according to the RPAL language's lexical grammar.
    public static final Pattern LETTER_PATTERN = Pattern.compile("[" + LETTER_REGEX + "]");
    public static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[" + LETTER_REGEX + DIGIT_REGEX + "_]");
    public static final Pattern DIGIT_PATTERN = Pattern.compile(DIGIT_REGEX);
    public static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[" + PUNCTUATION_REGEX + "]");

    // These lines of code define regular expression patterns for different types of tokens in the RPAL
    // language.
    public static final String opsymbolRegex = "[" + escapeMetaChars(OPSYMBOL_REGEX, OPSYMBOL_TO_ESCAPE) + "]";
    public static final Pattern OPSYMBOL_PATTERN = Pattern.compile(opsymbolRegex);
    public static final Pattern STRING_PATTERN = Pattern.compile("[ \\t\\n\\\\" + PUNCTUATION_REGEX + LETTER_REGEX + DIGIT_REGEX + escapeMetaChars(OPSYMBOL_REGEX, OPSYMBOL_TO_ESCAPE) + "]");
    public static final Pattern SPACE_PATTERN = Pattern.compile(SPACE_REGEX);

    public static final Pattern COMMENT_PATTERN = Pattern.compile("[ \\t\\'\\\\ \\r" + PUNCTUATION_REGEX + LETTER_REGEX + DIGIT_REGEX + escapeMetaChars(OPSYMBOL_REGEX, OPSYMBOL_TO_ESCAPE) + "]"); //the \\r is for Windows LF; not really required since we're targeting *nix systems

    private static String escapeMetaChars(String inputString, String charsToEscape){
        return inputString.replaceAll(charsToEscape, "\\\\\\\\$1");
    }
}

public class Scanner {
    private BufferedReader buffer;
    private String extraCharRead;
    private int sourceLineNumber;

    public Scanner(String inputFile) throws IOException {
        sourceLineNumber = 1;
        buffer = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputFile))));
    }

    public boolean hasMoreTokens() {
        return extraCharRead != null || buffer != null;
    }
    public Token readNextToken() {
        Token nextToken = null;
        String nextChar;
        if (extraCharRead != null) {
            nextChar = extraCharRead;
            extraCharRead = null;
        } else {
            nextChar = readNextChar();
        }
        if (nextChar != null) {
            nextToken = buildToken(nextChar);
        }
        return nextToken;
    }

    private String readNextChar() {
        String nextChar = null;
        try {
            int c = buffer.read();
            if (c != -1) {
                nextChar = Character.toString((char) c);
                if (nextChar.equals("\n")) sourceLineNumber++;
            } else {
                buffer.close();
            }
        } catch (IOException e) {
            // Handle exception (if needed)
        }
        return nextChar;
    }

    private Token buildToken(String currentChar) {
        Token nextToken = null;
        if (LexicalRegexPatterns.LETTER_PATTERN.matcher(currentChar).matches()) {
            nextToken = buildIdentifierToken(currentChar);
        } else if (LexicalRegexPatterns.DIGIT_PATTERN.matcher(currentChar).matches()) {
            nextToken = buildIntegerToken(currentChar);
        } else if (LexicalRegexPatterns.OPSYMBOL_PATTERN.matcher(currentChar).matches()) {
            nextToken = buildOperatorToken(currentChar);
        } else if (currentChar.equals("\'")) {
            nextToken = buildStringToken(currentChar);
        } else if (LexicalRegexPatterns.SPACE_PATTERN.matcher(currentChar).matches()) {
            nextToken = buildSpaceToken(currentChar);
        } else if (LexicalRegexPatterns.PUNCTUATION_PATTERN.matcher(currentChar).matches()) {
            nextToken = buildPunctuationToken(currentChar);
        }
        return nextToken;
    }

    private Token buildIdentifierToken(String currentChar) {
        Token identifierToken = new Token();
        identifierToken.setType(TokenType.IDENTIFIER);
        identifierToken.setSourceLineNumber(sourceLineNumber);
        StringBuilder sBuilder = new StringBuilder(currentChar);

        String nextChar = readNextChar();
        while (nextChar != null) {
            if (LexicalRegexPatterns.IDENTIFIER_PATTERN.matcher(nextChar).matches()) {
                sBuilder.append(nextChar);
                nextChar = readNextChar();
            } else {
                extraCharRead = nextChar;
                break;
            }
        }

        String value = sBuilder.toString();
        identifierToken.setValue(value);
        return identifierToken;
    }

    private Token buildIntegerToken(String currentChar) {
        Token integerToken = new Token();
        integerToken.setType(TokenType.INTEGER);
        integerToken.setSourceLineNumber(sourceLineNumber);
        StringBuilder sBuilder = new StringBuilder(currentChar);

        String nextChar = readNextChar();
        while (nextChar != null) {
            if (LexicalRegexPatterns.DIGIT_PATTERN.matcher(nextChar).matches()) {
                sBuilder.append(nextChar);
                nextChar = readNextChar();
            } else {
                extraCharRead = nextChar;
                break;
            }
        }

        integerToken.setValue(sBuilder.toString());
        return integerToken;
    }

    private Token buildOperatorToken(String currentChar) {
        Token opSymbolToken = new Token();
        opSymbolToken.setType(TokenType.OPERATOR);
        opSymbolToken.setSourceLineNumber(sourceLineNumber);
        StringBuilder sBuilder = new StringBuilder(currentChar);

        String nextChar = readNextChar();

        if (currentChar.equals("/") && nextChar.equals("/"))
            return buildCommentToken(currentChar + nextChar);

        while (nextChar != null) {
            if (LexicalRegexPatterns.OPSYMBOL_PATTERN.matcher(nextChar).matches()) {
                sBuilder.append(nextChar);
                nextChar = readNextChar();
            } else {
                extraCharRead = nextChar;
                break;
            }
        }

        opSymbolToken.setValue(sBuilder.toString());
        return opSymbolToken;
    }

    private Token buildStringToken(String currentChar) {
        Token stringToken = new Token();
        stringToken.setType(TokenType.STRING);
        stringToken.setSourceLineNumber(sourceLineNumber);
        StringBuilder sBuilder = new StringBuilder("");

        String nextChar = readNextChar();
        while (nextChar != null) {
            if (nextChar.equals("\'")) {
                stringToken.setValue(sBuilder.toString());
                return stringToken;
            } else if (LexicalRegexPatterns.STRING_PATTERN.matcher(nextChar).matches()) {
                sBuilder.append(nextChar);
                nextChar = readNextChar();
            }
        }

        return null;
    }

    private Token buildSpaceToken(String currentChar) {
        Token deleteToken = new Token();
        deleteToken.setType(TokenType.DELETE);
        deleteToken.setSourceLineNumber(sourceLineNumber);
        StringBuilder sBuilder = new StringBuilder(currentChar);

        String nextChar = readNextChar();
        while (nextChar != null) {
            if (LexicalRegexPatterns.SPACE_PATTERN.matcher(nextChar).matches()) {
                sBuilder.append(nextChar);
                nextChar = readNextChar();
            } else {
                extraCharRead = nextChar;
                break;
            }
        }

        deleteToken.setValue(sBuilder.toString());
        return deleteToken;
    }

    private Token buildCommentToken(String currentChar) {
        Token commentToken = new Token();
        commentToken.setType(TokenType.DELETE);
        commentToken.setSourceLineNumber(sourceLineNumber);
        StringBuilder sBuilder = new StringBuilder(currentChar);

        String nextChar = readNextChar();
        while (nextChar != null) {
            if (LexicalRegexPatterns.COMMENT_PATTERN.matcher(nextChar).matches()) {
                sBuilder.append(nextChar);
                nextChar = readNextChar();
            } else if (nextChar.equals("\n")) {
                break;
            }
        }

        commentToken.setValue(sBuilder.toString());
        return commentToken;
    }

    private Token buildPunctuationToken(String currentChar) {
        Token punctuationToken = new Token();
        punctuationToken.setSourceLineNumber(sourceLineNumber);
        punctuationToken.setValue(currentChar);
        if (currentChar.equals("("))
            punctuationToken.setType(TokenType.L_PAREN);
        else if (currentChar.equals(")"))
            punctuationToken.setType(TokenType.R_PAREN);
        else if (currentChar.equals(";"))
            punctuationToken.setType(TokenType.SEMICOLON);
        else if (currentChar.equals(","))
            punctuationToken.setType(TokenType.COMMA);

        return punctuationToken;
    }
}


