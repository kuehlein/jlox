package jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
  // helps us no try to execute code with a known error
  static boolean hadError = false;

  public static void main(String[] args) throws IOException {
    // throw an error - too many args passed
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);

      // args[0] is path to file to be executed
    } else if (args.length == 1) {
      runFile(args[0]);

      // run interpreter interactively
    } else {
      runPrompt();
    }
  }

  // run code via command line by giving it a path to the file
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));

    // Indicate an error in the exit code.
    if (hadError) System.exit(65);
  }

  // run interpreter interactively - enter & execute one line of code at a time
  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null) break; // control + D to kill
      run(line);
      hadError = false; // don't kill the session in interactive mode
    }
  }

  // ...
  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    for (Token token : tokens) {
      System.out.println(token);
    }
  }

  // used for syntax errors
  static void error(int line, String message) {
    report(line, "", message);
  }

  // its good practice to separate the code that generates errors (e.g. scanner/parser) from the
  // code that reports them. various phases of the front end will detect errors, but its not really
  // their job to know how to present that to a user. in a full-featured language implementation you
  // will likely have multiple ways errors get displayed: on stderr, in an IDE's error window,
  // logged onto a file, etc. Please don't put that code all over the scanner/parser. Ideally put it
  // in some kind of abstraction (e.g. `ErrorReporter` passed to scanner and parser)
  private static void report(int line, String where, String message) {
    System.err.println(
      "[line " + line + "] Error" + where + ": " + message
    );
    hadError = true;
  }
}
