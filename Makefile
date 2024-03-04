all: NanoMorphoCompiler.class NanoMorphoLexer.class

NanoMorphoCompiler.class: NanoMorphoCompiler.java NanoMorphoLexer.class
	javac $<

NanoMorphoLexer.class: NanoMorphoLexer.java
	javac $^

NanoMorpo.class: NanoMorpho.java
	javac $^

NanoMorpho.java: NanoMorpho.y
	bison $^

NanoMorphoLexer.java: NanoMorphoLexer.jflex
	~/.local/bin/jflex NanoMorphoLexer.jflex

.PHONY: test
test: NanoMorpho.class NanoMorphoLexer.class
	java NanoMorpho test.nm

.PHONY: clean
clean:
	rm *.class
