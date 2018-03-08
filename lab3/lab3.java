import java.io.*;
import java_cup.runtime.*;
import CPP.*;
import CPP.Absyn.*;

public class lab3 {
  public static void main(String args[]) {

    if (args.length != 1) {
      System.err.println("Usage: lab3 <SourceFile>");
      System.exit(1);
    }

    Yylex l = null;
    String filePath = args[0];
    try {
      l = new Yylex(new FileReader(filePath));
      parser p = new parser(l);
      CPP.Absyn.Program parseTree = p.pProgram();
      new TypeChecker().typecheck(parseTree);
      new Compiler().compile(stripSuffix(stripPath(filePath)), parseTree);
    }
    catch (TypeException e) {
      System.out.println("TYPE ERROR");
      System.err.println(e.toString());
      System.exit(1);
    }
    catch (RuntimeException e) {
      //            System.out.println("RUNTIME ERROR");
      System.err.println(e.toString());
      System.exit(-1);
    }
    catch (IOException e) {
      System.err.println(e.toString());
      System.exit(1);
    }
    catch (Throwable e) {
      System.out.println("SYNTAX ERROR");
      System.out.println("At line " + String.valueOf(l.line_num())
                 + ", near \"" + l.buff() + "\" :");
      System.out.println("     " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  // Remove path from file.
  private static String stripPath(String name) {
    return new File(name).getName();
  }

  // Remove extension from file
  private static String stripSuffix(String filename) {
    int divider = filename.lastIndexOf('.');
    if (divider <= 0) return filename;
    else return filename.substring(0, divider);
  }
}
