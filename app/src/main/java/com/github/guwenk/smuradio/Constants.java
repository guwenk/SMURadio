package com.github.guwenk.smuradio;

class Constants {
    String getBASS_ErrorFromCode(int code) {
        String output;
        switch (code) {
            case 0:
                output = "all is OK";
                break;
            case 1:
                output = "memory error";
                break;
            case 2:
                output = "can't open the file";
                break;
            case 3:
                output = "can't find a free/valid driver";
                break;
            case 4:
                output = "the sample buffer was lost";
                break;
            case 5:
                output = "invalid handle";
                break;
            case 6:
                output = "unsupported sample format";
                break;
            case 7:
                output = "invalid position";
                break;
            case 8:
                output = "BASS_Init has not been successfully called";
                break;
            case 9:
                output = "BASS_Start has not been successfully called";
                break;
            case 10:
                output = "SSL/HTTPS support isn't available";
                break;
            case 14:
                output = "already initialized/paused/whatever";
                break;
            case 18:
                output = "can't get a free channel";
                break;
            case 19:
                output = "an illegal type was specified";
                break;
            case 20:
                output = "an illegal parameter was specified";
                break;
            case 21:
                output = "no 3D support";
                break;
            case 22:
                output = "no EAX support";
                break;
            case 23:
                output = "illegal device number";
                break;
            case 24:
                output = "not playing";
                break;
            case 25:
                output = "illegal sample rate";
                break;
            case 27:
                output = "the stream is not a file stream";
                break;
            case 29:
                output = "no hardware voices available";
                break;
            case 31:
                output = "the MOD music has no sequence data";
                break;
            case 32:
                output = "no internet connection could be opened";
                break;
            case 33:
                output = "couldn't create the file";
                break;
            case 34:
                output = "effects are not available";
                break;
            case 37:
                output = "requested data is not available";
                break;
            case 38:
                output = "the channel is a \"decoding channel\"";
                break;
            case 39:
                output = "a sufficient DirectX version is not installed";
                break;
            case 40:
                output = "connection timedout";
                break;
            case 41:
                output = "unsupported file format";
                break;
            case 42:
                output = "unavailable speaker";
                break;
            case 43:
                output = "invalid BASS version (used by add-ons)";
                break;
            case 44:
                output = "codec is not available/supported";
                break;
            case 45:
                output = "the channel/file has ended";
                break;
            case 46:
                output = "the device is busy";
                break;
            case -1:
                output = "some other mystery problem";
                break;
            case 500:
                output = "object class problem";
                break;
            default:
                output = "unknown error";
        }
        return output;
    }

    interface ACTION {
        String PLAY_ACTION = "com.github.guwenk.smuradio.action.play";
        String STARTFOREGROUND_ACTION = "com.github.guwenk.smuradio.action.startforeground";
        String STOPFOREGROUND_ACTION = "com.github.guwenk.smuradio.action.stopforeground";
        String UPDATE_ACTIVITY_ACTION = "com.github.guwenk.smuradio.updateActivity";
        String WIDGET_BUTTON_PLAY = "com.github.guwenk.smuradio.action.widget.play";
        String WIDGET_REFRESH_UI = "com.github.guwenk.smuradio.action.widget.refresh";
        String RECONNECT = "com.github.guwenk.smuradio.action.reconnect";
    }

    interface PREFERENCES {
        String LINK = "stream_link";
        String LANGUAGE = "lang";
        String RECONNECT = "reconnect";
        String SET_BACKGROUND = "setBackground";
        String RESTORE_BACKGROUND = "restoreBackground";
        String BACKGROUND_PATH = "backgroundPath";
        String BACKGROUND = "background";
        String BUG_REPORT = "bugReport";
        String INFO = "info";
        String COPY_LINK_TO_CLIPBOARD = "copy_link_to_clipboard";
        String BUFFER_SIZE = "buffer_size";
        String HEADSET_BUTTON = "double_click";
        String SYSTEM_LANGUAGE = "system_lang";
        String RESTORE_SETTINGS = "restore_settings";
    }

    interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 101;
    }

    interface OTHER {
        String USER_RATE = "rate_value";
        String ORDER_FREEZE = "order_freeze";
        String SONG_TITLE_INTENT = "song_title";
    }

    interface FIREBASE {
        String RATE_VAL = "rate";
        String RATE_COUNT = "count";
        String REQUESTS = "Requests";
        String ORDER = "order";
        String SERVER_STATUS = "ServerStatus";
    }
}
