/* global cordova:false */
/* globals window */

var MetronomePlugin = {};

MetronomePlugin.prototype.setBeat = function setBeat(speed, pattern, callback) {
    cordova.exec(callback, callback, 'Echo', 'setBeatSpeed', [speed, pattern]);
};

module.exports = MetronomePlugin;
