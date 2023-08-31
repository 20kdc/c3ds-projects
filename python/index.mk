PYTHON_REL += python/README.md

PYTHON_REL += python/libkc3ds/s16.py
PYTHON_REL += python/libkc3ds/s16pil.py
PYTHON_REL += python/libkc3ds/cpx.py
PYTHON_REL += python/libkc3ds/bitdither.py
PYTHON_REL += python/libkc3ds/parts.py

PYTHON_REL += python/kc3dsbpy/__init__.py
PYTHON_REL += python/kc3dsbpy/imaging.py
PYTHON_REL += python/kc3dsbpy/database.py
PYTHON_REL += python/kc3dsbpy/dataext.py
PYTHON_REL += python/kc3dsbpy/gizmo.py

PYTHON_REL += python/caosterm.py python/cpxinfo.py python/inject.py
PYTHON_REL += python/bdmptest.py python/maprooms.py python/maptypes.py
PYTHON_REL += python/wastemon.py

PYTHON_REL += python/s16tool.py
PYTHON_REL += python/c3breedie.py

PYTHON_REL += python/cpxciesv.py

rel: $(PYTHON_REL)
rel-sdk: $(PYTHON_REL)

