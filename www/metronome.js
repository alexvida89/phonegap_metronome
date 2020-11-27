/* global cordova:false */
/* global module:false */

module.exports = function setBeat(speed, pattern, callback) {
    console.log('metronome.setBeat', speed, pattern);
    cordova.exec(callback, callback, 'Echo', 'setBeatSpeed', [speed, pattern]);
};
