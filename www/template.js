/* global cordova:false */
/* globals window */

console.log("WORKS");
var exec = cordova.require('cordova/exec'),
    utils = cordova.require('cordova/utils');

module.exports = function setBeat(speed, callback) {
    exec(callback, callback, 'Echo', 'setBeatSpeed', [speed]);
    // exec(callback, callback, 'Echo', 'sound_start ', [speed]);
};