LIBKC3DS_PY += libkc3ds/__init__.py
LIBKC3DS_PY += libkc3ds/s16.py
LIBKC3DS_PY += libkc3ds/s16pil.py
LIBKC3DS_PY += libkc3ds/cpx.py
LIBKC3DS_PY += libkc3ds/bitdither.py
LIBKC3DS_PY += libkc3ds/parts.py
LIBKC3DS_PY += libkc3ds/aging.py
LIBKC3DS_PY += libkc3ds/_aging_defs.py
LIBKC3DS_PY += libkc3ds/_aging_dsc.py
LIBKC3DS_PY += libkc3ds/_aging_c3g.py
LIBKC3DS_PY += libkc3ds/_aging_c3e.py
LIBKC3DS_ABS = $(addprefix python/,$(LIBKC3DS_PY))

KC3DSBPY_PY += kc3dsbpy/__init__.py
KC3DSBPY_PY += kc3dsbpy/imaging.py
KC3DSBPY_PY += kc3dsbpy/dataext.py
KC3DSBPY_PY += kc3dsbpy/gizmo.py
KC3DSBPY_PY += kc3dsbpy/framereq.py
KC3DSBPY_ABS = $(addprefix python/,$(KC3DSBPY_PY))

PYTHON_REL += python/README.md

PYTHON_REL += $(LIBKC3DS_ABS)

PYTHON_REL += python/caosterm.py python/cpxinfo.py python/inject.py
PYTHON_REL += python/bdmptest.py python/maprooms.py python/maptypes.py
PYTHON_REL += python/wastemon.py

PYTHON_REL += python/s16tool.py
PYTHON_REL += python/c3breedie.py
PYTHON_REL += python/c3breedsizes.py

PYTHON_REL += python/cpxciesv.py

INTERMEDIATES += python/blender-addons.zip
all: python/blender-addons.zip

python/blender-addons.zip: $(LIBKC3DS_ABS) $(KC3DSBPY_ABS)
	rm -f $@
	cd python && zip -r -n .pyc blender-addons.zip $(LIBKC3DS_PY) $(KC3DSBPY_PY)

rel: $(PYTHON_REL)
rel-sdk: $(PYTHON_REL)

