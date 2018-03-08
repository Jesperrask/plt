// Function types

import java.util.*;
import CPP.Absyn.*;

public class FunType {
  final Type returnType;
  final ListArg args;
  final String ref;
  public FunType (String javaRef, Type t, ListArg l) {
	this.ref = javaRef;
    returnType = t;
    args = l;
  }
  public String toJVM () {
   String argTypes = "";
   for (Arg a: args) {
     ADecl decl = (ADecl)a;
     argTypes = argTypes + decl.type_.accept (new TypeVisitor (), null);
   }
   return "(" + argTypes + ")" + returnType.accept (new TypeVisitor(), null);
  }
}

class TypeVisitor implements Type.Visitor<String,Void>
{
  public String visit(CPP.Absyn.Type_bool p, Void arg)
  {
    return "Z";
  }
  public String visit(CPP.Absyn.Type_int p, Void arg)
  {
    return "I";
  }
  public String visit(CPP.Absyn.Type_double p, Void arg)
  {
    return "D";
  }
  public String visit(CPP.Absyn.Type_void p, Void arg)
  {
    return "V";
  }
}
