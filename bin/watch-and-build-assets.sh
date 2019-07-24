#!/bin/bash

d=public/models
i=$d/assets.blend
o=$d/assets.glb

while [ 1 ]
do
  if [ "$i" -nt "$o" ]
  then
    blender --background $i --python "src/export-scene.py"
    checksum=`sha256sum $o | cut -b1-8`
    echo -e "(ns 'px3d.assets)\n(def checksum 0x$checksum)" > src/px3d/assets.cljs
  fi
  sleep 0.1
done
