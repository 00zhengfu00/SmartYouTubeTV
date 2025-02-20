/**
 * Description: Imitate press on OK button after seek
 */

console.log("Scripts::Running script codec_fix.js");

function CodecFixAddon() {
    this.TAG = 'CodecFixAddon';
    this.run = function() {
        var config = this.findProperConfig();
        this.applyCodecFixes(config);
    };

    /**
     * Obtains device map for WebView or XWalk respectively
     */
    this.findProperConfig = function() {
        // CodecSelectorAddon usage
        switch (DeviceUtils.getPreferredCodec()) {
            case DeviceUtils.AVC:
                Log.d(this.TAG, "Forcing AVC codec...");
                return CodecConfig_AVC;
            case DeviceUtils.VP9:
                Log.d(this.TAG, "Forcing VP9 codec...");
                return CodecConfig_VP9;
        }

        if (DeviceUtils.isWebView()) {
            return CodecConfig_WebView;
        }

        if (DeviceUtils.isXWalk()) {
            return CodecConfig_XWalk;
        }

        Log.d(this.TAG, "CodecFixAddon::unknown engine type " + DeviceUtils.getEngineType());
        return {};
    };

    /**
     * Consumes hash map, that consists of {'device name': 'disabled codec list'}
     * Device name could be obtained from the CPU-Z for example.
     * For details see: WebViewJavaScriptInterface.java
     */
    this.applyCodecFixes = function(deviceMap) {
        for (var device in deviceMap) {
            if (DeviceUtils.isMyDevice(device)) {
                // multiple codecs supported, divided by comma
                var codecs = deviceMap[device].split(',');
                var codecsLen = codecs.length;
                for (var i = 0; i < codecsLen; i++) {
                    var codec = codecs[i].trim();
                    DeviceUtils.disableCodec(codec, true);
                }
                break;
            }
        }
    };
}

new CodecFixAddon().run();
