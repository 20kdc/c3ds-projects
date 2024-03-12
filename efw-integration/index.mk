INTERMEDIATES += efw-integration/efw.zip

all: efw-integration/efw.zip
rel: efw-integration/efw.zip

efw-integration/efw.zip: efw-integration/verbatim/server.cfg
	rm -f efw-integration/efw.zip
	cd efw-integration/verbatim ; zip ../efw.zip server.cfg
