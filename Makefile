all: NanoMorphoParser.class NanoMorphoLexer.class

NanoMorphoParser.class: NanoMorphoParser.java NanoMorphoLexer.class
	javac $<

NanoMorphoLexer.class: NanoMorphoLexer.java
	javac $^

NanoMorpo.class: NanoMorpho.java
	javac $^

NanoMorpho.java: NanoMorpho.y
	bison $^

NanoMorphoLexer.java: NanoMorphoLexer.jflex
	~/.local/bin/jflex NanoMorphoLexer.jflex

.PHONY test: NanoMorpho.class NanoMorphoLexer.class
	java NanoMorpho test.nm
