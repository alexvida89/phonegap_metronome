package org.cordova.plugins.metronome;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.util.Base64;

/**
* This class exposes methods in Cordova that can be called from JavaScript.
*/
public class Echo extends CordovaPlugin {

    private Metronome metronome;
    private ToneGenerator.Tone tone;


    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        metronome = new SoundMetronome(cordova.getActivity().getApplicationContext());
        initializeTone();
    }

     /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback context from which we were invoked.
     */
    @SuppressLint("NewApi")
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("setBeatSpeed")) {
            int speed = args.getInt(0);
            String measure = args.getString(1);
            PluginResult result = setBeatSpeed(speed, measure);
            callbackContext.sendPluginResult(result);
        } else if (action.equals("playTone")) {
            PluginResult result = playTone();
            callbackContext.sendPluginResult(result);
        } else if (action.equals("stopTone")) {
            PluginResult result = stopTone();
            callbackContext.sendPluginResult(result);
        } else {
            return false;
        }
        return true;
    }

    private PluginResult setBeatSpeed(int speed, String measure) {
        try {
            metronome.start(speed, measure);
            return new PluginResult(PluginResult.Status.OK);
        } catch (IllegalArgumentException ex) {
            return new PluginResult(PluginResult.Status.ERROR, ex.getMessage());
        }
    }

    private void initializeTone() {
        tone = ToneGenerator.generateTone(440, 1000, true);
    }

    private PluginResult playTone() {
        if (!tone.isPlaying()) {
            initializeTone();
            tone.play();
        }

        return new PluginResult(PluginResult.Status.OK);
    }

    private PluginResult stopTone() {
        if (tone.isPlaying()) {
            tone.release();
        }

        return new PluginResult(PluginResult.Status.OK);
    }
}
