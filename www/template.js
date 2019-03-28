/* global cordova:false */
/* globals window */

var exec = cordova.require('cordova/exec'),
    utils = cordova.require('cordova/utils');

module.exports = function setBeat(speed, pattern, callback) {
    exec(callback, callback, 'Echo', 'setBeatSpeed', [speed,pattern]);
    // exec(callback, callback, 'Echo', 'sound_start ', [speed]);
};
module.exports = function setHaptic(callback) {
    exec(callback, callback, 'Echo', 'setHaptic');
};
