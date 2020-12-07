/* global cordova:false */
/* global module:false */

var platform = cordova.require('cordova/platform');

exports.setBeat = function setBeat(speed, pattern, callback) {
    console.log('metronome.setBeat', speed, pattern);
    cordova.exec(callback, callback, 'Echo', 'setBeatSpeed', [speed, pattern]);
};

exports.playTone = function playTone(callback) {
    if (platform.id === 'android') {
        cordova.exec(callback, callback, 'Echo', 'playTone', []);
    } // not available ios yet
};

exports.stopTone = function stopTone(callback) {
    if (platform.id === 'android') {
        cordova.exec(callback, callback, 'Echo', 'stopTone', []);
    } // not available ios yet
};
