package notification.listener.service;

import static notification.listener.service.NotificationConstants.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;

import androidx.annotation.RequiresApi;

import io.flutter.plugin.common.EventChannel.EventSink;

import java.util.HashMap;

public class NotificationReceiver extends BroadcastReceiver {

    private EventSink eventSink;

    public NotificationReceiver(EventSink eventSink) {
        this.eventSink = eventSink;
    }

    @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = intent.getStringExtra(PACKAGE_NAME);
        String title = intent.getStringExtra(NOTIFICATION_TITLE);
        String content = intent.getStringExtra(NOTIFICATION_CONTENT);
        byte[] notificationIcon = intent.getByteArrayExtra(NOTIFICATIONS_ICON);
        byte[] notificationExtrasPicture = intent.getByteArrayExtra(EXTRAS_PICTURE);
        byte[] largeIcon = intent.getByteArrayExtra(NOTIFICATIONS_LARGE_ICON);
        boolean haveExtraPicture = intent.getBooleanExtra(HAVE_EXTRA_PICTURE, false);
        boolean hasRemoved = intent.getBooleanExtra(IS_REMOVED, false);
        boolean canReply = intent.getBooleanExtra(CAN_REPLY, false);
        int id = intent.getIntExtra(ID, -1);


        HashMap<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("packageName", packageName);
        data.put("title", title);
        data.put("content", content);
        data.put("notificationIcon", notificationIcon);
        data.put("notificationExtrasPicture", notificationExtrasPicture);
        data.put("haveExtraPicture", haveExtraPicture);
        data.put("largeIcon", largeIcon);
        data.put("hasRemoved", hasRemoved);
        data.put("canReply", canReply);
        data.put("notificationTag", intent.getStringExtra(NotificationConstants.NOTIFICATION_TAG));
        data.put("postTime", intent.getLongExtra(NotificationConstants.POST_TIME, 0));
        data.put("isOngoing", intent.getBooleanExtra(NotificationConstants.IS_ONGOING, false));
        data.put("isClearable", intent.getBooleanExtra(NotificationConstants.IS_CLEARABLE, false));
        data.put("userId", intent.getStringExtra(NotificationConstants.USER_ID));
        data.put("notificationKey", intent.getStringExtra(NotificationConstants.NOTIFICATION_KEY));
        data.put("groupKey", intent.getStringExtra(NotificationConstants.GROUP_KEY));
        data.put("isGroup", intent.getBooleanExtra(NotificationConstants.IS_GROUP,false));
        data.put("isAppGroup", intent.getBooleanExtra(NotificationConstants.IS_APP_GROUP,false));
        data.put("user", intent.getStringExtra(NotificationConstants.USER));

        eventSink.success(data);
    }
}
