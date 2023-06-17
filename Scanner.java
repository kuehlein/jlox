package jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jlox.TokenType.*;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0; // points to the first character in the lexeme being scanned
  private int current = 0; // points to the character currently being considered
  private int line = 1; // tracks what source line `current` is on so we can produce tokens that know their location

  private static final Map<String, TokenType> keywords;
  static {
    keywords = new HashMap<>();
    keywords.put("and", AND);
    keywords.put("class", CLASS);
    keywords.put("else", ELSE);
    keywords.put("false", FALSE);
    keywords.put("for", FOR);
    keywords.put("fun", FUN);
    keywords.put("if", IF);
    keywords.put("nil", NIL);
    keywords.put("or", OR);
    keywords.put("print", PRINT);
    keywords.put("return", RETURN);
    keywords.put("super", SUPER);
    keywords.put("this", THIS);
    keywords.put("true", TRUE);
    keywords.put("var", VAR);
    keywords.put("while", WHILE);
  }

  Scanner(String source) {
    this.source = source;
  }

  // the scanner works its way through the source code, adding tokens until it runs out of
  // characters. then it appends one final "end of file" token. that isn't strictly needed, but it
  // makes our parser a little cleaner.
  List<Token> scanTokens() {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  // in each turn of the loop (in `scanTokens`) we scan a single token. this is the real heart of
  // the scanner...
  private void scanToken() {
    char c = advance();

    switch (c) {
      // single character tokens
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA); break;
      case '.': addToken(DOT); break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break;

      // single or multi-character tokens
      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL : GREATER);
        break;
      case '/':
        if (match('/')) {
          // A comment goes until the end of the line.
          // comments are lexemes, but they aren't meaningful and the parser doesn't want to deal
          // with them. When we reach the end of the comment, we don't want to call `addToken`. When
          // we loop back around to the start of the next lexeme, `start` gets reset and the
          // comment's lexeme disappears in a puff of smoke.
          while (peek() != '\n' && !isAtEnd()) advance();
        } else {
          addToken(SLASH);
        }
        break;

      // when encountering whitespace, we simply go back to the beginning of the scan loop. That
      // starts a new lexeme after the whitespace character
      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break;

      // for newlines, we go back to the beginning of the scan loop (like with whitespace), but we
      // also increment the `line` counter (this is why we used `peek` to find the newline ending a
      // comment instead of `match`, we want the newline to get us here so we can update `line`)
      case '\n':
        line++;
        break;

      // ...
      case '"': string(); break;

      // handle unrecognized characters
      // the erroneous character is still consumed with the call to `advance`. we need this to
      // happen so we don't get stuck in an infinite loop. we also keep scanning. there may be other
      // errors in the program. it gives users a better experience if we detect as many errors as we
      // can in one go - since `hadError` gets set in `Lox.error`, we never try to execute any of
      // this code
      default:
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          Lox.error(line, "Unexpected character.");
        }
        break;
    }
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) advance();

    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null) type = IDENTIFIER;
    addToken(type);
  }

  // we consume as many digits as we find for the integer part of the literal. then we look for a
  // fractional part, which is a decimal point (`.`) followed by at least one digit. If we do have a
  // fractional part, again, we consume as many digits as we can find.
  private void number() {
    while (isDigit(peek())) advance();

    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();

      while (isDigit(peek())) advance();
    }

    // convert the lexeme to its numeric value. we are using java's own parsing method to convert
    // the lexeme to a real java double.
    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  // like with comments, we consume characters until we hit the `"` that ends the string. We also
  // gracefully handle running out of input before the string is closed and report an error for
  // that.
  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      // lox supports multiline strings
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    advance(); // The closing `"`.

    // Trim the surrounding quotes.
    // when we create the token we also produce the actual string *value* that will be used later by
    // the interpreter. Here, that conversion only requires a `substring` to strip off the
    // surrounding quotes. *If lox supported escape sequences like `\n`, we'd unescape those here
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }

  // functions like a conditional `advance` - only consume the character if its what we are looking
  // for
  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }

  // sort of like `advance`, but doesn't consume the character. This is called a `lookahead`. Since
  // it only looks at the current unconsumed character, we have "one character of lookahead." The
  // smaller this number is, generally, the faster the scanner runs. The rules of the lexical
  // grammar dictate how much lookahead we need. Most languages in use only peek one or two
  // characters ahead
  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  // looking past the decimal point requires a second character of lookahead since we don't want to
  // consume the `.` until we're sure there is a digit after it
  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
           c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  // handle input - consumes the next character in the source file and returns it
  private char advance() {
    return source.charAt(current++);
  }

  // handle output - grabs the text of the current lexeme and creates a new token for it
  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}
