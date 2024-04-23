all: nano-morpho.jar doc

nano-morpho.jar: src/nano-morpho.jar
	cp src/nano-morpho.jar nano-morpho.jar

.PHONY: src/nano-morpho.jar
src/nano-morpho.jar:
	$(MAKE) -C src

.PHONY: doc
doc:
	$(MAKE) -C doc

.PHONY: clean
clean:
	find . -name "nano-morpho.jar" -delete
	$(MAKE) -C src clean
	$(MAKE) -C doc clean
