/* global cordova:false */
/* globals window */

var exec = cordova.require('cordova/exec'),
    utils = cordova.require('cordova/utils');

module.exports = function setBeat(speed, callback) {
    var pattern = "HNNHNNH";
    exec(callback, callback, 'Echo', 'setBeatSpeed', [speed,pattern]);
    // exec(callback, callback, 'Echo', 'sound_start ', [speed]);
};