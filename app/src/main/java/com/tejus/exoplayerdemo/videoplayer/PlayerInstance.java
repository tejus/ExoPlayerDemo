package com.tejus.exoplayerdemo.videoplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.tejus.exoplayerdemo.MainActivity;
import com.tejus.exoplayerdemo.R;

public class PlayerInstance {

    private static final String LOG_TAG = PlayerInstance.class.getSimpleName();

    private static final String CURRENT_POSITION_KEY = "current_position";
    private static final String CURRENT_WINDOW_KEY = "current_window";
    private static final String PLAY_WHEN_READY_KEY = "play_when_ready";
    private static final String NOTIFICATION_CHANNEL_ID = "Video";

    private Context mContext;
    private PlayerView mPlayerView;
    private SimpleExoPlayer mPlayer;
    private String mVideoUri;
    private long mCurrentPosition;
    private int mCurrentWindowIndex;
    private boolean mPlayWhenReady;
    private static MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    private NotificationManager mNotificationManager;

    public PlayerInstance(Context context, PlayerView playerView, Bundle bundle) {
        if (playerView != null) {
            mContext = context;
            mPlayerView = playerView;
            mCurrentPosition = C.TIME_UNSET;
            mCurrentWindowIndex = C.INDEX_UNSET;
            mPlayWhenReady = false;
            mNotificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (bundle != null) {
                mCurrentPosition = bundle.getLong(CURRENT_POSITION_KEY);
                mCurrentWindowIndex = bundle.getInt(CURRENT_WINDOW_KEY);
                mPlayWhenReady = bundle.getBoolean(PLAY_WHEN_READY_KEY);
            } else {
                mCurrentPosition = C.TIME_UNSET;
                mCurrentWindowIndex = C.INDEX_UNSET;
                mPlayWhenReady = false;
            }
            initialiseMediaSession();
        } else {
            throw new IllegalArgumentException(context.toString()
                    + " must pass a valid PlayerView!");
        }
    }

    private void initialiseMediaSession() {
        Log.d(LOG_TAG, "initialiseMediaSession()");
        mMediaSession = new MediaSessionCompat(mContext, LOG_TAG + mContext);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setMediaButtonReceiver(null);
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_FAST_FORWARD |
                        PlaybackStateCompat.ACTION_REWIND |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_STOP);
        mMediaSession.setPlaybackState(mStateBuilder.build());
        mMediaSession.setCallback(new MediaSessionCallback());
        mMediaSession.setActive(true);
    }

    private void initialisePlayer() {
        Log.d(LOG_TAG, "initialisePlayer()");
        String userAgent = Util.getUserAgent(mContext, "ExoPlayerDemo");

        mPlayer = ExoPlayerFactory.newSimpleInstance(mContext);
        mPlayerView.setPlayer(mPlayer);
        mPlayer.addListener(new ExoEventCallback());

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_SPEECH)
                .build();
        mPlayer.setAudioAttributes(audioAttributes, true);

        DefaultHttpDataSourceFactory httpSourceFactory = new DefaultHttpDataSourceFactory(userAgent);
        ExtractorMediaSource extractorMediaSource = new ExtractorMediaSource.Factory(httpSourceFactory)
                .createMediaSource(Uri.parse(mVideoUri));

        if (mCurrentPosition != C.TIME_UNSET && mCurrentWindowIndex != C.INDEX_UNSET) {
            mPlayer.seekTo(mCurrentWindowIndex, mCurrentPosition);
            mPlayer.prepare(extractorMediaSource, false, false);
            mPlayer.setPlayWhenReady(mPlayWhenReady);
        } else {
            mPlayer.prepare(extractorMediaSource);
            mPlayWhenReady = true;
            mPlayer.setPlayWhenReady(true);
        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            mPlayWhenReady = true;
            mPlayer.setPlayWhenReady(true);
        }

        @Override
        public void onPause() {
            mPlayWhenReady = false;
            mPlayer.setPlayWhenReady(false);
        }

        @Override
        public void onFastForward() {
            mPlayer.seekTo(mPlayer.getCurrentPosition() + 5000);
        }

        @Override
        public void onRewind() {
            mPlayer.seekTo(mPlayer.getCurrentPosition() - 5000);
        }

        @Override
        public void onSkipToPrevious() {
            mPlayWhenReady = false;
            mPlayer.setPlayWhenReady(false);
            mPlayer.seekTo(0);
        }

        @Override
        public void onStop() {
            this.onSkipToPrevious();
        }
    }

    private class ExoEventCallback implements Player.EventListener {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == Player.STATE_READY && playWhenReady) {
                mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                        mPlayer.getCurrentPosition(), 1f);
            } else if (playbackState == Player.STATE_READY) {
                mStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                        mPlayer.getCurrentPosition(), 1f);
            }
            mMediaSession.setPlaybackState(mStateBuilder.build());
            showNotification(mStateBuilder.build());
        }
    }

    private void showNotification(PlaybackStateCompat playbackState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID);

        int icon;
        String playPause;
        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            icon = R.drawable.exo_controls_pause;
            playPause = "Pause";
        } else {
            icon = R.drawable.exo_controls_play;
            playPause = "Play";
        }

        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
                icon, playPause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(mContext,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE)
        );

        NotificationCompat.Action restartAction = new NotificationCompat.Action(
                R.drawable.exo_controls_previous, "Restart",
                MediaButtonReceiver.buildMediaButtonPendingIntent(mContext,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, MainActivity.class), 0);

        builder.setContentTitle("Title goes here")
                .setContentText("Text goes here")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.exo_icon_play)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(restartAction)
                .addAction(playPauseAction)
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(mContext,
                        PlaybackStateCompat.ACTION_STOP))
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1));

        mNotificationManager.notify(0, builder.build());
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Video", NotificationManager.IMPORTANCE_LOW);
        mChannel.setDescription("Video playback");
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);
    }

    public static class MediaReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            MediaButtonReceiver.handleIntent(mMediaSession, intent);
        }
    }

    private void releasePlayer() {
        mNotificationManager.cancelAll();
        mCurrentPosition = mPlayer.getCurrentPosition();
        mCurrentWindowIndex = mPlayer.getCurrentWindowIndex();
        mPlayWhenReady = mPlayer.getPlayWhenReady();
        mPlayer.release();
        mPlayer = null;
    }

    public void play(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return;
        }
        if (mPlayer != null) {
            releasePlayer();
        }
        mVideoUri = uri;
        initialisePlayer();
    }

    public Bundle stop() {
        releasePlayer();
        Bundle bundle = new Bundle();
        bundle.putLong(CURRENT_POSITION_KEY, mCurrentPosition);
        bundle.putInt(CURRENT_WINDOW_KEY, mCurrentWindowIndex);
        bundle.putBoolean(PLAY_WHEN_READY_KEY, mPlayWhenReady);
        return bundle;
    }

    public void close() {
        mMediaSession.setActive(false);
    }
}
