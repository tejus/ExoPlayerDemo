package com.tejus.exoplayerdemo;

import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.exoplayer2.ui.PlayerView;

public class FullscreenDialogFragment extends DialogFragment {

    private static final String LOG_TAG = FullscreenDialogFragment.class.getSimpleName();

    PlayerView mPlayerView;

    public static FullscreenDialogFragment newInstance() {
        return new FullscreenDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_fullscreen_dialog, container, false);
        mPlayerView = rootView.findViewById(R.id.fullscreen_player_view);
        ((MainActivity) getActivity()).switchToFullscreen(mPlayerView);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                Log.d("FullscreenDialogFrag", "in overriden onBackPress");
                ((MainActivity) getActivity()).disableFullscreen(mPlayerView, this);
            }
        };
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}
