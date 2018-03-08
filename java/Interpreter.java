import java.util.*;
import Fun.Absyn.*;

public class Interpreter {

	// Signature
	final Map<String, Exp> sig = new TreeMap();

	// Strategy
	final boolean strategy;

	// Control debug printing.
	final boolean debug = false;

	public Interpreter(boolean strategy) {
		this.strategy = strategy;
	}

	//////////////////////////////////////////////////////
	//////////////////////////////////////////////////////
	// ENTRY POINT OF INTERP
	public void interpret(Program p) {
		System.out.println(p.accept(new ProgramVisitor(), null).intValue());
	}

	public class ProgramVisitor implements Program.Visitor<Value, Void> {
		public Value visit(Fun.Absyn.Prog p, Void arg) {
			// build signature
			for (Def d : p.listdef_) {

				d.accept(new DefVisitor(), null);

			}
			// execute main expression
			return p.main_.accept(new MainVisitor(), null);
		}
	}

	public class MainVisitor implements Main.Visitor<Value, Void> {
		public Value visit(Fun.Absyn.DMain p, Void arg) {
			if (debug) {
				for (String key : sig.keySet())
					System.out.println("Check: Found key " + key + " in the signature");
				System.out.println("Evaluating main expression: " + Fun.PrettyPrinter.print(p.exp_));
			}
			return p.exp_.accept(new EvalVisitor(), new Empty());
		}
	}

	// visit defs only to build the signature.
	public class DefVisitor implements Def.Visitor<Void, Void> {
		public Void visit(Fun.Absyn.DDef p, Void arg) {
			// abstract over arguments from right to left
			Exp e = p.exp_;

			Collections.reverse(p.listident_);
			for (String x : p.listident_)
				e = new EAbs(x, e);

			// Add to signature
			sig.put(p.ident_, e);

			if (debug) {
				if (sig.get(p.ident_) != null)
					System.out.println("Added " + p.ident_ + " to the signature.");
				else
					System.out.println("Failed to add " + p.ident_ + " to the signature.");
			}
			return null;
		}
	}

	public class EvalVisitor implements Exp.Visitor<Value, Environment> {
		// variable
		public Value visit(Fun.Absyn.EVar p, Environment env) {
			if (debug) {
				for (String key : sig.keySet())
					System.out.println("EVar: Found key " + key + " in the signature");
				System.out.println("Resolving variable " + p.ident_);
			}

			// Look first in the local environment
			Value v = env.lookup(p.ident_);
			if (v != null) {
				if (debug)
					System.out.println("Found " + p.ident_ + " in the environment");
				return v;
			}

			// Look then in the global signature
			if (debug) {
				if (sig.isEmpty())
					System.out.println("Signature is empty!");
				else
					System.out.println("Signature is not empty!");
				for (String key : sig.keySet())
					System.out.println("Found key " + key + " in the signature");
			}

			Exp e = sig.get(p.ident_);
			if (e != null) {
				if (debug)
					System.out.println("Found " + p.ident_ + " in the signature");
				return e.accept(this, new Empty());
			}
			// not found?
			else
				throw new RuntimeException("unbound variable " + p.ident_);
		}

		// literal
		public Value visit(Fun.Absyn.EInt p, Environment env) {
			return new VInt(p.integer_);
		}

		// lambda
		public Value visit(Fun.Absyn.EAbs p, Environment env) {
			return new VClos(p.ident_, p.exp_, env);
		}

		// application
		public Value visit(Fun.Absyn.EApp p, Environment env) {

			Value value = p.exp_1.accept(new EvalVisitor(), env);

			VClos f = (VClos) value;
			Value temp;

			if (strategy) { // CBN
				String id = f.x;

				if (p.exp_2 instanceof EInt) {
					EInt eint = (EInt) p.exp_2;
					temp = new VInt(eint.integer_);

				}

				else if (p.exp_2 instanceof EAbs) {
					EAbs eabs = (EAbs) p.exp_2;
					temp = new VClos(eabs.ident_, eabs.exp_, env);

				}

			} else {
				temp = p.exp_2.accept(this, env);

			}
			return f.apply(temp);
		}

		// plus
		public Value visit(Fun.Absyn.EAdd p, Environment env) {
			Value v = p.exp_1.accept(new EvalVisitor(), env);
			Value v2 = p.exp_2.accept(new EvalVisitor(), env);
			try {
				return new VInt(v.intValue() + v2.intValue());
			} catch (RuntimeException e) {
				throw new RuntimeException("(+) can only be done using ints");
			}
		}

		// minus
		public Value visit(Fun.Absyn.ESub p, Environment env) {
			Value v = p.exp_1.accept(new EvalVisitor(), env);
			Value v2 = p.exp_2.accept(new EvalVisitor(), env);
			try {
				return new VInt(v.intValue() + v2.intValue());
			} catch (RuntimeException e) {
				throw new RuntimeException("(-) can only be done using ints");
			}
		}

		// less-than
		public Value visit(Fun.Absyn.ELt p, Environment env) {
			Value v = p.exp_1.accept(new EvalVisitor(), env);
			Value v2 = p.exp_2.accept(new EvalVisitor(), env);
			try {
				if (v.intValue() < v2.intValue()) {
					return new VInt(1); // this means true
				} else {
					return new VInt(0); // false
				}
			} catch (RuntimeException e) {
				throw new RuntimeException("LT error, use integers!");
			}
		}

		// if
		public Value visit(Fun.Absyn.EIf p, Environment env) {
			int temp;
			Value v = p.exp_1.accept(new EvalVisitor(), env);

			try {
				temp = v.intValue();
			} catch (RuntimeException e) {
				throw new RuntimeException("the condition in an if-statement must be an int");
			}

			if (temp == 1) {
				return p.exp_2.accept(new EvalVisitor(), env);
			}

			else if (temp != null) {
				return p.exp_3.accept(new EvalVisitor(), env);
			}

			else
				return null;

		}
	}


	// Value /////////////////////////////////////////////////////////////

	abstract class Value {
		abstract public int intValue();

		abstract public Value apply(Value v); // call by value
	}

	// Numeric values

	class VInt extends Value {

		final int val;

		public VInt(int i) {
			val = i;
		}

		public int intValue() {
			return val;
		}

		public Value apply(Value v) {
			throw new RuntimeException("cannot apply a number");
		}
	}

	// Function values

	class VClos extends Value {
		final String x;
		final Exp exp;
		final Environment env;

		public VClos(String x, Exp exp, Environment env) {
			this.x = x;
			this.exp = exp;
			this.env = env;
		}

		public int intValue() {
			throw new RuntimeException("not an integer");
		}

		public Value apply(Value v) {

			if (exp instanceof EInt) {
				throw new RuntimeException("cant apply this");

				return exp.accept(new EvalVisitor(), new Extend(x, v, env)); // TODO
																				// //
																				// x=v
			}

		}
	}

		// Environment ///////////////////////////////////////////////////////

		abstract class Environment {
			abstract Value lookup(String x);
		}

		class Empty extends Environment {
			Value lookup(String x) {
				return null;
			}
		}

	class Extend extends Environment {
    final String x;
    final Value  v;          // TODO: call-by-name
    final Environment rest;

    public Extend (String x, Value v, Environment rest) {
      this.x = x;
      this.v = v;
      this.rest = rest;
    }


    Value lookup (String y) {
      if (!x.equals(y)){
        return rest.lookup(y);
      }
    }
}
}
