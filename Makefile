STATIC=index.html scene.js css/main.css models/assets.glb

all: build/js/app.js $(foreach S, $(STATIC), build/$(S))

build/js/app.js: $(shell find src) project.clj public/models/assets.glb
	lein package

build/%: public/%
	@mkdir -p `dirname $@`
	cp $< $@

public/models/assets.glb: public/models/assets.blend
	PROD=1 bin/watch-and-build-assets.sh

clean:
	rm -rf build/*
	lein clean
