all: NanoMorpo.class NanoMorphoLexer.class

NanoMorpo.class NanoMorphoLexer.class: NanoMorpho.java NanoMorphoLexer.java
	javac $^

NanoMorpho.java: NanoMorpho.y
	bison $^

NanoMorphoLexer.java: NanoMorphoLexer.jflex
	jflex NanoMorphoLexer.jflex

