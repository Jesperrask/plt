import CPP.Absyn.*;


import java.util.*;

public class Interpreter {
	//init scanner for input via keyboard
	public Scanner scan = new Scanner(System.in);
	
	public static abstract class Value{
		
		
		public boolean isInt(){
			
			return false;
		}
		
		public Integer getInt(){
			
			throw new RuntimeException("Value is not an integer");
		}
		
		public double getDouble(){
			
			throw new RuntimeException("Value is not a double");
		}
		
		public String print(){
			if(this.isInt()){
				return this.toString();
			}else{
				return this.toString();
			}
	
		}
	}
	
	
	public static class Undefined extends Value{
		
		public Undefined(){
			
		}
		public String toString(){
			
			return "undefined";
		}
	}
	
	public static class IntegerValue extends Value{
		
		Integer i;
		
		public IntegerValue(Integer i){
			this.i = i;
		}
		
		public Integer getInt(){
			return i;
		}
		
		public boolean isInt(){ 
			return true;
		}
		
		public String printInt(){
			return i.toString();
		}

		
	}
	
	public static class DoubleValue extends Value{
		double d;
		public DoubleValue(Double d){
			this.d = d;
		}
		
		public double getDouble(){
			return d;
		}
		
		public String printDouble(){
			return String.valueOf(d);
		}
		
	}
	
	public static class FunExecute{
		public LinkedList<String> arguments;
		public LinkedList<Stm> statements;
		public Value returnValue;
	}
	
	
	public static class Env{
		public HashMap<String, FunExecute> signature;
		public LinkedList<HashMap<String, Value>> contexts;

		public Env(){
			contexts = new LinkedList<HashMap<String, Value>>();
			signature = new HashMap<String, FunExecute>();
			enterScope();
		}
		
		public Value lookVar(String id){
		
			for (HashMap<String, Value> context : contexts) {
				Value value = context.get(id);
				if (value != null) {
					return value;
				}
			}
			throw new RuntimeException("There is no [" + id + "] variable");
		}
		
		public FunExecute lookFun(String id){
			FunExecute funExec = signature.get(id);
			if (funExec == null) {
				throw new TypeException("There is no [" + id + "] function");
			} else {
				return funExec;
			}
		}
		//declares a variable 
		public void declVar(String id){
			contexts.getFirst().put(id, new Undefined());
		}
		//Initialize a variable
		public void initVar(String id, Value val){
			contexts.getFirst().put(id, val);
		}
		
		//update a variable
		public void updateVar(String id, Value val){
			
			for(HashMap<String, Value> currentScope : contexts){
	
				if(currentScope.containsKey(id)){
					currentScope.put(id, val);
					return;
				}
			}
		}
		

		public void enterScope(){
			contexts.addFirst(new HashMap<String, Value>());
		}

		public void leaveScope(){
			contexts.removeFirst();
		}
	}


	public void interpret(Program p) {
	
			PDefs defs = (PDefs) p ; 
	        Env env = new Env() ; 
	        
	    	for(Def def : defs.listdef_){
				//add function exec body, but not the main function
				checkFunDef(def, env);
			}
			
			for(Def def : defs.listdef_){
				//exec main function
				checkMainDef(def, env);
			}
		}

		private void checkFunDef(Def d, Env env){
			d.accept(new defExeAdd(), env);
		}
		private void checkMainDef(Def d, Env env){
			d.accept(new checkMainExec(), env);
		}
    
    
		private class defExeAdd implements Def.Visitor<Object , Env> {
			
			public Env visit(DFun p, Env env){
				
				if(p.id_ != "main"){
				 env.enterScope();
				FunExecute funExe = new FunExecute();
				funExe.arguments = new LinkedList<String>();
				funExe.statements = p.liststm_;
				
				for( Arg arg : p.listarg_){
					ADecl temp = (ADecl)arg;
					funExe.arguments.addLast(temp.id_);
				}
				
				env.signature.put(p.id_, funExe);
				env.leaveScope();
				}
				return null;
		}
	}
		
	
	
	private class checkMainExec implements Def.Visitor<Object , Env>{
			
		public Env visit(DFun p, Env env){
			Value val;
			if(p.id_ == "main"){
				env.enterScope();
				for(Stm stm : p.liststm_){
					val = executeStm(stm, env);
						if(val != null){
	    					env.leaveScope();
	    					return null;
						}
				}
				env.leaveScope();
			}else{
				return null;
			}
			return null;
		}
	}

	
	
	public Value executeStm(Stm p, Env env){
		return p.accept(new StmVisitor(), env);
	}
	
	
	  ////////////////////////////// Statements //////////////////////////////

	  public class StmVisitor implements Stm.Visitor<Value,Env>
	  {
	    public Value visit(CPP.Absyn.SExp p, Env env)
	    { /* Code For SExp Goes Here */
	      p.exp_.accept(new ExpVisitor(), env);
	      return null;
	    }
	    public Value visit(CPP.Absyn.SDecls p, Env env)
	    { // Code For SDecls Goes Here 
	      
	      for (String x: p.listid_){ 
	    	  env.initVar(x, new Undefined());
	      }
	      return null;
	    }
	    public Value visit(CPP.Absyn.SInit p, Env env)
	    { /* Code For SInit Goes Here */
		  
		   env.initVar(p.id_, executeExp(p.exp_, env));

		   	return null;
	    }
	    public Value visit(CPP.Absyn.SReturn p, Env env)
	    { /* Code For SReturn Goes Here */
	    	Value val = executeExp(p.exp_, env);
	    	return (Value)val;
	    }
	    
	    public Value visit(CPP.Absyn.SWhile p, Env env)
	    { /* Code For SWhile Goes Here */
			boolean b = true;
	    	while(b){
	    		Value val = executeExp(p.exp_, env);
	    		if(val.isInt()){
					
	    			Integer i = val.getInt();
	    			if(i.equals(1)){
	    				Value temp = executeStm(p.stm_, env);
	    				if(temp != null){
	    					return (Value)temp;
	    				}
	    			}
	    			else{

	    				b = false;
	    			}
	    		}
	    	}
	    	return null;
	    }
	    
	    public Value visit(CPP.Absyn.SBlock p, Env env)
	   { /* Code For SBlock Goes Here */
	    	
	    	env.enterScope();
	    	for (Stm stm : p.liststm_){
	    	  Value val = executeStm(stm, env);
	    	  if(val != null){
	    		  env.leaveScope();
	    		  return (Value)val;
	    	  }
	    	}
	    	env.leaveScope();
	    	return null;
	    }
	    
	    public Value visit(CPP.Absyn.SIfElse p, Env env)
	    { /* Code For SIfElse Goes Here */
	    	Value val = (executeExp(p.exp_, env));
	    	if(val.isInt()){
	    		Integer i = val.getInt();
	    		
	    		if(i.equals(1)){
	    			Value temp = executeStm(p.stm_1, env);
	    			
	    			if(temp != null){
	    				return (Value)temp;
	    			}
	    		}
	    		else{
	    			Value temp2 = executeStm(p.stm_2, env);
	    			
	    			if(temp2 != null){
	    				return (Value)temp2;
	    			}
	    		}
	    	}
	      return null;
	    }
	    
	  }

	  
		public Value executeExp(Exp p, Env env){
			return p.accept(new ExpVisitor(), env);
		}
		
	  ////////////////////////////// Expressions //////////////////////////////

	  public class ExpVisitor implements Exp.Visitor<Value,Env>
	  {
		  
	    public Value visit(CPP.Absyn.ETrue p, Env env)
	    { /* Code For ETrue Goes Here */
	      return new IntegerValue(1);
	    }
	    
	    public Value visit(CPP.Absyn.EFalse p, Env env)
	    { /* Code For EFalse Goes Here */
	    	return new IntegerValue(0);
	    }
	    
	    public Value visit(CPP.Absyn.EInt p, Env env)
	    { /* Code For EInt Goes Here */
	      //p.integer_;
	    	return new IntegerValue(p.integer_);
	    }
	    
	    public Value visit(CPP.Absyn.EDouble p, Env env)
	    { /* Code For EDouble Goes Here */
	      //p.double_;
	    	return new DoubleValue(p.double_);
	    }
	    
	    public Value visit(CPP.Absyn.EId p, Env env)
	    { /* Code For EId Goes Here */
	      //p.id_;
	    	return env.lookVar(p.id_);
	    }
	    
	    public Value visit(CPP.Absyn.EApp p, Env env){ /* Code For EApp Goes Here */
	      //p.id_;
	    	if(p.id_ == "readInt"){
	    		
	    		Integer i = scan.nextInt();
	    		return new IntegerValue(i);
	    	}
	    	else if(p.id_ == "printInt"){
	    		
	    		Exp exp = p.listexp_.get(0);
	    		Value val = executeExp(exp, env);
	    		System.out.println(val.getInt().toString());
	    		return new Undefined();
	    	}
	    	else if(p.id_ == "readDouble"){
	    		
	    		double d = scan.nextDouble();
	    		return new DoubleValue(d);
	    	}
	    	else if(p.id_ == "printDouble"){
	    		
	    		Exp exp = p.listexp_.get(0);
	    		Value val = executeExp(exp, env);
	    		System.out.println(val.getDouble());
	    		return new Undefined();
	    	}
	    	else{
	    		//Execute the function 
	    		FunExecute funExecute = env.lookFun(p.id_);
	    		//Eval args
	    		LinkedList<Value> temp = new LinkedList<Value>();
	    		
	    		for(Exp exp : p.listexp_){
	    			
	    			temp.addLast(executeExp(exp, env));
	    		
	    		}
	    		env.enterScope();
	    		for(String id : funExecute.arguments){
	    			
	    			env.declVar(id);
	    			env.updateVar(id, temp.remove());
	    			
	    		}
	    		for(Stm stm : funExecute.statements){
	    			
	    			Value val = executeStm(stm, env);
	    			if(val != null){
	    				env.leaveScope();
	    				return val;
	    			}
	    		}
	    		env.leaveScope();
	    		return null;
	    		
	    	}
	    }
	    
	    public Value visit(CPP.Absyn.EPostIncr p, Env env)
	    { /* Code For EPostIncr Goes Here */
		    
		     Value val = env.lookVar(p.id_);
		     if(val.isInt()){
		    	 env.updateVar(p.id_, new IntegerValue(val.getInt() + 1 ));
		    	 return val;
		     }
		     else{
		    	 env.updateVar(p.id_, new DoubleValue(val.getDouble() + 1.0 ));
		    	 return val;
		     }
	   }
	    
	    public Value visit(CPP.Absyn.EPostDecr p, Env env)
	    { /* Code For EPostDecr Goes Here */
		    
		     Value val = env.lookVar(p.id_);
		     if(val.isInt()){
		    	 env.updateVar(p.id_, new IntegerValue(val.getInt() - 1 ));
		    	 return val;
		     }
		     else{
		    	 env.updateVar(p.id_, new DoubleValue(val.getDouble() - 1.0 ));
		    	 return val;
		     }  
	    }
	    
	    public Value visit(CPP.Absyn.EPreIncr p, Env env)
	    { /* Code For EPreIncr Goes Here */
	      Value val = env.lookVar(p.id_);
	      if(val.isInt()){
	    	  env.updateVar(p.id_, new IntegerValue(val.getInt() + 1));
	    	  return env.lookVar(p.id_);
	      } else {
	    	  env.updateVar(p.id_, new DoubleValue(val.getDouble() + 1));
	    	  return env.lookVar(p.id_);
	      }
	    }
	    public Value visit(CPP.Absyn.EPreDecr p, Env env)
	    { /* Code For EPreDecr Goes Here */
		      Value val = env.lookVar(p.id_);
		      if(val.isInt()){
		    	  env.updateVar(p.id_, new IntegerValue(val.getInt() - 1));
		    	  return env.lookVar(p.id_);
		      } else {
		    	  env.updateVar(p.id_, new DoubleValue(val.getDouble() - 1.0));
		    	  return env.lookVar(p.id_);
		      }
	    }
	    
	    public Value visit(CPP.Absyn.ETimes p, Env env)
	    { /* Code For ETimes Goes Here */
		     Value val1 = executeExp(p.exp_1,env);
		     Value val2 = executeExp(p.exp_2, env);
		 
		     if(val1.isInt()){
		    	 return new IntegerValue(val1.getInt() * val2.getInt());
		     }
		     else{
		    	 return new DoubleValue(val1.getDouble() * val2.getDouble());    
		     }
	   }
	    
	    public Value visit(CPP.Absyn.EDiv p, Env env)
	    { /* Code For EDiv Goes Here */
		     Value val1 = executeExp(p.exp_1,env);
		     Value val2 = executeExp(p.exp_2, env);
		   
		     if(val1.isInt()){
		    	 return new IntegerValue(val1.getInt() / val2.getInt());
		     }
		     else{
		    	 return new DoubleValue(val1.getDouble() / val2.getDouble());    
		     }
	    }
	    
	    public Value visit(CPP.Absyn.EPlus p, Env env)
	    { /* Code For EPlus Goes Here */
		     Value val1 = executeExp(p.exp_1,env);
		     Value val2 = executeExp(p.exp_2, env);
		    
		     if(val1.isInt()){
		    	 return new IntegerValue(val1.getInt() + val2.getInt());
		     }
		     else{
		    	 return new DoubleValue(val1.getDouble() + val2.getDouble());    
		     }
	    }
	    
	    public Value visit(CPP.Absyn.EMinus p, Env env)
	    { /* Code For EMinus Goes Here */
		     Value val1 = executeExp(p.exp_1,env);
		     Value val2 = executeExp(p.exp_2, env);
		     		    
		     if(val1.isInt()){
		    	 return new IntegerValue(val1.getInt() - val2.getInt());
		     }
		     else{
		    	 return new DoubleValue(val1.getDouble() - val2.getDouble());    
		     }
	    }
	    
	    public Value visit(CPP.Absyn.ELt p, Env env)
	    { /* Code For ELt Goes Here */
	    	
		     Value val1 = executeExp(p.exp_1,env);
		     Value val2 = executeExp(p.exp_2, env);
	     
		     if(val1.isInt()){
		     	
		    	 if(val1.getInt() < val2.getInt()){
		     		return new IntegerValue(1);
		     	}
		    	 else{
		    		 return new IntegerValue(0);      
		    	 }
		     }
		     else{
		    	 
		    	 if(val1.getDouble() < val2.getDouble()){
			     		return new IntegerValue(1);
			     	}
			     	else{ 
			     		return new IntegerValue(0);       
			     }
		     }
	    }
	    
	    public Value visit(CPP.Absyn.EGt p, Env env)
	    { /* Code For EGt Goes Here */
	    	
	    	Value val1 = executeExp(p.exp_1,env);
		     Value val2 = executeExp(p.exp_2, env);
	     
		     if(val1.isInt()){
		     	
		    	 if(val1.getInt() > val2.getInt()){
		     		return new IntegerValue(1);
		     	}
		    	 else{
		    		 return new IntegerValue(0);      
		    	 }
		     }
		     else{
		    	 
		    	 if(val1.getDouble() > val2.getDouble()){
			     		return new IntegerValue(1);
			     	}
			     	else{ 
			     		return new IntegerValue(0);       
			     }
		     }
	    }
	    
	    public Value visit(CPP.Absyn.ELtEq p, Env env)
	    { /* Code For ELtEq Goes Here */
	    	 Value val1 = executeExp(p.exp_1,env);
		     Value val2 = executeExp(p.exp_2, env);
	     
		     if(val1.isInt()){
		     	
		    	 if(val1.getInt() <= val2.getInt()){
		     		return new IntegerValue(1);
		     	}
		    	 else{
		    		 return new IntegerValue(0);      
		    	 }
		     }
		     else{
		    	 
		    	 if(val1.getDouble() <= val2.getDouble()){
			     		return new IntegerValue(1);
			     	}
			     	else{ 
			     		return new IntegerValue(0);       
			     }
		     }
	    }
	    
	    public Value visit(CPP.Absyn.EGtEq p, Env env)
	    { /* Code For EGtEq Goes Here */
	    	 Value val1 = executeExp(p.exp_1,env);
		     Value val2 = executeExp(p.exp_2, env);
	     
		     if(val1.isInt()){
		     	
		    	 if(val1.getInt() >= val2.getInt()){
		     		return new IntegerValue(1);
		     	}
		    	 else{
		    		 return new IntegerValue(0);      
		    	 }
		     }
		     else{
		    	 
		    	 if(val1.getDouble() >= val2.getDouble()){
			     		return new IntegerValue(1);
			     	}
			     	else{ 
			     		return new IntegerValue(0);       
			     }
		     }
	    }
	    
	    public Value visit(CPP.Absyn.EEq p, Env env)
	    { /* Code For EEq Goes Here */
	    	 
	    	Value val1 = executeExp(p.exp_1,env);
		     Value val2 = executeExp(p.exp_2, env);
	     
		     if(val1.isInt()){
		    	 
		    	 Integer val1Int = val1.getInt();
		    	 Integer val2Int = val2.getInt();
		    	 
		    	 if(val1Int.equals(val2Int)){
		     		return new IntegerValue(1);
		     	}
		    	 else{
		    		 return new IntegerValue(0);      
		    	 }
		     }
		     else{
		    	 
		    	 Double val1Double = val1.getDouble();
		    	 Double val2Double = val2.getDouble();
		    	 if(val1Double .equals (val2Double)){
			     		return new IntegerValue(1);
			     	}
			     	else{ 
			     		return new IntegerValue(0);       
			     }
		     }
	    }
	    
	    public Value visit(CPP.Absyn.ENEq p, Env env)
	    { /* Code For ENEq Goes Here */
	    	Value val1 = executeExp(p.exp_1,env);
		     Value val2 = executeExp(p.exp_2, env);
	     
		     if(val1.isInt()){
		    	 
		    	 Integer val1Int = val1.getInt();
		    	 Integer val2Int = val2.getInt();
		    	 
		    	 if(!(val1Int.equals(val2Int))){
		     		return new IntegerValue(1);
		     	}
		    	 else{
		    		 return new IntegerValue(0);      
		    	 }
		     }
		     else{
		    	 
		    	 Double val1Double = val1.getDouble();
		    	 Double val2Double = val2.getDouble();
		    	 if(!(val1Double.equals(val2Double))){
			     		return new IntegerValue(1);
			     	}
			     	else{ 
			     		return new IntegerValue(0);       
			     }
		     }
	    }
	    
	    public Value visit(CPP.Absyn.EAnd p, Env env)
	    { /* Code For EAnd Goes Here */
	    	Value val1 = executeExp(p.exp_1, env);
	    	
	    	if(val1.isInt()){
	    		Integer intVal1 = val1.getInt(); 

	    		if(intVal1.equals(0)){
	    			return new IntegerValue(0);
	    		}
	    		
	    		Value val2 = executeExp(p.exp_2, env);
	    		Integer intVal2 = val2.getInt(); 
	    		
	    		if(intVal2.equals(1)){
	    			return new IntegerValue(1);
	    		}
	    		else {
	    			return new IntegerValue(0);
	    		}
	    	
	    	}
	    	else{
	    		
	    
	    	
	    		Double DoubleVal1 = val1.getDouble(); 
	    		
	    		if(DoubleVal1.equals(0.0)){
	    			return new IntegerValue(0);
	    		}
	    		
	    		Value val2 = executeExp(p.exp_2, env);
	    		Double DoubleVal2 = val2.getDouble(); 
	    	
	    		if(DoubleVal2.equals(1.0)){
	    			return new IntegerValue(1);
	    		}
	    	
	    		else {
	    			return new IntegerValue(0); 
	    		}
	    		
	    	}
	    }
	    
	    public Value visit(CPP.Absyn.EOr p, Env env)
	    { /* Code For EOr Goes Here */
	    	Value val1 = executeExp(p.exp_1, env);
	
	    	if(val1.isInt()){
	    		
	    		Integer intVal1 = val1.getInt(); 
		    	
	    		if(intVal1.equals(1)){
	    			return new IntegerValue(1);
	    		}
	    		
	    		Value val2 = executeExp(p.exp_2, env);
	    		Integer intVal2 = val2.getInt(); 
	    		
	        	if(intVal2.equals(1)){
	    			return new IntegerValue(1);
	    		}
	    	
	    		else {
	    			return new IntegerValue(0); 
	    		}
	    	}
	    	else{
	    		
	    		Double DoubleVal1 = val1.getDouble();
	    		
	    		if(DoubleVal1.equals(1.0)){
	    			return new IntegerValue(1);
	    		}
	    		
	    		Value val2 = executeExp(p.exp_2, env);
	    		Double DoubleVal2 = val2.getDouble(); 
	    	
	    		if(DoubleVal2.equals(1.0)){
	    			return new IntegerValue(1);
	    		}
	    	
	    		else {
	    			return new IntegerValue(0); 
	    		}
	    		
	    	}
	    }
	    
	    public Value visit(CPP.Absyn.EAss p, Env env)
	    { /* Code For EAss Goes Here */

	    	Value val = executeExp(p.exp_, env);
	    	
	    	env.updateVar(p.id_, val);
	    	return val;
	    }
	    
	  }
}
