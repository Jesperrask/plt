import CPP.Absyn.*;
import java.util.*;
import java.util.LinkedList;

public class TypeChecker {
	public static enum TypeCode {
		Type_int, Type_void, Type_bool, Type_double
	}
	
	public static class FunType{
		public LinkedList<TypeCode> args;
		public TypeCode returnType;
	}

	public static class Env{
		public HashMap<String, FunType> signature;
		public LinkedList<HashMap<String, TypeCode>> contexts;
		public int returnFlag;

		public Env(){
			contexts = new LinkedList<HashMap<String, TypeCode>>();
			signature = new HashMap<String, FunType>();
			returnFlag = 0;
			enterScope();
		}

		public TypeCode lookVar(String id){
			for (HashMap<String, TypeCode> context : contexts) {
				TypeCode type = context.get(id);
				if (type != null) {
					return type;
				}
			}
			throw new TypeException("There is no [" + id + "] function");
		}

		public boolean isFunDecl(String id){
			return signature.containsKey(id);
		}

		public boolean isVarDecl(String id){
			// just check if the current scope contains the variable
			TypeCode type;
			HashMap<String, TypeCode> context = contexts.getFirst();
			type = context.get(id);
			if (type != null) {
				return true;
			}
			return false;
		}

		public FunType lookFun(String id){
			FunType funcType = signature.get(id);
			if (funcType == null) {
				throw new TypeException("There is no [" + id + "] function");
			} else {
				return funcType;
			}
		}
        //checks if the variable is already declared
		public void updateVar(String id, TypeCode ty){
			HashMap<String, TypeCode> currentScope = contexts.getFirst();
			if (isVarDecl(id)) {
				throw new TypeException("The variable [ " + id + "] has already been declared");
			} else {
				currentScope.put(id, ty);
			}
		}

		public void enterScope(){
			contexts.addFirst(new HashMap<String, TypeCode>());
		}

		public void leaveScope(){
			contexts.removeFirst();
		}

	}
	 public static String isVar (Exp e) {
		    if (e instanceof EId) {
		      return ((EId)e).id_;
		    } else throw new TypeException("expected variable, found " + e);
		  }

	public void typecheck(Program p) {
		PDefs defs = (PDefs) p;
		Env env = new Env();
		
		//Add read/print for ints and doubles
		FunType readInt = new FunType(); 
		readInt.returnType = TypeCode.Type_int ; 
        readInt.args = new LinkedList<TypeCode>() ;
        env.signature.put("readInt", readInt) ;
		
        FunType readDouble = new FunType(); 
        readDouble.returnType = TypeCode.Type_double ;
        readDouble.args = new LinkedList<TypeCode>() ;
        env.signature.put("readDouble", readDouble) ;
		
        FunType printInt = new FunType(); 
		printInt.returnType = TypeCode.Type_void ;
        printInt.args = new LinkedList<TypeCode>() ;
        printInt.args.addLast(TypeCode.Type_int);
        env.signature.put("printInt", printInt) ;
		
        FunType printDouble = new FunType(); 
		printDouble.returnType = TypeCode.Type_void ;
        printDouble.args = new LinkedList<TypeCode>() ;
        printDouble.args.addLast(TypeCode.Type_double);
        env.signature.put("printDouble", printDouble) ;

		for(Def def : defs.listdef_){
			//check function decl
			checkFunDef(def, env);
		}
		if(!env.isFunDecl("main")){
			throw new TypeException("There is no main function declared");
		}
		
		for(Def def : defs.listdef_){
			//check statements in functions
			checkStmDef(def, env);
			if(env.returnFlag == 0){
				throw new TypeException("No return statement");
			}else{
				env.returnFlag = 0;
			}
		}
	}

	private void checkFunDef(Def d, Env env){
		d.accept(new defFunAdd(), env);
	}
	private void checkStmDef(Def d, Env env){
		d.accept(new checkFunStmDef(), env);
	}
	
	private class defFunAdd implements Def.Visitor<Object , Env> {
		
		public Env visit(DFun p, Env env){
			
			if(env.isFunDecl(p.id_)){
				throw new TypeException("Error DFun: The function is already declared");
			}
			
			if(!(p.listarg_.isEmpty()) && p.id_ == "main"){
				throw new TypeException("Error DFun: Should not be args in main");
			}
			
			
			
			FunType funType = new FunType();
			funType.args = new LinkedList<TypeCode>();
			funType.returnType = getTypeCode(p.type_);
			
			
			for( Arg arg : p.listarg_){
				funType.args.addLast(getTypeCode(((ADecl)arg).type_));
			}
			
			env.signature.put(p.id_, funType);
			
			return env;
		}
	}

	
	private class checkFunStmDef implements Def.Visitor<Object , Env>{
		
		public Env visit(DFun p, Env env){
			env.enterScope();
			
			ADecl temp;
			//Beacouse "return" is reserved we add it to the variables, so that it cant be taken
			
			
			for(Arg arg : p.listarg_){
				temp = (ADecl)arg;
				env.updateVar(temp.id_, getTypeCode(temp.type_));
			}
			TypeCode returnType = getTypeCode(p.type_);
			env.updateVar("return", returnType);
			
			if(p.liststm_.isEmpty() && p.id_ == "main"){
				throw new TypeException("Error DFun: no stm");
		}
				
			//Check if the type is void or main, then we should not need a return
				if(returnType == TypeCode.Type_void || p.id_ == "main"){
						env.returnFlag = 1;
				}
			//Add all stm's to the environment
			for(Stm stm : p.liststm_){
				stmCheck(stm, env);
			}
			env.leaveScope();
			
			return env;
		
		}
	}
	
	public void stmCheck(Stm x, Env env){
		x.accept(new CheckStm(), env);
	}
	

	// ... checking different statements
	public class CheckStm implements Stm.Visitor<Object, Env>{
		
		public Object visit(SExp p, Env env) {
			checkExpression(p.exp_, env);
			return null;
		}

		public Object visit(SDecls p, Env env){
			TypeCode type = getTypeCode(p.type_);
			for ( String s : p.listid_){
			env.updateVar(s, type);
			}
			return null;
		}

		public Object visit(SInit p, Env env){
			if(env.isVarDecl(p.id_)){
				throw new TypeException("SInit error: variable name already exists");
			}
			
			TypeCode eType = checkExpression(p.exp_, env);
			TypeCode tType = getTypeCode(p.type_);
			
			if(eType == tType){
				env.updateVar(p.id_, tType);
			}else{
				throw new TypeException("SInit error: Type does not match");
			}
			return null;
		}

		public Object visit(SReturn p, Env env){
			
			TypeCode eType = checkExpression(p.exp_, env);
			TypeCode rType = env.lookVar("return");
			
			if(eType != rType){
				throw new TypeException("SReturn error: Type does not match");
			}
			
			env.returnFlag = 1;
			
			return null;
		}

		public Object visit(SWhile p, Env env){
			TypeCode eType = checkExpression(p.exp_, env);
			
			if(eType != TypeCode.Type_bool){
				throw new TypeException("SWhile error: Type does not match");
			}
			env.enterScope();
			stmCheck(p.stm_, env);
			env.leaveScope();
			return null;
		}

		public Object visit(SBlock p, Env env){
			
			env.enterScope();
			for(Stm s : p.liststm_){
				stmCheck(s, env);
			}
			
			env.leaveScope();
			
			return null;
		}

		public Object visit(SIfElse p, Env env){
			TypeCode eType = checkExpression(p.exp_, env);
			if(eType != TypeCode.Type_bool){
				throw new TypeException("SIfElse error: Type does not match");
			}
			env.enterScope();
			stmCheck(p.stm_1, env);
			env.leaveScope();
			env.enterScope();
			stmCheck(p.stm_2, env);
			env.leaveScope();
			
			return null;			
		}
	}
	
	public TypeCode checkExpression(Exp exp, Env env){
		return exp.accept(new InferExp(), env);
	};

	private class InferExp implements Exp.Visitor<TypeCode, Env>{
		
		public TypeCode visit(ETrue p, Env env) {
			return TypeCode.Type_bool;
		}

		public TypeCode visit(EFalse p, Env env){
			return TypeCode.Type_bool;
		}

		public TypeCode visit(EInt p, Env env){
			return TypeCode.Type_int;
		}

		public TypeCode visit(EDouble p, Env env){
			return TypeCode.Type_double;
		}

		public TypeCode visit(EId p, Env env){
			return env.lookVar(p.id_);
		}

		
		public TypeCode visit(EApp p, Env env){
			FunType funType = env.lookFun(p.id_);
			int temp = 0, argSize1 = funType.args.size(), argSize2 = p.listexp_.size();
			if(argSize1 != argSize2){
				throw new TypeException("EApp error: Length of args are not the same size");
			}
			for(Exp exp : p.listexp_){
				if(checkExpression(exp, env) != funType.args.get(temp)){
					throw new TypeException("EApp error: "+ temp + "args doesnt match");
				} else {
					temp++;
				}
			}
				return funType.returnType;
		}

		public TypeCode visit(EPostIncr p, Env env){
			
			TypeCode type = checkExpression(p.exp_, env);
			if(type == TypeCode.Type_bool){
				throw new TypeException("EPostIncr error : can't increment boolean");
			}
			return type;
		}

		public TypeCode visit(EPostDecr p, Env env){
			TypeCode type = checkExpression(p.exp_, env);
			if(type == TypeCode.Type_bool){
				throw new TypeException("EPostDecr error : can't decrement boolean");
			}
			return type;
		}

		public TypeCode visit(EPreIncr p, Env env){ 
			TypeCode type = checkExpression(p.exp_, env);
			if(type == TypeCode.Type_bool){
				throw new TypeException("EPreIncr error : can't increment boolean");
			}
			return type;
			
		}

		public TypeCode visit(EPreDecr p, Env env){
			TypeCode type = checkExpression(p.exp_, env);
			if(type == TypeCode.Type_bool){
				throw new TypeException("EPreDecr error : can't decrement boolean");
			}
			return type;
		}

		public TypeCode visit(ETimes p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != type2){
				throw new TypeException("ETimes error : Expressions does not have the same type");
			}else if(type1 == TypeCode.Type_bool || type2 ==  TypeCode.Type_bool){
				throw new TypeException("ETimes error : Expression is boolean");
			}
			return type2;
		}

		public TypeCode visit(EDiv p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != type2){
				throw new TypeException("EDiv error : Expressions does not have the same type");
			}else if(type1 == TypeCode.Type_bool || type2 ==  TypeCode.Type_bool){
				throw new TypeException("EDiv error : Expression is boolean");
			}
			return type2;
		}

		public TypeCode visit(EPlus p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != type2){
				throw new TypeException("EPlus error : Expressions does not have the same type");
			}else if(type1 == TypeCode.Type_bool || type2 ==  TypeCode.Type_bool){
				throw new TypeException("EPlus error : Expression is boolean");
			}
			return type2;
		}
	

		public TypeCode visit(EMinus p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != type2){
				throw new TypeException("EMinus error : Expressions does not have the same type");
			}else if(type1 == TypeCode.Type_bool || type2 ==  TypeCode.Type_bool){
				throw new TypeException("EMinus error : Expression is boolean");
			}
			return type2;
		}


		public TypeCode visit(ELt p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != type2){
				throw new TypeException("ELt error : Expressions does not have the same type");
			}
			return TypeCode.Type_bool;
		}

		public TypeCode visit(EGt p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != type2){
				throw new TypeException("EGt error : Expressions does not have the same type");
			}
			return TypeCode.Type_bool;
		}

		public TypeCode visit(ELtEq p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != type2){
				throw new TypeException("ELtEg error : Expressions does not have the same type");
			}
			return TypeCode.Type_bool;
		}

		public TypeCode visit(EGtEq p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != type2){
				throw new TypeException("EGtEq error : Expressions does not have the same type");
			}
			return TypeCode.Type_bool;
		}

		public TypeCode visit(EEq p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != type2){
				throw new TypeException("EEq error : Expressions does not have the same type");
			}
			return TypeCode.Type_bool;
		}

		public TypeCode visit(ENEq p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != type2){
				throw new TypeException("ENEq error : Expressions does not have the same type");
			}
			return TypeCode.Type_bool;
		}

		public TypeCode visit(EAnd p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != TypeCode.Type_bool || type2 != TypeCode.Type_bool ){
				throw new TypeException("EAnd error : Expressions must be of type Boolean");
			}
			return TypeCode.Type_bool;
		}

		public TypeCode visit(EOr p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 != TypeCode.Type_bool || type2 != TypeCode.Type_bool ){
				throw new TypeException("EOr error : Expressions must be of type Boolean");
			}
			return TypeCode.Type_bool;
		}

		public TypeCode visit(EAss p, Env env){
			TypeCode type1 = checkExpression(p.exp_1, env);
			TypeCode type2 = checkExpression(p.exp_2, env);
			
			if(type1 !=  type2){
				throw new TypeException("EAss error : Expressions does not have the same type");
			}
			return type2;
		}

		// ... inferring types of different expressions
	}
    // function for TypeCode
    private TypeCode getTypeCode(Type type){

        return type.accept(new TypeCoder(), null);
    }
    
    private class TypeCoder implements Type.Visitor<TypeCode , Object>{
    	
        public TypeCode visit(Type_bool type, Object arg)
        {
            return TypeCode.Type_bool; 
        }
        public TypeCode visit(Type_int type, Object arg)
        {
            return TypeCode.Type_int;
        }
        public TypeCode visit(Type_double type, Object arg)
        {
            return TypeCode.Type_double;
        }
        public TypeCode visit(Type_void type, Object arg)
        {
            return TypeCode.Type_void;
        }

    }

}

