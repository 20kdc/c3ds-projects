CAOSPROX_REL := caosproxy/README.md caosproxy/spec.txt caosproxy/cpxciesv.py
include caosproxy/w32/index.mk
include caosproxy/tools/index.mk

rel: $(CAOSPROX_REL)
rel-sdk: $(CAOSPROX_REL)

