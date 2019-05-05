package com.tejus.exoplayerdemo;

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
import android.support.v7.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String CURRENT_POSITION_KEY = "current_position";
    private static final String CURRENT_WINDOW_KEY = "current_window";
    private static final String PLAY_WHEN_READY_KEY = "play_when_ready";
    private static final String NOTIFICATION_CHANNEL_ID = "Video";

    private PlayerView mPlayerView;
    private SimpleExoPlayer mPlayer;
    private long mCurrentPosition;
    private int mCurrentWindowIndex;
    private boolean mPlayWhenReady;
    private static MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    NotificationManager mNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayerView = findViewById(R.id.player_view);

        if (savedInstanceState != null) {
            mCurrentPosition = savedInstanceState.getLong(CURRENT_POSITION_KEY);
            mCurrentWindowIndex = savedInstanceState.getInt(CURRENT_WINDOW_KEY);
            mPlayWhenReady = savedInstanceState.getBoolean(PLAY_WHEN_READY_KEY);
        } else {
            mCurrentPosition = C.TIME_UNSET;
            mCurrentWindowIndex = C.INDEX_UNSET;
            mPlayWhenReady = false;
        }

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        initialiseMediaSession();
    }

    @Override
    protected void onStart() {
        super.onStart();
        /* Marshmallow supports multi-window, so initialise the player in onStart instead of onResume
         * Also release the player in the respective lifecycle end calls.
         */
        if (Util.SDK_INT >= 24) {
            initialisePlayer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Util.SDK_INT < 24) {
            initialisePlayer();
        }
    }

    private void initialiseMediaSession() {
        mMediaSession = new MediaSessionCompat(this, LOG_TAG);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setMediaButtonReceiver(null);
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_FAST_FORWARD |
                        PlaybackStateCompat.ACTION_REWIND |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        mMediaSession.setPlaybackState(mStateBuilder.build());
        mMediaSession.setCallback(new MediaSessionCallback());
        mMediaSession.setActive(true);
    }

    private void initialisePlayer() {
        String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");

        mPlayer = ExoPlayerFactory.newSimpleInstance(this);
        mPlayerView.setPlayer(mPlayer);
        mPlayer.addListener(new ExoEventCallback());

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_SPEECH)
                .build();
        mPlayer.setAudioAttributes(audioAttributes, true);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, userAgent);
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse("asset:///-intro-creampie.mp4"));

        DefaultHttpDataSourceFactory httpSourceFactory = new DefaultHttpDataSourceFactory(userAgent);
        ExtractorMediaSource extractorMediaSource = new ExtractorMediaSource.Factory(httpSourceFactory)
                .createMediaSource(Uri.parse("https://d17h27t6h515a5.cloudfront.net/topher/2017/April/58ffd974_-intro-creampie/-intro-creampie.mp4"));

        if (mCurrentPosition != C.TIME_UNSET) {
            mPlayer.seekTo(mCurrentWindowIndex, mCurrentPosition);
            mPlayer.prepare(mediaSource, false, false);
        } else {
            mPlayer.prepare(mediaSource);
        }

        mPlayer.setPlayWhenReady(mPlayWhenReady);
    }

    private void releasePlayer() {
        mNotificationManager.cancelAll();
        mCurrentPosition = mPlayer.getCurrentPosition();
        mCurrentWindowIndex = mPlayer.getCurrentWindowIndex();
        mPlayWhenReady = mPlayer.getPlayWhenReady();
        mPlayer.release();
        mPlayer = null;
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            mPlayWhenReady = true;
            mPlayer.setPlayWhenReady(mPlayWhenReady);
        }

        @Override
        public void onPause() {
            mPlayWhenReady = false;
            mPlayer.setPlayWhenReady(mPlayWhenReady);
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
            mPlayer.setPlayWhenReady(mPlayWhenReady);
            mPlayer.seekTo(0);
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

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
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE)
        );

        NotificationCompat.Action restartAction = new NotificationCompat.Action(
                R.drawable.exo_controls_previous, "Restart",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        builder.setContentTitle("Title goes here")
                .setContentText("Text goes here")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.exo_icon_play)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(restartAction)
                .addAction(playPauseAction)
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

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) {
            releasePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            releasePlayer();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CURRENT_POSITION_KEY, mCurrentPosition);
        outState.putInt(CURRENT_WINDOW_KEY, mCurrentWindowIndex);
        outState.putBoolean(PLAY_WHEN_READY_KEY, mPlayWhenReady);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaSession.setActive(false);
    }
}
