all:
	happy -gca ParLab.y
	alex -g LexLab.x
	ghc --make TestLab.hs -o TestLab

clean:
	-rm -f *.log *.aux *.hi *.o *.dvi

distclean: clean
	-rm -f DocLab.* LexLab.* ParLab.* LayoutLab.* SkelLab.* PrintLab.* TestLab.* AbsLab.* TestLab ErrM.* SharedString.* ComposOp.* lab1.dtd XMLLab.* Makefile*
	

