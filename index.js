const System = Java.type('java.lang.System');
load('./node_modules/jvm-npm/src/main/javascript/jvm-npm.js');
const path = Java.type('com.github.tncrazvan.svelte3ssr.PathJS');
const fs = Java.type('com.github.tncrazvan.svelte3ssr.FileSystemJS');
	
const register = require('./node_modules/require-extension/index.js');
require('svelte/register');
const App = require('./ww/src/App.svelte').default;
const { head, html, css } = App.render({
	answer: 42
});