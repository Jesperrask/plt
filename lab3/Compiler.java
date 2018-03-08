import java.io.*;
import java.util.*;

import CPP.Absyn.*;

public class Compiler
{
  // The output of the compiler is a list of strings.
  LinkedList<String> output;

  
  public ListArg intArg = new ListArg(); 
  
   private class Env {
	     // Signature mapping function names to their JVM name and type
        public HashMap<String , FunType> sig ; 
		 // Context mapping variable identifiers to their type.
        public LinkedList<HashMap<String , Integer >> cxt ; 
        public LinkedList<Integer> stackSize ;
        public Integer limitStack;
        public Integer localLabelCount ; 
        public Type returnType ; 
        public int returnFlag ; 
        public Env() 
        {
            sig = new HashMap<String , FunType>() ;
            cxt = new LinkedList<HashMap<String , Integer>>();
            stackSize = new LinkedList<Integer>() ; 
            cxt.addFirst(new HashMap<String , Integer>());
            stackSize.addFirst(0);
            localLabelCount = 0 ; 
            limitStack = 0;
        }
        public Integer lookupVar (String id)
        {
            Integer ans = 0 ; 
            for(HashMap<String ,Integer> scope : this.cxt)
            {
                ans = scope.get(id);
                if(ans != null)
                    break;
            }
            return ans ; 
        } 
        public Integer lookupLabelCount()
        {
            return localLabelCount;
        }
        public void addLabelCount()
        {
           localLabelCount  ++ ; 
        }
        public void addVar(String id)
        {
            HashMap<String , Integer > scope = cxt.getFirst();
            Integer number = stackSize.getFirst();
            scope.put(id , number);
            number = number +1 ; 
            stackSize.removeFirst();
            stackSize.addFirst(number); 
        }
        public boolean isFunDecl (String id )
        {
            return sig.containsKey(id); 
        }
        public void updateFunction (String id , FunType funType)
        {
            sig.put(id , funType) ; 
        }
        public FunType lookupFun (String id)
        {
            FunType funType = sig.get(id) ; 
            return funType ; 
        }
        public void enterScope()
        {
            cxt.addFirst(new HashMap<String , Integer>());
        }
        public void leaveScope()
        {
            cxt.removeFirst();
        }
    }
  

  private Env env = new Env();
  // Share type constants
  public final Type BOOL   = new CPP.Absyn.Type_bool();
  public final Type INT    = new CPP.Absyn.Type_int();
  public final Type DOUBLE = new CPP.Absyn.Type_double();
  public final Type VOID   = new CPP.Absyn.Type_void();

  // name should be just the class name without file extension.
  public void compile(String name, CPP.Absyn.Program p) {
    // Initialize output
    output = new LinkedList();
    
    // Output boilerplate
    output.add(".class public " + name + "\n");
    output.add(".super java/lang/Object\n");
    output.add("\n");
    output.add(".method public <init>()V\n");
    output.add("  .limit locals 1\n");
    output.add("\n");
    output.add("  aload_0\n");
    output.add("  invokespecial java/lang/Object/<init>()V\n");
    output.add("  return\n");
    output.add("\n");
    output.add(".end method\n");
    output.add("\n");
    output.add(".method public static main([Ljava/lang/String;)V\n");
    output.add("  .limit locals 1\n");
    output.add("  .limit stack  1\n");
    output.add("\n");
    output.add("  invokestatic " + name + "/main()I\n");
    output.add("  pop\n");
    output.add("  return\n");
    output.add("\n");
    output.add(".end method\n");
    output.add("\n");

    // Built-in functions
    intArg = new ListArg();    
    intArg.add (new ADecl(INT   , "x"));
    env.updateFunction("printInt",    new FunType ("Runtime/printInt", VOID, intArg));
    env.updateFunction("readInt",    new FunType ("Runtime/readInt",INT  , intArg));

    // User-defined functions
    for (Def d: ((PDefs)p).listdef_) {
	    DFun def = (DFun)d;
		Type returnType = def.type_;
		env.updateFunction(def.id_ , new FunType(name + "/"+def.id_ , returnType, def.listarg_ ));
    }
	

    // Run compiler
    p.accept(new ProgramVisitor(), null);

    // Output result
    try{
    FileWriter write = new FileWriter("answer.cc");
    for (String s: output) {
      System.out.print(s);
      write.write(s);
    }
    write.close();
    }catch(IOException e){}
  }

  public class ProgramVisitor implements Program.Visitor<Void,Void>
  {
    public Void visit(CPP.Absyn.PDefs p, Void arg)
    {
      for (Def def: p.listdef_)
      {
        def.accept(new DefVisitor(), null);
      }
      return null;
    }
  }
  public class DefVisitor implements Def.Visitor<Void,Void>
  {
    public Void visit(CPP.Absyn.DFun p, Void arg)
    {

      // save output so far and reset output;
      LinkedList<String> savedOutput = output;
      output = new LinkedList();
		
      // Compile function

      // Add function parameters to context
      env.enterScope();
      for (Arg x: p.listarg_)
        x.accept (new ArgVisitor(), null);
      for (Stm s: p.liststm_)
        s.accept (new StmVisitor(), null);

      // add new Output to old output
      LinkedList<String> newOutput = output;
      output = savedOutput;

      FunType f = new FunType(p.id_, p.type_, p.listarg_);
      output.add("\n.method public static " + f.toJVM() + "\n");
      output.add("  .limit locals " + env.localLabelCount + "\n");
      output.add("  .limit stack " + env.limitStack + "\n\n");
      for (String s: newOutput) {
        output.add("  " + s);
      }
      output.add("\n.end method\n");
      env.stackSize.removeFirst();
      env.leaveScope();
      return null;
    }
  }

  public class ArgVisitor implements Arg.Visitor<Void,Void>
  {
    public Void visit(CPP.Absyn.ADecl p, Void arg)
    {
    	
    	env.addVar(p.id_);
    	return null;
    }
  }

  void nyiStm (CPP.Absyn.Stm s) {
    throw new RuntimeException ("Not yet implemented: " + CPP.PrettyPrinter.print(s));
  }

  public class StmVisitor implements Stm.Visitor<Void,Void>
  {
    // e;
    public Void visit(CPP.Absyn.SExp p, Void arg)
    {
      p.exp_.accept (new ExpVisitor(), null);
      emit (new Pop (p.exp_.getType()));
      return null;
    }

    // int x,y,z;
    public Void visit(CPP.Absyn.SDecls p, Void arg)
    {
    	for (String x: p.listid_){
    	  env.addVar(x);
    	  if(p.type_.equals(INT)){
    		  emit(new IConst(0));
    		  emit( new Store(p.type_,env.lookupVar(x)));
    	  }
      }
      return null;
    }

    // int x = e;
    public Void visit(CPP.Absyn.SInit p, Void arg)
    {
      
      p.exp_.accept(new ExpVisitor(), null);
      env.addVar(p.id_);
      if(p.type_.equals(INT)){
          emit (new Store (p.type_, env.lookupVar(p.id_)));
      }
      else if(p.type_.equals(BOOL)){
          emit (new Store (p.type_, env.lookupVar(p.id_)));
      }

      return null;
    }

    // return e;
    public Void visit(CPP.Absyn.SReturn p, Void arg)
    {
      p.exp_.accept (new ExpVisitor(), null);
      emit (new Return (p.exp_.getType()));
      return null;
    }

    // while (e) s
    //ASK TEACHER
    public Void visit(CPP.Absyn.SWhile p, Void arg)
    {
      // p.exp_
      // p.stm_

    	Integer startLabel = env.lookupLabelCount();
    	env.addLabelCount();
    	Integer endLabel = env.lookupLabelCount();
    	env.addLabelCount();
    	emitToLabel(new Label(startLabel));
    	p.exp_.accept(new ExpVisitor(), null);
    	emit(new IConst(0));
    	emit(new IfIcmpEq(endLabel));
    	p.stm_.accept(new StmVisitor(), null);
    	emit(new Goto(startLabel));
    	emitToLabel(new Label(endLabel));
    	return null;
    }

    // { ss }
    public Void visit(CPP.Absyn.SBlock p, Void arg)
    {
    	env.enterScope();
      for (Stm s: p.liststm_) {
    	  s.accept(new StmVisitor(),null);
      }
      env.leaveScope();
      return null;
    }

    // if (e) s else s'
	//Ask teacher
    public Void visit(CPP.Absyn.SIfElse p, Void arg)
    {
    	Integer ifElseLabel = env.lookupLabelCount();
    	env.addLabelCount();
    	Integer endLabel = env.lookupLabelCount();
    	env.addLabelCount();
    	p.exp_.accept(new ExpVisitor(), arg);
    	emit(new IConst(0));
    	emit(new IfIcmpEq(ifElseLabel));
    	p.stm_1.accept(new StmVisitor(), arg);
    	emit(new Goto(endLabel));
    	p.stm_2.accept(new StmVisitor(), arg);
    	emitToLabel(new Label(endLabel));
    	return null;
    }
  }

  void nyiExp (CPP.Absyn.Exp e) {
    throw new RuntimeException ("Not yet implemented: " + CPP.PrettyPrinter.print(e));
  }
  
  

  public class ExpVisitor implements Exp.Visitor<Integer,Void>
  {
    // true
    public Integer visit(CPP.Absyn.ETrue p, Void arg)
    {
      emit(new IConst(1));
      return null;
    }

    public Integer visit(CPP.Absyn.EFalse p, Void arg)
    {
    	emit(new IConst(0));
    	return null;
    }

    public Integer visit(CPP.Absyn.EInt p, Void arg)
    {
      emit (new IConst (p.integer_));
      return null;
    }
    public Integer visit(CPP.Absyn.EDouble p, Void arg)
    {
      //Do not handle doubles
      return null;
    }



    public Integer visit(CPP.Absyn.EId p, Void arg)
    {
      Integer adr = env.lookupVar (p.id_);
      emit (new Load(INT, adr));
      return adr;
    }


    public Integer visit(CPP.Absyn.EApp p, Void arg)
    {
      for (Exp e: p.listexp_){
    	  e.accept (this, null);
      }
      FunType ft = env.lookupFun(p.id_);
      emit(new Call(new Fun(p.id_, ft)));
      if(ft.returnType.equals(VOID)){
    	  emit(new IConst(0));
      }
      return null;
    }
    public Integer visit(CPP.Absyn.EPostIncr p, Void arg)
    {
    	
    	//fix this 
    	Integer addr = p.exp_.accept(new ExpVisitor(), null);
    	emit(new Load(p.exp_.getType(), addr));
    	emit(new Dup(p.exp_.getType()));
    	emit (new IConst (1));
    	emit(new Add(p.exp_.getType()));
    	if(addr != null){
    		emit(new Store(INT, addr));
    	}
    	return null;
    }

 
    public Integer visit(CPP.Absyn.EPostDecr p, Void arg)
    {
    	//fix this 
    	Integer addr = p.exp_.accept(new ExpVisitor(), null);
    	addr = p.exp_.accept(new ExpVisitor(), null);
    	emit (new IConst (1));
    	emit(new Sub(INT));
    	if(addr != null){
    		emit(new Store(INT, addr));
    	}
    	return null;
    }
    

    // ++x
    public Integer visit(CPP.Absyn.EPreIncr p, Void arg)
    {
    	Integer addr = p.exp_.accept(new ExpVisitor(), null);
    	emit (new IConst (1));
    	emit(new Add(INT));
    	if(addr != null){
    		emit(new Store(INT, addr));
    		emit(new Load(INT, addr));
    	}
    	return null;
    }

    // --x
    public Integer visit(CPP.Absyn.EPreDecr p, Void arg)
    {
    	Integer addr = p.exp_.accept(new ExpVisitor(), null);
    	emit (new IConst (1));
    	emit(new Sub(INT));
    	if(addr != null){
    		emit(new Store(INT, addr));
    		emit(new Load(INT, addr));
    	}
    	return null;
    }


///////////////////////////////////////////////////7

    // e * e'
    public Integer visit(CPP.Absyn.ETimes p, Void arg)
    {
    	p.exp_1.accept(new ExpVisitor(), arg);
    	p.exp_2.accept(new ExpVisitor(), arg);
    	emit(new Mul(p.exp_1.getType()));
    	return null;
    }

    // e / e'
    public Integer visit(CPP.Absyn.EDiv p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      emit(new Div(p.exp_1.getType()));
      return null;
    }

    //  e + e'
    public Integer visit(CPP.Absyn.EPlus p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      emit(new Add(p.exp_1.getType()));
      return null;
    }

    // e - e'
    public Integer visit(CPP.Absyn.EMinus p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);    		
      emit(new Sub(p.exp_1.getType()));
      return null;
    }

    // e < e'
    public Integer visit(CPP.Absyn.ELt p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      Integer tLabel = env.lookupLabelCount();
      env.addLabelCount();
      Integer endLabel = env.lookupLabelCount();
      env.addLabelCount();
      emit(new IfIcmpLt(tLabel));
      emit(new IConst(0));
      emit(new Goto(endLabel));
      emitToLabel(new Label(tLabel));
      emit(new IConst(1));
      emitToLabel(new Label(endLabel));
      
      return null;
    }

    // e > e'
    public Integer visit(CPP.Absyn.EGt p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);

      
      nyiExp(p); return null;
    }

    // e <= e'
    public Integer visit(CPP.Absyn.ELtEq p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      nyiExp(p); return null;
    }

    // e >= e'
    public Integer visit(CPP.Absyn.EGtEq p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      nyiExp(p); return null;
    }

    // e == e'
    public Integer visit(CPP.Absyn.EEq p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      nyiExp(p); return null;
    }

    // e != e'
    public Integer visit(CPP.Absyn.ENEq p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      nyiExp(p); return null;
    }

    // e && e'
    public Integer visit(CPP.Absyn.EAnd p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      nyiExp(p); return null;
    }

    // e || e'
    public Integer visit(CPP.Absyn.EOr p, Void arg)
    {
      p.exp_1.accept(new ExpVisitor(), arg);
      p.exp_2.accept(new ExpVisitor(), arg);
      nyiExp(p); return null;
    }

    // x = e
    public Integer visit(CPP.Absyn.EAss p, Void arg)
    {
      String x = TypeChecker.isVar(p.exp_1);
      p.exp_2.accept(new ExpVisitor(), arg);
      nyiExp(p); return null;
    }
  }
//////////////////////////////////////////////////////////////
  void emit (Code c) {
    String line = c.accept(new CodeToJVM());
    if (!line.isEmpty()) output.add(line);
    adjustStack(c);
  }
  
  void emitToLabel(Label label){
	  String line = label.toJVM();
	  if(line.isEmpty()){
		  output.add(line);
	  }
  }

  // update limitStack, currentStack according to instruction
  void adjustStack(Code c) {
    c.accept(new AdjustStack());
  }
  


  void incStack(Type t) {
	int stackSz = env.stackSize.getFirst();
    stackSz = stackSz + t.accept(new Size(), null);
    if (stackSz > env.limitStack) env.limitStack = stackSz;
    env.stackSize.removeFirst();
    env.stackSize.addFirst(stackSz);
  }

  void decStack(Type t) {
		int stackSz = env.stackSize.getFirst();
			    stackSz = stackSz - t.accept(new Size(), null);
			    if (stackSz > env.limitStack) env.limitStack = stackSz;
			    env.stackSize.removeFirst();
			    env.stackSize.addFirst(stackSz);
  }

  // Calculate the size in words for an element of the give type

  class Size implements Type.Visitor<Integer,Void> {
    public Integer visit (Type_double t, Void arg) { return 2; }
    public Integer visit (Type_int    t, Void arg) { return 1; }
    public Integer visit (Type_bool   t, Void arg) { return 1; }
    public Integer visit (Type_void   t, Void arg) { return 0; }
  }

  class AdjustStack implements CodeVisitor<Void> {
    public Void visit (Comment a){                   return null; }
    public Void visit (Store a)  { decStack(a.type); return null; }
    public Void visit (Load  a)  { incStack(a.type); return null; }
    public Void visit (IConst c) { incStack(INT);    return null; }
    public Void visit (IfIcmpLt c)  {return null;}
    public Void visit (IfIcmpLe c)  {return null;}
    public Void visit (IfIcmpEq c)  {return null;}
    public Void visit (IfIcmpGt c)  {return null;}
    public Void visit (IfIcmpGe c)  {return null;}
    public Void visit (IfIcmpNe c)  {return null;}
    public Void visit (Goto c)	 	{return null;}
    public Void visit (IfEq c)		{return null;}
    public Void visit (Dup c)    { incStack(c.type); return null; }
    public Void visit (Pop c)    { decStack(c.type); return null; }
    public Void visit (Return c) { decStack(c.type); return null; }
    


    public Void visit (Call c)   {
      FunType ft = c.fun.funType;
      for (Arg arg: ft.args) decStack(((ADecl)arg).type_);
      incStack(ft.returnType);
      return null;
    }
    public Void visit (Add c)    { decStack(c.type); return null; }
    public Void visit (Mul c)    { decStack(c.type); return null; }
    public Void visit (Div c)    { decStack(c.type); return null; }
    public Void visit (Sub c)    { decStack(c.type); return null; }
  }
  
 
}
