all: nano-morpho.jar

nano-morpho.jar: src/nano-morpho.jar
	cp src/nano-morpho.jar nano-morpho.jar

.PHONY: src/nano-morpho.jar
src/nano-morpho.jar:
	$(MAKE) -C src

.PHONY: clean
clean:
	find . -name "nano-morpho.jar" -delete
	$(MAKE) -C src clean
