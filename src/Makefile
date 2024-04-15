JFLEX = ~/.local/bin/jflex
BISON = bison

CLASSES=$(addprefix target/, $(addsuffix .class, Compiler NanoMorphoLexer NanoMorphoParser SymbolTable))

all: nano-morpho.jar

nano-morpho.jar: ${CLASSES}
	cd target && jar cfe $@ $(basename $(notdir $<)) *.class && mv $@ ..

target/%.class: %.java
	javac -d target $^

target/Compiler.class target/NanoMorphoLexer.class: Compiler.java NanoMorphoLexer.java NanoMorpho.java

NanoMorphoLexer.java: NanoMorpho.jflex
	${JFLEX} $^

NanoMorphoParser.class NanoMorphoParser$$Lexer.class: NanoMorpho.java

NanoMorpho.java: NanoMorpho.y
	${BISON} $^

.PHONY: clean
clean:
	find . -name "*.class" -delete
	find . -name NanoMorpho.java -delete
	find . -name NanoMorphoLexer.java -delete
	find . -name "*.jar" -delete
	find . -name "target" -delete
