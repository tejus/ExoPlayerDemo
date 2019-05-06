package com.tejus.exoplayerdemo;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;
import com.tejus.exoplayerdemo.videoplayer.PlayerInstance;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String CURRENT_POSITION_KEY = "current_position";
    private static final String CURRENT_WINDOW_KEY = "current_window";
    private static final String PLAY_WHEN_READY_KEY = "play_when_ready";
    private static final String VIDEO_URL = "https://d17h27t6h515a5.cloudfront.net/topher/2017/April/58ffda20_7-add-cream-mix-creampie/7-add-cream-mix-creampie.mp4";

    private FrameLayout mPreviewFrame;
    private ImageView mPreviewImage;
    PlayerView mPlayerView;
    private boolean isPlayerInitialised;
    private PlayerInstance mPlayerInstance;
    private long mCurrentPosition;
    private int mCurrentWindowIndex;
    private boolean mPlayWhenReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreviewFrame = findViewById(R.id.frame_preview);
        mPreviewImage = findViewById(R.id.iv_preview);
        mPlayerView = findViewById(R.id.player_view);

        if (savedInstanceState != null) {
            mCurrentPosition = savedInstanceState.getLong(CURRENT_POSITION_KEY);
            mCurrentWindowIndex = savedInstanceState.getInt(CURRENT_WINDOW_KEY);
            mPlayWhenReady = savedInstanceState.getBoolean(PLAY_WHEN_READY_KEY);
            isPlayerInitialised = true;
        } else {
            mCurrentPosition = C.TIME_UNSET;
            mCurrentWindowIndex = C.INDEX_UNSET;
            mPlayWhenReady = false;
            isPlayerInitialised = false;
        }

        Bundle bundle = new Bundle();
        bundle.putLong(CURRENT_POSITION_KEY, mCurrentPosition);
        bundle.putInt(CURRENT_WINDOW_KEY, mCurrentWindowIndex);
        bundle.putBoolean(PLAY_WHEN_READY_KEY, mPlayWhenReady);

        mPlayerInstance = new PlayerInstance(this, mPlayerView, bundle);
    }

    @Override
    protected void onStart() {
        super.onStart();
        /* Marshmallow supports multi-window, so initialise the player in onStart instead of onResume
         * Also release the player in the respective lifecycle end calls.
         */
        if (Util.SDK_INT >= 24 && isPlayerInitialised) {
            playVideo();
        } else {
            initialisePreview();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Util.SDK_INT < 24 && isPlayerInitialised) {
            playVideo();
        } else {
            initialisePreview();
        }
    }

    private void playVideo() {
        mPreviewFrame.setVisibility(View.GONE);
        mPlayerView.setVisibility(View.VISIBLE);
        mPlayerInstance.play(VIDEO_URL);
    }

    private void initialisePreview() {
        Log.d(LOG_TAG, "initialisePreview()");
        AppExecutors.getInstance().networkIO().execute(() -> {
            Bitmap image = getVideoBitmap(VIDEO_URL);
            AppExecutors.getInstance().mainThread().execute(() ->
                    mPreviewImage.setImageBitmap(image));
        });
        mPreviewImage.setOnClickListener(v -> {
            playVideo();
        });
    }

    private Bitmap getVideoBitmap(String path) {
        Log.d(LOG_TAG, "getVideoBitmap()");
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = null;

        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path, new HashMap<>());
            bitmap = retriever.getFrameAtTime();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception caught in getVideoBitmap: " + e.getMessage());
        } finally {
            if (null != retriever) {
                retriever.release();
            }
        }
        return bitmap;
    }

    private void closePlayer() {
        mPreviewFrame.setVisibility(View.VISIBLE);
        mPlayerView.setVisibility(View.GONE);
        Bundle bundle = mPlayerInstance.close();
        mCurrentPosition = bundle.getLong(CURRENT_POSITION_KEY);
        mCurrentWindowIndex = bundle.getInt(CURRENT_WINDOW_KEY);
        mPlayWhenReady = bundle.getBoolean(PLAY_WHEN_READY_KEY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) {
            mPlayerInstance.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            mPlayerInstance.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closePlayer();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CURRENT_POSITION_KEY, mCurrentPosition);
        outState.putInt(CURRENT_WINDOW_KEY, mCurrentWindowIndex);
        outState.putBoolean(PLAY_WHEN_READY_KEY, mPlayWhenReady);
    }
}
