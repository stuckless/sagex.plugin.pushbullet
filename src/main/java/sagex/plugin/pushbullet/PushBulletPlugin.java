package sagex.plugin.pushbullet;

import sage.MediaFile;
import sage.SageTVPlugin;
import sage.SageTVPluginRegistry;
import sage.api.MediaFileAPI;
import sage.msg.SystemMessage;
import sage.plugin.PluginEventManager;
import sagex.plugin.*;
import sagex.remote.json.JSONException;
import sagex.remote.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static sage.msg.SystemMessage.*;

/**
 * Created by seans on 18/12/15.
 */
public class PushBulletPlugin extends AbstractPlugin {
    public static final String PUSHBULLET_URL = "https://api.pushbullet.com/v2/pushes";
    public static final String PROP_BASE = "sagex/plugin/pushbullet/";
    public static final String PROP_API_KEY = PROP_BASE + "apikey";
    public static final String PROP_ENABLE_ALL = PROP_BASE + "enableAll";
    public static final String PROP_TEST_SEND = PROP_BASE + "testSend";
    public static final String PROP_REPEAT_MAX = PROP_BASE + "maxRepeatCount";

    public PushBulletPlugin(SageTVPluginRegistry registry) {
        super(registry);
    }

    @Override
    public void start() {
        super.start();

        addProperty(SageTVPlugin.CONFIG_TEXT, PROP_API_KEY, "", "API KEY", "PushBullet API Key");

        addProperty(SageTVPlugin.CONFIG_INTEGER, PROP_REPEAT_MAX, "10", "Max Message Repeats", "Maximum # of message repeats that will be sent");

        addProperty(SageTVPlugin.CONFIG_BOOL, PROP_ENABLE_ALL, "false", "Enable All System Events", "If enabled, then ALL System Events will be pushed");

        int messages[] = new int[] {
        LINEUP_LOST_FROM_SERVER_MSG,
        NEW_CHANNEL_ON_LINEUP_MSG,
        CHANNEL_SCAN_NEEDED_MSG,
        EPG_UPDATE_FAILURE_MSG,
        EPG_LINKAGE_FOR_MR_CHANGED_MSG,

        MISSED_RECORDING_FROM_CONFLICT_MSG,
        CAPTURE_DEVICE_LOAD_ERROR_MSG,
        PARTIAL_RECORDING_FROM_CONFLICT_MSG,

        ENCODER_HALT_MSG,
        CAPTURE_DEVICE_RECORD_ERROR_MSG,
        MISSED_RECORDING_FROM_CAPTURE_FAILURE_MSG,
        DISKSPACE_INADEQUATE_MSG,
        CAPTURE_DEVICE_DATASCAN_ERROR_MSG,
        VIDEO_DIRECTORY_OFFLINE_MSG,
        PLAYLIST_MISSING_SEGMENT,

        SYSTEM_LOCKUP_DETECTION_MSG,
        OUT_OF_MEMORY_MSG,
        SOFTWARE_UPDATE_AVAILABLE_MSG,
        STORAGE_MONITOR_MSG,
        GENERAL_MSG,
        PLUGIN_INSTALL_MISSING_FILE_MSG
        };

        for (int message: messages) {
            addProperty(SageTVPlugin.CONFIG_BOOL, PROP_BASE + String.valueOf(message), "false", SystemMessage.getNameForMsgType(message), SystemMessage.getNameForMsgType(message))
                    .setVisibility(new IPropertyVisibility() {
                        @Override
                        public boolean isVisible() {
                            return !getConfigBoolValue(PROP_ENABLE_ALL);
                        }
                    });
        }

        addProperty(SageTVPlugin.CONFIG_BUTTON, PROP_TEST_SEND, "TEST", "Send Test Notification", "Send Test Notification");
    }

    @Override
    public void stop() {
        super.stop();
    }

    @ButtonClickHandler(PROP_TEST_SEND)
    public void onTestSend() {
        log.debug("Sending Test Notification");
        try {
            Properties props = new Properties();
            props.setProperty("testa", "Field A Value");
            props.setProperty("testb", "Field B Value");
            SystemMessage systemMessage = new SystemMessage(Integer.MAX_VALUE, SystemMessage.ERROR_PRIORITY, "Test Message", props);
            Map event = new HashMap();
            event.put(PluginEventManager.VAR_SYSTEMMESSAGE, systemMessage);
            event.put(SageEvents.SystemMessagePosted, event);

            // call the plugin with the event
            onSystemMessage(event);
        } catch (Throwable t) {
            log.error("Test PushBullet Send Failed", t);
        }
    }

    @SageEvent(value= SageEvents.SystemMessagePosted, background = true)
    public void onSystemMessage(Map vars) {
        //Authorization: Bearer $API
        //https://api.pushbullet.com/v2/pushes
        // Content-Type: application/json
        // {\"type\": \"note\", \"title\": \"Rename Failed!!!\", \"body\": \"$FILE was not renamed because $MSG \"}
        try {
            SystemMessage msg = (SystemMessage) vars.get(PluginEventManager.VAR_SYSTEMMESSAGE);
            if (!canSendNotification(msg)) {
                return;
            }

            JSONObject pb = new JSONObject();
            pb.put("type","note");
            pb.put("title", msg.getMessageText());
            pb.put("body", formatMessageBody(msg));

            sendMessage(pb);
        } catch (JSONException e) {
            log.error("JSON Error", e);
        } catch (IOException e) {
            log.error("Failed to use PushBullet", e);
        } catch (Throwable t) {
            log.error("Erorr", t);
        }
    }

    @SageEvent(value = SageEvents.RecordingCompleted, background = true)
    public void onRecordingComplete(Map vars) {
        Object mf = vars.get(PluginEventManager.VAR_MEDIAFILE);

    }

    void sendMessage(JSONObject pb) throws IOException {
        RequestBuilder builder = new RequestBuilder(PUSHBULLET_URL);
        builder.setContentType("application/json");
        builder.addHeader("Authorization", "Bearer " + getApiKey());
        builder.setBody(pb.toString());
        builder.postRequest();
    }

    boolean canSendNotification(SystemMessage msg) {
        if (msg==null) return false;
        if (msg.getType() == Integer.MAX_VALUE) return true; // test message

        // check if the message is enabled
        if (getConfigBoolValue(PROP_ENABLE_ALL) || getConfigBoolValue(PROP_BASE + String.valueOf(msg.getType()))) {
            // check the repeat count
            return msg.getRepeatCount() <= getConfigIntValue(PROP_REPEAT_MAX);
        }
        return false;
    }

    String getApiKey() {
        return getConfigValue(PROP_API_KEY);
    }

    String formatMessageBody(SystemMessage msg) {
        if (msg==null) return "";
        StringBuilder sb = new StringBuilder();
        if (msg.getRepeatCount()>1) {
            if (msg.getRepeatCount() == getConfigIntValue(PROP_REPEAT_MAX)) {
                sb.append("Repeats " + msg.getRepeatCount() + ".  There may be more repeats, but they will be ignored.\n");
            } else {
                sb.append("Repeat Count " + msg.getRepeatCount() + "\n");
            }
        }
        if (msg.getMessageVarNames()!=null) {
            for (String s: msg.getMessageVarNames()) {
                sb.append(s).append(": ").append(msg.getMessageVarValue(s)).append("\n");
            }
        }
        return sb.toString();
    }
}
