/* global cordova:false */
/* globals window */

var exec = cordova.require('cordova/exec'),
    utils = cordova.require('cordova/utils');

// module.exports = function setBeat(speed, pattern, callback) {
//     exec(callback, callback, 'Echo', 'setBeatSpeed', [speed,pattern]);
//     // exec(callback, callback, 'Echo', 'sound_start ', [speed]);
// };



// var myPlugin = function () {
// }
// myPlugin.setBeat = function(speed, pattern, callback) {
//     exec(callback, callback, 'Echo', 'setBeatSpeed', [speed,pattern]);
// }
// myPlugin.setHaptic = function(callback) {
//     exec(callback, callback, 'Echo', 'setHaptic');
// }
// module.exports = myPlugin;


module.exports.setBeat = function(speed, pattern, callback) { 
    exec(callback, callback, 'Echo', 'setBeatSpeed', [speed,pattern]);
};
module.exports.setHaptic = function(callback) { 
    exec(callback, callback, 'Echo', 'setHaptic');
};