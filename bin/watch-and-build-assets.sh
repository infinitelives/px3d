#!/bin/bash

d=public/models
i=$d/assets.blend
o=$d/assets.glb
a=src/px3d/assets.cljs

echo "Starting blend file rebuilder on $i"
while [ 1 ]
do
  if [ "$i" -nt "$o" ]
  then
    echo "Rebuilding $o"
    blender --background $i --python "src/export-scene.py" > /dev/null
    echo "Wrote $o"
    checksum=`sha256sum $o | cut -b1-8`
    echo -e "(ns px3d.assets)\n(def checksum 0x$checksum)" > $a
    echo "Wrote $a"
  fi
  sleep 0.1
done
