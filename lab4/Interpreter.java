import java.util.*;
import Fun.Absyn.*;

public class Interpreter {

  // Signature
  final Map<String,Exp> sig  = new TreeMap();

  // Strategy
  final boolean strategy;

  // Control debug printing.
  final boolean debug = false;

  public Interpreter (boolean strategy) {
    this.strategy = strategy;
    this.sig = new HashMap<String,Exp>();
  }


//////////////////////////////////////////////////////
//////////////////////////////////////////////////////
  //ENTRY POINT OF INTERP
  public void interpret(Program p) {
    System.out.println(p.accept(new ProgramVisitor(), null).intValue());
  }

  public class ProgramVisitor implements Program.Visitor<Value,Void>
  {
    public Value visit(Fun.Absyn.Prog p, Void arg)
    {
      // build signature
      for (Def d: p.listdef_){

        d.accept(new DefVisitor(), null);

      }
      // execute main expression
      return p.main_.accept(new MainVisitor(), null);
    }
  }

  public class MainVisitor implements Main.Visitor<Value,Void>
  {
    public Value visit(Fun.Absyn.DMain p, Void arg)
    {
      if (debug) {
        for (String key : sig.keySet())
          System.out.println ("Check: Found key " + key + " in the signature");
        System.out.println("Evaluating main expression: " +
          Fun.PrettyPrinter.print(p.exp_));
      }
      return p.exp_.accept(new EvalVisitor(), new Empty());
    }
  }

  // visit defs only to build the signature.
  public class DefVisitor implements Def.Visitor<Void,Void>
  {
    public Void visit(Fun.Absyn.DDef p, Void arg)
    {
      // abstract over arguments from right to left
      Exp e = p.exp_;

      Collections.reverse(p.listident_);
      for (String x: p.listident_) e = new EAbs(x, e);

      // Add to signature
      sig.put(p.ident_, e);

      if (debug) {
        if (sig.get(p.ident_) != null)
          System.out.println("Added " + p.ident_ + " to the signature.");
        else System.out.println("Failed to add " + p.ident_ + " to the signature.");
      }
      return null;
    }
  }

  public class EvalVisitor implements Exp.Visitor<Value,Environment>
  {
    // variable
    public Value visit(Fun.Absyn.EVar p, Environment env)
    {
      if (debug) {
        for (String key : sig.keySet())
          System.out.println ("EVar: Found key " + key + " in the signature");
        System.out.println ("Resolving variable " + p.ident_);
      }

      // Look first in the local environment
      Value v = env.lookup(p.ident_);
      if (v != null) {
        if (debug) System.out.println ("Found " + p.ident_ + " in the environment");
        return v;
      }

      // Look then in the global signature
      if (debug) {
        if (sig.isEmpty()) System.out.println ("Signature is empty!");
        else System.out.println ("Signature is not empty!");
        for (String key : sig.keySet())
          System.out.println ("Found key " + key + " in the signature");
      }

      Exp e = sig.get(p.ident_);
      if (e != null) {
        if (debug) System.out.println ("Found " + p.ident_ + " in the signature");
        return e.accept(this, new Empty());
      }
      // not found?
      else throw new RuntimeException("unbound variable " + p.ident_);
    }

    // literal
    public Value visit(Fun.Absyn.EInt p, Environment env)
    {
      return new VInt(p.integer_);
    }

    // lambda
    public Value visit(Fun.Absyn.EAbs p, Environment env)
    {
      return new VClos(p.ident_, p.exp_, env);
    }

    // application
    public Value visit(Fun.Absyn.EApp p, Environment env)
    {
      if (debug) {
        for (String key : sig.keySet())
          System.out.println ("EApp: Found key " + key + " in the signature");
      }
      if (strategy) todo("call-by-name");
      // Evaluate the function
      Value vfun = p.exp_1.accept (this, env);
      // Evaluate the argument
      Value varg = p.exp_2.accept (this, env);
      return vfun.apply(varg);
    }

    // plus
    public Value visit(Fun.Absyn.EAdd p, Environment env)
    {
      Value v = p.exp_1.accept(new EvalVisitor(), env);
      Value v2 = p.exp_2.accept(new EvalVisitor(), env);
      try{
          return new VInt (v.intValue() + v2.intValue());
      }
      catch(RuntimeException e){
        throw new RuntimeException("(+) can only be done using ints");
      }
    }

    // minus
    public Value visit(Fun.Absyn.ESub p, Environment env)
    {
      Value v = p.exp1_.accept(new EvalVisitor(), env);
      Value v2 = p.exp_2.accept(new EvalVisitor(), env);
      try{
          return new VInt (v.intValue() + v2.intValue());
      }
      catch(RuntimeException e){
        throw new RuntimeException("(-) can only be done using ints");
      }
    }

    // less-than
    public Value visit(Fun.Absyn.ELt p, Environment env)
    {
      Value v = p.exp_1.accept(new EvalVisitor(), env);
      Value v2 = p.exp_2.accept(new EvalVisitor(), env);
      try{
          if(v.intValue() < v2.intValue()){
            return new VInt(1); // this means true
          }
          else{
            return VInt(0); //false
          }
      }
      catch(RuntimeException e){
        throw new RuntimeException("LT error, use integers!");
      }
    }

    // if
    public Value visit(Fun.Absyn.EIf p, Environment env)
    {
      int temp;
      Value v = p.exp_1.accept(new EvalVisitor(), env);
      
      try{
          temp = v.intValue();
          }catch(RuntimeException e){
            throw new RuntimeException("the condition in an if-statement must be an int");
          }
      
          if(temp == 1){
            return p.exp_2.accept(new EvalVisitor(), env);
            return p.exp_3.accept(new EvalVisitor(),env);
          }
      }
      // KANSKE MÅSVINGE HÄR
    }

  // Value /////////////////////////////////////////////////////////////

  abstract class Value {
    abstract public int   intValue();
    abstract public Value apply(Value v); // call by value
    abstract public Value apply(Exp e); // call by name
  }

  // Numeric values

  class VInt extends Value {

    final int val;
    public VInt (int i) { val = i; }

    public int   intValue() {
      return val;
    }
    public Value apply(Value v) {
      throw new RuntimeException ("cannot apply a number");
    }

    public Value apply(Exp v){
      throw new RuntimeException ("cannot apply a number");
    }
  }

  // Function values

  class VClos extends Value {
    final String x;
    final Exp body;
    final Environment env;

    public VClos (String x, Exp body, Environment env) {
      this.x = x;
      this.body = body;
      this.env = env;
    }

    public int intValue() { throw new RuntimeException ("not an integer"); }
    
    public Value apply (Value v) {
      return body.accept (new EvalVisitor(), new Extend(x,v,env));  // TODO add x=v to env
    }
   
   //call by name

      public Value apply (Exp v) {
        return body.accept (new EvalVisitor(), new Extend(x,e,env)); 
    }
  }

  // Environment ///////////////////////////////////////////////////////

  abstract class Environment {
    abstract Value lookup (String x);
  }

  class Empty extends Environment {
    Value lookup (String x) {
     return null; 
   }
  }

  class Extend extends Environment {
    final String x;
    final Value  v;          // TODO: call-by-name
    final Exp exp ;
    final Environment rest;

    public Extend (String x, Value v, Environment rest) {
      this.x = x;
      this.v = v;
      this.exp = null;
      this.rest = rest;
    }

     public Extend (String x, Exp exp, Environment rest) {
      this.x = x;
      this.v = null;
      this.exp = exp;
      this.rest = rest;
    }

    Value lookup (String y) {
      if (!x.equals(y)){
        return rest.lookup(y);
      }

      if(!Strategy){
        return v;
      }
      
      return e.accept(new EvalVisitor(), rest);
  }

}
