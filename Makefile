STATIC=index.html scene.js css/main.css models/assets.glb

all: build/js/app.js $(foreach S, $(STATIC), build/$(S))

build/js/app.js: $(shell find src) package.json shadow-cljs.edn public/models/assets.glb
	npx shadow-cljs compile prod

build/%: public/%
	@mkdir -p `dirname $@`
	cp $< $@

public/models/assets.glb: public/models/assets.blend
	PROD=1 bin/watch-and-build-assets.sh

.PHONY: watch clean

watch:
	./bin/watch-and-build-assets.sh &
	npx shadow-cljs watch app

clean:
	rm -rf build/*
	lein clean
