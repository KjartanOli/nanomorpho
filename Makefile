all: NanoMorpho.class

%.class: %.java
	javac $^

NanoMorpho.class NanoMorphoLexer.class SymbolTable.class: NanoMorpho.java NanoMorphoLexer.java SymbolTable.java
	javac $^

NanoMorpho.java: NanoMorpho.y
	bison $^

NanoMorphoLexer.java: NanoMorpho.jflex
	~/.local/bin/jflex $^

tests/test%.out: tests/test%.nm Compiler.class
	java Compiler $< > tests/$@.out

.PHONY: clean
clean:
	find . -name "*.class" -delete
	find . -name NanoMorpho.java -delete
	find . -name NanoMorphoLexer.java -delete
