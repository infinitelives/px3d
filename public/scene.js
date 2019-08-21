var container, stats;
var camera, controls, scene, renderer, clock, mixers, gameloop;
var mixers = [];
var objects = [];

var PIXELATE = 4;

init();

function init() {
  
  clock = new THREE.Clock();

  container = document.createElement( 'div' );
  document.body.appendChild( container );

  camera = new THREE.PerspectiveCamera( 70, window.innerWidth / window.innerHeight, 1, 5000 );
  camera.position.set(10, 10, -10);

  scene = new THREE.Scene();
  scene.background = new THREE.Color( 0xf0f0f0 );

  scene.add( new THREE.AmbientLight( 0xffffff, 1.0 ) );

  var light = new THREE.SpotLight( 0xffffff, 1.0 );
  light.position.set( 100, 100, 100 );
  light.castShadow = true;
  // light.shadowDarkness = 0.5;
  light.shadow.camera.near = 10;
  light.shadow.camera.far = 400;
  light.shadow.mapSize.width = 1024;
  light.shadow.mapSize.height = 1024;
  //light.shadow.bias = 0.0001;
  light.angle = Math.PI / 12;
  scene.add( light );

  var light = new THREE.DirectionalLight( 0xffffff, 0.5 );
  light.position.set( 200, 200, 200 );
  //light.castShadow = true;
  //light.shadow.camera.near = 1000;
  //light.shadow.camera.far = 4000;
  //light.shadow.mapSize.width = 1024;
  //light.shadow.mapSize.height = 1024;
  //light.angle = Math.PI / 3;
  scene.add( light );

  renderer = new THREE.WebGLRenderer( { antialias: false } );
  renderer.setPixelRatio( window.devicePixelRatio );
  renderer.setSize( window.innerWidth / PIXELATE, window.innerHeight / PIXELATE );

  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFShadowMap;
  renderer.gammaOutput = true;

  container.appendChild( renderer.domElement );

  controls = new THREE.OrbitControls( camera, renderer.domElement );
  controls.rotateSpeed = 1.0;
  controls.zoomSpeed = 1.2;
  controls.panSpeed = 0.8;
  controls.enableZoom = true;
  controls.enablePan = true;
  controls.staticMoving = true;
  controls.dynamicDampingFactor = 0.3;

  // don't let controls go underground
  controls.maxPolarAngle = Math.PI/2 - 0.175;
  // don't allow zoom in/out too far
  controls.minDistance = 5;
  controls.maxDistance = 50;

  var dragControls = new THREE.DragControls( objects, camera, renderer.domElement );
  dragControls.addEventListener( 'dragstart', function () {

    controls.enabled = false;

  } );
  dragControls.addEventListener( 'dragend', function () {

    controls.enabled = true;

  } );

  stats = new Stats();
  container.appendChild( stats.dom );

  //

  window.addEventListener( 'resize', onWindowResize, false );
  renderer.domElement.style.width = window.innerWidth + "px";
  renderer.domElement.style.height = window.innerHeight + "px";
}

function onWindowResize() {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();

  renderer.setSize( window.innerWidth / PIXELATE, window.innerHeight / PIXELATE );

  renderer.domElement.style.width = window.innerWidth + "px";
  renderer.domElement.style.height = window.innerHeight + "px";
}
