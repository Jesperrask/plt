# Makefile for lab3 in java1.5

JAVAC = javac
JAVAC_FLAGS = -sourcepath .

JAVA = java

# Original grammar
grm=CC

# Name of generated .cup file for bnfc 2.8
# Was $(grm)/$(grm).cup for older versions of bnfc
CUPFILE = $(grm)/_cup.cup

test=test

.PHONY: bnfc lab3 clean distclean vclean test

default: lab3

current: Compiler.class Code.class FunType.class TypeChecker.class lab3 lab3.class test

all: bnfc lab3

test: test/$(test).cc lab3.class Compiler.class Runtime.class
	lab3-test $<

%.class : %.java
	${JAVAC} ${JAVAC_FLAGS} $<

lab3: Runtime.class FunType.class TypeChecker.class Code.class Compiler.class
	${JAVAC} ${JAVAC_FLAGS} lab3.java
	chmod a+x lab3

bnfc:
	bnfc -java1.5 $(grm).cf
	${JAVA} ${JAVA_FLAGS} JLex.Main $(grm)/Yylex
	${JAVA} ${JAVA_FLAGS} java_cup.Main -nopositions -expect 100 $(CUPFILE)
	mv sym.java parser.java $(grm)

clean:
	 -rm -f $(grm)/Absyn/*.class $(grm)/*.class
	 -rm -f .dvi $(grm).aux $(grm).log $(grm).ps  *.class

distclean: vclean

vclean: clean
	 -rm -f $(grm)/Absyn/*.java
	 -rmdir $(grm)/Absyn/
	 -rm -f $(grm).tex $(grm).dvi $(grm).aux $(grm).log $(grm).ps
	 -rm -f $(grm)/Yylex $(CUPFILE) $(grm)/Yylex.java $(grm)/VisitSkel.java $(grm)/ComposVisitor.java $(grm)/AbstractVisitor.java $(grm)/FoldVisitor.java $(grm)/AllVisitor.java $(grm)/PrettyPrinter.java $(grm)/Skeleton.java $(grm)/Test.java $(grm)/sym.java $(grm)/parser.java $(grm)/*.class
	 -rmdir -p $(grm)/

# EOF
