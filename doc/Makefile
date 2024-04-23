FIGS=$(addsuffix .png, fun prog int double char string ifexpr cond match expr name body decl stmt while for opname binop unop digit octal alpha)
TEXI=$(addsuffix .texi, nano-morpho version fdl)

all: nano-morpho.info nano-morpho.pdf

nano-morpho.pdf: ${TEXI} ${FIGS}
	makeinfo --pdf $<

nano-moprho.info: ${TEXI} ${FIGS}
	makeinfo $<

%.png: %.tex
	pdflatex --shell-escape $^

.PHONY: clean
clean:
	find . -name "*.png" -delete
	find . -name "nano-morpho.pdf" -delete
