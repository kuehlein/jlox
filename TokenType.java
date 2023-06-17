package jlox;

// the parser could categorize tokens from the raw lexemes by comparing the strings, but that's slow
// and kind of ugly. Instead, at the point that we recognize a lexeme, we also remember which kind
// of lexeme it represents

enum TokenType {
  // Single-character tokens.
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
  COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

  // One or two character tokens.
  BANG, BANG_EQUAL, EQUAL, EQUAL_EQUAL,
  GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,

  // Literals.
  // since the scanner has to walk each character in the literal to correctly identify it, it can
  // also convert that textual representation of a value to the living runtime object that will be
  // used by the interpreter later
  IDENTIFIER, STRING, NUMBER,

  // Keywords.
  AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
  PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

  EOF
}
