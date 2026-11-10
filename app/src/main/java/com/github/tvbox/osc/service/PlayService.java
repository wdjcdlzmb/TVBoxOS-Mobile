package com.github.tvbox.osc.service;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.constant.IntentKey;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.player.MyVideoView;
import com.github.tvbox.osc.ui.activity.DetailActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PlayService extends Service {

    static String videoInfo = "MBox&&第一集";
    private static MyVideoView videoView;

    public static void start(MyVideoView controller,String currentVideoInfo) {
        videoInfo = currentVideoInfo;
        PlayService.videoView = controller;
        ContextCompat.startForegroundService(App.getInstance(), new Intent(App.getInstance(), PlayService.class));
    }

    public static void stop() {
        App.getInstance().stopService(new Intent(App.getInstance(), PlayService.class));
    }


    private static final String CHANNEL_ID = "MBoxPlayChannel";
    private static final int NOTIFICATION_ID = 1;

    private MediaSessionCompat mSession;
    private final Handler mStateHandler = new Handler(Looper.getMainLooper());
    private final Runnable mStateTick = new Runnable() {
        @Override
        public void run() {
            updatePlaybackState();
            mStateHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "后台播放", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        initMediaSession();
    }

    private void initMediaSession() {
        mSession = new MediaSessionCompat(this, "MBoxPlay");
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                if (videoView != null) videoView.start();
                updatePlaybackState();
            }

            @Override
            public void onPause() {
                if (videoView != null) videoView.pause();
                updatePlaybackState();
            }

            @Override
            public void onSkipToNext() {
                sendControlBroadcast(IntentKey.BROADCAST_ACTION_NEXT);
            }

            @Override
            public void onSkipToPrevious() {
                sendControlBroadcast(IntentKey.BROADCAST_ACTION_PREV);
            }

            @Override
            public void onStop() {
                sendControlBroadcast(IntentKey.BROADCAST_ACTION_CLOSE);
            }

            @Override
            public void onSeekTo(long pos) {
                if (videoView != null) videoView.seekTo(pos);
                updatePlaybackState();
            }
        });
        mSession.setActive(true);
        mStateHandler.postDelayed(mStateTick, 1000);
    }

    private void sendControlBroadcast(int actionCode) {
        sendBroadcast(new Intent(IntentKey.BROADCAST_ACTION)
                .putExtra("action", actionCode)
                .setPackage(getPackageName()));
    }

    private void updatePlaybackState() {
        if (mSession == null || videoView == null) return;
        int state = videoView.isPlaying()
                ? PlaybackStateCompat.STATE_PLAYING
                : PlaybackStateCompat.STATE_PAUSED;
        PlaybackStateCompat.Builder builder =
                new PlaybackStateCompat.Builder()
                        .setActions(PlaybackStateCompat.ACTION_PLAY
                                | PlaybackStateCompat.ACTION_PAUSE
                                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                | PlaybackStateCompat.ACTION_STOP
                                | PlaybackStateCompat.ACTION_SEEK_TO);
        builder.setState(state, videoView.getCurrentPosition(), 1.0f);
        mSession.setPlaybackState(builder.build());
    }

    private void updateMetadata() {
        if (mSession == null) return;
        String title = videoInfo.split("&&")[0];
        String episodes = videoInfo.contains("&&") ? videoInfo.split("&&")[1] : "";
        android.support.v4.media.MediaMetadataCompat.Builder builder =
                new android.support.v4.media.MediaMetadataCompat.Builder()
                        .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, title)
                        .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, episodes)
                        .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION,
                                videoView == null ? 0 : videoView.getDuration());
        mSession.setMetadata(builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        videoView.start();
        updateMetadata();
        return START_NOT_STICKY;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event){
        if (event.type == RefreshEvent.TYPE_REFRESH_NOTIFY){
            if (event.obj != null) {
                videoInfo = event.obj.toString();
            }
            updateMetadata();
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private Notification buildNotification(){

        String title = videoInfo.split("&&")[0];
        String episodes = videoInfo.contains("&&") ? videoInfo.split("&&")[1] : "";
        boolean playing = videoView != null && videoView.isPlaying();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(title)
                .setContentText("正在播放: " + episodes)
                .setContentIntent(getPendingIntentActivity())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.ic_notify_pre, "上一集", getPendingIntent(IntentKey.BROADCAST_ACTION_PREV))
                .addAction(playing ? R.drawable.ic_notify_pause : R.drawable.ic_notify_play,
                        playing ? "暂停" : "播放", getPendingIntent(IntentKey.BROADCAST_ACTION_PLAYPAUSE))
                .addAction(R.drawable.ic_notify_next, "下一集", getPendingIntent(IntentKey.BROADCAST_ACTION_NEXT))
                .addAction(R.drawable.ic_notify_close, "关闭", getPendingIntent(IntentKey.BROADCAST_ACTION_CLOSE))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mSession == null ? null : mSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        return builder.build();
    }

    private PendingIntent getPendingIntentActivity() {
        Intent intent = new Intent(this, DetailActivity.class);
        return PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public static PendingIntent getPendingIntent(int actionCode) {
        return PendingIntent.getBroadcast(App.getInstance(), actionCode, new Intent(IntentKey.BROADCAST_ACTION).putExtra("action", actionCode).setPackage(App.getInstance().getPackageName()),PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        mStateHandler.removeCallbacks(mStateTick);
        if (mSession != null) {
            mSession.setActive(false);
            mSession.release();
            mSession = null;
        }
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
