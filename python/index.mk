PYTHON_REL += python/README.md

PYTHON_REL += python/libkc3ds/s16.py
PYTHON_REL += python/libkc3ds/cpx.py

PYTHON_REL += python/caosterm.py python/cpxinfo.py python/inject.py
PYTHON_REL += python/bdmptest.py python/maprooms.py python/maptypes.py
PYTHON_REL += python/wastemon.py

PYTHON_REL += python/c3breedie.py
PYTHON_REL += python/cpxciesv.py

rel: $(PYTHON_REL)
rel-sdk: $(PYTHON_REL)

