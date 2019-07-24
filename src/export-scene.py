import bpy
bpy.ops.export_scene.gltf(filepath=bpy.data.filepath.replace(".blend", ".glb"))
