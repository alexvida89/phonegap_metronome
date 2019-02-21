/* global cordova:false */
/* globals window */

var exec = cordova.require('cordova/exec'),
    utils = cordova.require('cordova/utils');

module.exports = function setBeat(speed, pattern, callback) {
    exec(callback, callback, 'Echo', 'setBeatSpeed', [speed,pattern]);
    // exec(callback, callback, 'Echo', 'sound_start ', [speed]);
};
module.exports = function stop(speed,pattern,callback) {
    exec(callback, callback, 'Echo', 'stopBeatSpeed', [0,0]);
    // exec(callback, callback, 'Echo', 'sound_start ', [speed]);
};