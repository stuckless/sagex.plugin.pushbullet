package sagex.plugin.pushbullet;

import org.junit.BeforeClass;
import org.junit.Test;
import sage.SageTVPluginRegistry;
import sage.TestUtils;
import sage.msg.SystemMessage;
import sage.plugin.PluginEventManager;
import sagex.plugin.SageEvents;
import sagex.remote.json.JSONObject;
import sagex.util.LogProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by seans on 18/12/15.
 */
public class PushBulletPluginTest {
    @BeforeClass
    public static void init() throws Throwable {
        TestUtils.initializeSageTVForTesting();
    }

    @Test
    public void testOnSystemMessage() throws Exception {
        LogProvider.useSystemOut();
        LogProvider.getLogger(PushBulletPlugin.class).debug("Logging Initialized");

        // setup the event message
        PushBulletPlugin plugin = spy(new PushBulletPlugin(mock(SageTVPluginRegistry.class)));
        // just in case we are using a rest test
        // doReturn(getApiKey()).when(plugin).getApiKey();
        // normally we just prevent the pushbullet call
        doNothing().when(plugin).sendMessage(any(JSONObject.class));

        // use the test send command
        plugin.onTestSend();

        // verify that the message was sent
        verify(plugin, times(1)).sendMessage(any(JSONObject.class));
    }

    @Test
    public void testMessageBody() {
        SystemMessage msg = spy(new SystemMessage(1, 1, "Test Message", new Properties()));
        doReturn(10).when(msg).getRepeatCount();

        PushBulletPlugin plugin = spy(new PushBulletPlugin(mock(SageTVPluginRegistry.class)));
        doReturn(10).when(plugin).getConfigIntValue(eq(PushBulletPlugin.PROP_REPEAT_MAX));
        String val = plugin.formatMessageBody(msg);
        assertTrue(val.contains("Repeats 10"));
        System.out.println(val);

        doReturn(9).when(msg).getRepeatCount();
        val = plugin.formatMessageBody(msg);
        assertEquals("Repeat Count 9", val.trim());
        System.out.println(val);
    }
}