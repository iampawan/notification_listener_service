package notification.listener.service;

import static notification.listener.service.NotificationUtils.isPermissionGranted;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import notification.listener.service.models.Action;
import notification.listener.service.models.ActionCache;
import android.annotation.SuppressLint;
import android.os.Build;

import java.util.concurrent.TimeUnit;


public class NotificationListenerServicePlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener, EventChannel.StreamHandler {

    private static final String CHANNEL_TAG = "x-slayer/notifications_channel";
    private static final String EVENT_TAG = "x-slayer/notifications_event";

    private MethodChannel channel;
    private EventChannel eventChannel;
    private NotificationReceiver notificationReceiver;
    private Context context;
    private Activity mActivity;

    private Result pendingResult;
    final int REQUEST_CODE_FOR_NOTIFICATIONS = 1199;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL_TAG);
        channel.setMethodCallHandler(this);
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENT_TAG);
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        pendingResult = result;
        if (call.method.equals("isPermissionGranted")) {
            result.success(isPermissionGranted(context));
        } else if (call.method.equals("requestPermission")) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            mActivity.startActivityForResult(intent, REQUEST_CODE_FOR_NOTIFICATIONS);
        } else if (call.method.equals("sendReply")) {
            final String message = call.argument("message");
            final int notificationId = call.argument("notificationId");

            final Action action = ActionCache.cachedNotifications.get(notificationId);
            if (action == null) {
                result.error("Notification", "Can't find this cached notification", null);
            }
            try {
                action.sendReply(context, message);
                result.success(true);
            } catch (PendingIntent.CanceledException e) {
                result.success(false);
                e.printStackTrace();
            }
        } else if (call.method.equals("scheduleReply")) {
            int notificationId = call.argument("notificationId");
            String notificationKey = call.argument("notificationKey");
            String message = call.argument("message");
            int delaySeconds = call.argument("delay");

            scheduleReply(context, notificationId,notificationKey, message, delaySeconds);
            result.success(true);
        }
        else {
            result.notImplemented();
        }
    }

    private void scheduleReply(Context context, int notificationId, String notificationKey, String message, int delaySeconds) {
        WorkManager workManager = WorkManager.getInstance(context);

        Data inputData = new Data.Builder()
                .putInt("notificationId", notificationId)
                .putString("notificationKey", notificationKey)
                .putString("message", message)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DelayedReplyWorker.class)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setInputData(inputData)
                .build();

        workManager.enqueue(workRequest);
        Log.i("NotificationListener", "Scheduled delayed reply: " + message + " in " + delaySeconds + " seconds");
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.mActivity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        this.mActivity = null;
    }
    @SuppressLint("WrongConstant")
    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NotificationConstants.INTENT);
        notificationReceiver = new NotificationReceiver(events);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.registerReceiver(notificationReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        }else{
            context.registerReceiver(notificationReceiver, intentFilter);
        }
        Intent listenerIntent = new Intent(context, NotificationReceiver.class);
        context.startService(listenerIntent);
        Log.i("NotificationPlugin", "Started the notifications tracking service.");
    }

    @Override
    public void onCancel(Object arguments) {
        context.unregisterReceiver(notificationReceiver);
        notificationReceiver = null;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FOR_NOTIFICATIONS) {
            if (resultCode == Activity.RESULT_OK) {
                pendingResult.success(true);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                pendingResult.success(isPermissionGranted(context));
            } else {
                pendingResult.success(false);
            }
            return true;
        }
        return false;
    }
}
