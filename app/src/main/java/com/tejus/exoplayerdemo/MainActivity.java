package com.tejus.exoplayerdemo;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private PlayerView mPlayerView;
    private SimpleExoPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayerView = findViewById(R.id.player_view);
        String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");

        mPlayer = ExoPlayerFactory.newSimpleInstance(this);
        mPlayerView.setPlayer(mPlayer);
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, userAgent);
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse("asset:///-intro-creampie.mp4"));

        DefaultHttpDataSourceFactory httpSourceFactory = new DefaultHttpDataSourceFactory(userAgent);
        ExtractorMediaSource extractorMediaSource = new ExtractorMediaSource.Factory(httpSourceFactory)
                .createMediaSource(Uri.parse("https://d17h27t6h515a5.cloudfront.net/topher/2017/April/58ffd974_-intro-creampie/-intro-creampie.mp4"));
        mPlayer.prepare(extractorMediaSource);
        //mPlayer.setPlayWhenReady(true);
    }
}
