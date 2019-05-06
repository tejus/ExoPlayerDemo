package com.tejus.exoplayerdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageButton;

import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;
import com.tejus.exoplayerdemo.videoplayer.PlayerInstance;

public class FullscreenActivity extends AppCompatActivity {

    private static final String LOG_TAG = FullscreenActivity.class.getSimpleName();
    private static final String CURRENT_POSITION_KEY = "current_position";
    private static final String CURRENT_WINDOW_KEY = "current_window";
    private static final String PLAY_WHEN_READY_KEY = "play_when_ready";
    private static final String VIDEO_URL_KEY = "video_url";


    PlayerView mPlayerView;
    private ImageButton mFullscreenToggle;
    private boolean mIsPlayerInitialised;
    private PlayerInstance mPlayerInstance;
    private String mVideoUri;
    private long mCurrentPosition;
    private int mCurrentWindowIndex;
    private boolean mPlayWhenReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        Log.d(LOG_TAG, "onCreate()");

        mPlayerView = findViewById(R.id.player_view_fullscreen);
        mFullscreenToggle = findViewById(R.id.exo_fullscreen);
        mFullscreenToggle.setImageDrawable(getDrawable(R.drawable.baseline_fullscreen_exit_white_24));

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mVideoUri = bundle.getString(VIDEO_URL_KEY);
            mCurrentPosition = bundle.getLong(CURRENT_POSITION_KEY);
            mCurrentWindowIndex = bundle.getInt(CURRENT_WINDOW_KEY);
            mPlayWhenReady = bundle.getBoolean(PLAY_WHEN_READY_KEY);
            mIsPlayerInitialised = true;
        } else {
            Log.e(LOG_TAG, "Activity launched without video data!");
            finish();
        }

        mFullscreenToggle.setOnClickListener((v -> {
            Log.d(LOG_TAG, "Fullscreen toggle clicked");
            sendResultBack();
        }));

        mPlayerInstance = new PlayerInstance(this, mPlayerView, bundle);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart()");
        if (Util.SDK_INT >= 24 && mIsPlayerInitialised) {
            Log.d(LOG_TAG, "Play video now");
            playVideo();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume()");
        if (Util.SDK_INT < 24 && mIsPlayerInitialised) {
            Log.d(LOG_TAG, "Play video now");
            playVideo();
        }
    }

    private void playVideo() {
        mPlayerInstance.play(mVideoUri);
        mIsPlayerInitialised = true;
    }

    private void sendResultBack() {
        Bundle resultBundle = mPlayerInstance.stop();
        Intent intent = new Intent();
        intent.putExtras(resultBundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        sendResultBack();
    }

    private void stopPlayer() {
        Bundle bundle = mPlayerInstance.stop();
        mCurrentPosition = bundle.getLong(CURRENT_POSITION_KEY);
        mCurrentWindowIndex = bundle.getInt(CURRENT_WINDOW_KEY);
        mPlayWhenReady = bundle.getBoolean(PLAY_WHEN_READY_KEY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause()");
        if (Util.SDK_INT < 24) {
            stopPlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop()");
        if (Util.SDK_INT >= 24) {
            stopPlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");
        mPlayerInstance.close();
    }
}
