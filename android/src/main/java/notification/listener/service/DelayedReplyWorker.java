package notification.listener.service;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import notification.listener.service.models.Action;
import notification.listener.service.models.ActionCache;

public class DelayedReplyWorker extends Worker {

    public DelayedReplyWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int notificationId = getInputData().getInt("notificationId", -1);
        String notificationKey = getInputData().getString("notificationKey");
        String message = getInputData().getString("message");

        if (notificationId == -1 || notificationKey == null || message == null) {
            return Result.failure();
        }

        Action action = ActionCache.cachedNotifications.get(notificationId);
        if (action == null) {
            Log.e("DelayedReply", "Failed to find cached notification action.");
            return Result.failure();
        }

        try {
            action.sendReply(getApplicationContext(), message);
            Log.i("DelayedReply", "Successfully sent delayed reply: " + message);
            return Result.success();
        } catch (Exception e) {
            Log.e("DelayedReply", "Failed to send reply: " + e.getMessage());
            return Result.failure();
        }
    }
}