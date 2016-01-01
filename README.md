**Push Bullet Notifications for SageTV**

This SageTV Plugin enables SageTV System Messages to be pushed to a desktop or mobile device running the PushBullet Application.

A PushBullet account is required, and, you will need to configure your Access Token as the API KEY in the Plugin Configuration.

The easiest way to do this is to simply edit the Sage.properties (when the server is not running) and add the following key

```
sagex/plugin/pushbullet/apikey=ACCESS_TOKEN_FROM_ACCOUNT_TAB
```

If you do this **BEFORE** you install the plugin, then PushBullet will be configured.  You can then go into the Plugin Configuration options and turn on/off the messages that you want to receive.  Also, you can send a test message to verify that it is working.


You can sign-up for a PushBullet account here
https://www.pushbullet.com/

