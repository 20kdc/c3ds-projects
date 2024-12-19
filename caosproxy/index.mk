CAOSPROX_REL := caosproxy/README.md caosproxy/spec.txt
include caosproxy/w32/index.mk

rel: $(CAOSPROX_REL)
rel-sdk: $(CAOSPROX_REL)
