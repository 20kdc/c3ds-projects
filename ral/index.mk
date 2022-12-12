# RAL packaging as part of c3ds-projects

RAL_REL := ral/ral.jar ral/ral ral/ral.cmd ral/raljector ral/raljector.cmd ral/raltest ral/raltest.cmd

# RAL executable itself

.PHONY: ral/ral.jar
ral/ral.jar:
	cd ral ; ./build-scripts/all

# RAL support infrastructure
RAL_REL += ral/include/std ral/syntax

# RAL manual
RAL_REL += ral/manual.pdf ral/manual.html ral/samples

rel: $(RAL_REL)
rel-sdk: $(RAL_REL)
