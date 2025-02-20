package com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.support;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection.Factory;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.ExoPlayerFragment;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.support.trackstate.PlayerStateManagerBase;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.support.trackstate.PlayerStateManagerBase.MyFormat;

public class MyDefaultTrackSelector extends DefaultTrackSelector {
    private final Context mContext;
    private final PlayerStateManagerBase mStateManager;
    private boolean mAlreadyRestored;

    public MyDefaultTrackSelector(Factory trackSelectionFactory, Context context) {
        super(trackSelectionFactory);
        mContext = context;
        mStateManager = new PlayerStateManagerBase(context);
    }

    @Nullable
    @Override
    protected TrackSelection.Definition selectVideoTrack(TrackGroupArray groups, int[][] formatSupports, int mixedMimeTypeAdaptationSupports,
                                              Parameters params, boolean enableAdaptiveTrackSelection) throws ExoPlaybackException {

        // Restore state before video starts playing
        boolean isAuto = !params.hasSelectionOverride(ExoPlayerFragment.RENDERER_INDEX_VIDEO, groups);

        if (isAuto && !mAlreadyRestored) {
            mAlreadyRestored = true;
            restoreVideoTrack(groups);
        }

        return super.selectVideoTrack(groups, formatSupports, mixedMimeTypeAdaptationSupports, params, enableAdaptiveTrackSelection);
    }

    private void restoreVideoTrack(TrackGroupArray groups) {
        MyFormat format = mStateManager.findPreferredVideoFormat(groups);

        if (format != null) {
            setParameters(buildUponParameters().setSelectionOverride(
                    ExoPlayerFragment.RENDERER_INDEX_VIDEO,
                    groups,
                    new SelectionOverride(format.pair.first, format.pair.second)
            ));
        }
    }
}
