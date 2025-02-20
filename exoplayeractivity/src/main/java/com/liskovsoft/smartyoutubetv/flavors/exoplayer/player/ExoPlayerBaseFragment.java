package com.liskovsoft.smartyoutubetv.flavors.exoplayer.player;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.liskovsoft.exoplayeractivity.R;
import com.liskovsoft.sharedutils.dialogs.CombinedChoiceSelectorDialog;
import com.liskovsoft.sharedutils.dialogs.SingleChoiceSelectorDialog;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.autoframerate.AutoFrameRateManager;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.dialogs.restrictcodec.RestrictFormatDialogSource;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.dialogs.speed.SpeedDialogSource;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.dialogs.zoom.VideoZoomDialogSource;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.helpers.PlayerUtil;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.support.ExoPreferences;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.support.MyDebugViewHelper;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.support.MyDefaultTrackSelector;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.support.PlayerButtonsManager;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.support.PlayerInitializer;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.support.VideoZoomManager;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.support.trackstate.PlayerStateManager;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.widgets.LayoutToggleButton;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.widgets.TextToggleButton;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.widgets.ToggleButtonBase;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.widgets.previewtimebar.ExoPlayerManager;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.widgets.previewtimebar.PreviewTimeBar;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parsers.YouTubeStoryParser;
import com.liskovsoft.smartyoutubetv.fragments.PlayerListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public abstract class ExoPlayerBaseFragment extends PlayerCoreFragment {
    private static final String TAG = ExoPlayerBaseFragment.class.getName();

    public static final String BUTTON_FAVORITES = "button_favorites";
    public static final String BUTTON_USER_PAGE = "button_user_page";
    public static final String BUTTON_LIKE = "button_like";
    public static final String BUTTON_DISLIKE = "button_dislike";
    public static final String BUTTON_SUBSCRIBE = "button_subscribe";
    public static final String BUTTON_PREV = "button_prev";
    public static final String BUTTON_NEXT = "button_next";
    public static final String BUTTON_BACK = "button_back";
    public static final String BUTTON_SUGGESTIONS = "button_suggestions";
    public static final String VIDEO_DATE = "video_date";
    public static final String VIDEO_TITLE = "video_title";
    public static final String VIDEO_AUTHOR = "video_author";
    public static final String VIDEO_VIEW_COUNT = "video_views";
    public static final String VIDEO_ID = "video_id";
    public static final String TRACK_ENDED = "track_ended";
    public static final String DISPLAY_MODE_ID = "display_mode_id";
    public static final String VIDEO_CANCELED = "video_canceled";
    public static final String STORYBOARD_SPEC = "storyboard_spec";
    public static final String VIDEO_LENGTH = "video_length";
    public static final String VIDEO_POSITION = "video_position";

    private long mPosition;
    private int mInterfaceVisibilityState = View.INVISIBLE;
    private boolean mIsDurationSet;

    protected PlayerButtonsManager mButtonsManager;
    private PlayerInitializer mPlayerInitializer;
    private MyDebugViewHelper mDebugViewHelper;
    private AutoFrameRateManager mAutoFrameRateManager;
    private PlayerStateManager mStateManager;
    private VideoZoomManager mVideoZoomManager;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // NOTE: completely disable open/close animation for activity
        // overridePendingTransition(0, 0);

        mButtonsManager = new PlayerButtonsManager(this);
        mPlayerInitializer = new PlayerInitializer(this);
        mVideoZoomManager = new VideoZoomManager(getActivity(), mSimpleExoPlayerView);
    }

    @Override
    public void initializePlayer() {
        if (getIntent() == null || getActivity() == null) {
            return;
        }

        boolean needNewPlayer = mPlayer == null;
        boolean openNewVideo = mNeedRetrySource;
        super.initializePlayer();

        if (needNewPlayer) {
            mDebugViewHelper = new MyDebugViewHelper(mPlayer, mDebugViewGroup, getActivity());

            // Do not move this code to another place!!! This statement must come after player initialization
            mAutoFrameRateManager = new AutoFrameRateManager(getActivity(), mPlayer);

            mStateManager = new PlayerStateManager(this, mPlayer, mTrackSelector);

            mPlayerInitializer.initVideoTitle();

            initTimelinePreviews();
        }

        if (openNewVideo) {
            mAutoFrameRateManager.saveOriginalState();
            restoreSpeed();
        }
    }

    private void initTimelinePreviews() {
        PreviewTimeBar previewTimeBar = mSimpleExoPlayerView.findViewById(R.id.exo_progress);
        String spec = getIntent().getStringExtra(ExoPlayerFragment.STORYBOARD_SPEC);

        previewTimeBar.setPreviewLoader(
                new ExoPlayerManager(
                        previewTimeBar,
                        getView().findViewById(R.id.imageView),
                        new YouTubeStoryParser(spec).extractStory()
                )
        );
    }

    protected void initializeTrackSelector() {
        TrackSelection.Factory trackSelectionFactory =
                new AdaptiveTrackSelection.Factory();

        mTrackSelector = new MyDefaultTrackSelector(trackSelectionFactory, getActivity());

        mTrackSelector.setParameters(mTrackSelector.buildUponParameters().setForceHighestSupportedBitrate(true));

        // Commented out because of bug: can't instantiate OMX decoder...
        // NOTE: 'Tunneled video playback' (HDR and others) (https://medium.com/google-exoplayer/tunneled-video-playback-in-exoplayer-84f084a8094d)
        // Enable tunneling if supported by the current media and device configuration.
        //if (Util.SDK_INT >= 21) {
        //    mTrackSelector.setParameters(mTrackSelector.buildUponParameters().setTunnelingAudioSessionId(C.generateAudioSessionIdV21(getActivity())));
        //}

        mTrackSelectionHelper = new TrackSelectionHelper(mTrackSelector, trackSelectionFactory);
        mEventLogger = new EventLogger(mTrackSelector);
    }

    public void showDebugView(final boolean show) {
        if (mDebugViewHelper == null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showDebugView(show);
                }
            }, 1000);
            return;
        }

        mDebugViewHelper.show(show);
    }

    public String getMainTitle() {
        return mPlayerInitializer.getMainTitle();
    }

    public void onCheckedChanged(@NonNull ToggleButtonBase compoundButton, boolean b) {
        if (mButtonsManager != null)
            mButtonsManager.onCheckedChanged(compoundButton, b);
    }

    public void onPlayerAction() {
        Intent intent = mButtonsManager.createResultIntent();
        onPlayerAction(intent);
    }

    public void onPlayerAction(String action) {
        Intent intent = mButtonsManager.createResultIntent();
        intent.putExtra(action, true);
        onPlayerAction(intent);
    }

    private void onPlayerAction(Intent result) {
        fixSuggestionFocusLost();

        if (mPlayer != null) {
            result.putExtra(VIDEO_LENGTH, (float) mPlayer.getDuration() / 1_000);
            result.putExtra(VIDEO_POSITION, (float) mPlayer.getCurrentPosition() / 1_000);
        }

        if (getActivity() != null) {
            ((PlayerListener) getActivity()).onPlayerAction(result);
        }
    }

    private void fixSuggestionFocusLost() {
        if (mSimpleExoPlayerView == null) {
            return;
        }

        // wait till previously queued keys are being handled
        new Handler().postDelayed(mSimpleExoPlayerView::hideController, 500);
    }

    protected void syncButtonStates() {
        if (mButtonsManager != null) {
            mButtonsManager.syncButtonStates();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mStateManager != null)
            mStateManager.persistState(); // player about to crash
    }

    //@Override
    //public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    //    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    //        initializePlayer();
    //    } else {
    //        showToast(R.string.storage_permission_denied);
    //    }
    //}

    // OnClickListener methods

    @Override
    public void onClick(View view) {
        super.onClick(view);

        Context context = getActivity();
        if (context == null) {
            return;
        }

        if (view.getId() == R.id.btn_restrict_codec) {
            SingleChoiceSelectorDialog.create(context, new RestrictFormatDialogSource(context), R.style.AppDialog);
        } else if (view.getId() == R.id.btn_video_zoom) {
            SingleChoiceSelectorDialog.create(context, new VideoZoomDialogSource(context, mVideoZoomManager), R.style.AppDialog);
        }
    }

    @Override
    void addCustomButtonToQualitySection() {
        if (getActivity() == null) {
            return;
        }

        addRestrictCodecButton();
        addVideoZoomButton();
    }

    private void addRestrictCodecButton() {
        TextToggleButton button = new TextToggleButton(getActivity());
        button.setId(R.id.btn_restrict_codec);
        button.setText(R.string.btn_restrict_codec);
        button.setOnClickListener(this);
        mDebugRootView.addView(button, mDebugRootView.getChildCount() - 1);
    }

    private void addVideoZoomButton() {
        TextToggleButton button = new TextToggleButton(getActivity());
        button.setId(R.id.btn_video_zoom);
        button.setText(R.string.btn_video_zoom);
        button.setOnClickListener(this);
        mDebugRootView.addView(button, mDebugRootView.getChildCount() - 1);
    }

    // PlaybackControlView.VisibilityListener implementation

    @Override
    public void onVisibilityChange(int visibility) {
        mInterfaceVisibilityState = visibility;

        mPlayerTopBar.setVisibility(visibility);

        // NOTE: don't set to GONE or you will get fathom events
        if (visibility == View.VISIBLE) {
            resetStateOfLayoutToggleButtons();
            updateClockView();
        }
    }

    private void updateClockView() {
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Fragment's root view is null");
        }

        TextView clock = root.findViewById(R.id.clock);

        // details: https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
        SimpleDateFormat serverFormat = new SimpleDateFormat("EEE d MMM H:mm", Locale.getDefault());
        String currentTime = serverFormat.format(new Date());

        String timeTitle = getString(R.string.time_title, currentTime);
        clock.setText(timeTitle);
    }

    private void updateTitleQualityInfo() {
        View root = getView();

        if (root == null) {
            Log.e(TAG, "Fragment's root view is null");
            return;
        }

        TextView quality = root.findViewById(R.id.video_quality);
        quality.setText(PlayerUtil.getVideoQualityLabel(mPlayer));
    }

    private void resetStateOfLayoutToggleButtons() {
        View root = getView();
        if (root == null)
            throw new IllegalStateException("Fragment's root view is null");
        LayoutToggleButton toggleButton1 = root.findViewById(R.id.player_options_btn);
        LayoutToggleButton toggleButton2 = root.findViewById(R.id.player_quality_btn);
        toggleButton1.resetState();
        toggleButton2.resetState();
    }

    /**
     * Entry point: open brand new video
     * @param intent video info
     */
    protected void openVideoFromIntent(Intent intent) {
        Log.d(TAG, "Open video from intent=" + intent);

        if (isStateIntent(intent)) {
            if (getIntent() == null) {
                setIntent(intent);
            } else {
                Helpers.mergeIntents(getIntent(), intent);
            }

            mPlayerInitializer.initVideoTitle();
            syncButtonStates();
            return;
        }

        releasePlayer(); // dispose player
        mShouldAutoPlay = true; // force autoplay
        mNeedRetrySource = true; // process supplied intent
        clearResumePosition(); // restore position will be done later from the app storage
        setIntent(intent);
        syncButtonStates(); // onCheckedChanged depends on this
        initializePlayer();
    }

    private boolean isStateIntent(Intent intent) {
        return intent.getAction() == null;
    }

    /**
     * Used when exoplayer's fragment no longer visible (e.g. paused/resumed/stopped/started)
     */
    protected void releasePlayer() {
        if (mAutoFrameRateManager != null) {
            mAutoFrameRateManager.restoreOriginalState();
        }

        if (mPlayer != null) {
            mShouldAutoPlay = mPlayer.getPlayWhenReady(); // save paused state
            updateResumePosition(); // save position
            mPlayer.release();
            resetUiState();
        }

        if (mStateManager != null) {
            mStateManager.persistState();
        }

        if (mDebugViewHelper != null) {
            mDebugViewHelper.stop();
        }

        mAutoFrameRateManager = null;
        mPlayer = null;
        mStateManager = null; // force restore state
        mDebugViewHelper = null;
        mTrackSelector = null;
        mTrackSelectionHelper = null;
        mEventLogger = null;
        mIsDurationSet = false;
        mRetryCount = 0;
    }

    // ExoPlayer.EventListener implementation

    @Override
    public void onLoadingChanged(boolean isLoading) {
        // Do nothing.
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        restorePlayerStateIfNeeded();

        if (playbackState == Player.STATE_ENDED) {
            onPlayerAction(ExoPlayerBaseFragment.TRACK_ENDED);
        } else if (playbackState == Player.STATE_READY) {
            if (mAutoFrameRateManager != null) {
                mAutoFrameRateManager.apply();
            }

            mSimpleExoPlayerView.setControllerAutoShow(true); // show ui on pause or buffering
            mPlayerInitializer.initTimeBar(mPlayer); // set proper time increments
            updateTitleQualityInfo();
        }

        if (mSimpleExoPlayerView != null) {
            mSimpleExoPlayerView.setKeepScreenOn(playWhenReady && playbackState == Player.STATE_READY);
        }

        showHideLoadingMessage(playbackState);

        super.onPlayerStateChanged(playWhenReady, playbackState);
    }

    /**
     * Reset player's title and show loading
     */
    private void resetUiState() {
        showHideLoadingMessage(Player.STATE_IDLE);

        if (mPlayerInitializer != null) {
            mPlayerInitializer.resetVideoTitle();
        }

        if (mSimpleExoPlayerView != null) {
            // hide ui player by default
            mSimpleExoPlayerView.setControllerAutoShow(false);
            mSimpleExoPlayerView.hideController();

            // hide the last frame of the previous video
            View shutter = mSimpleExoPlayerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_shutter);

            if (shutter != null) {
                shutter.setVisibility(View.VISIBLE);
            }

            View loading = mSimpleExoPlayerView.findViewById(R.id.loading_view);

            if (loading != null) {
                loading.setVisibility(View.VISIBLE);
            }
        }

        //TimeBar timeBar = rootView.findViewById(R.id.player_view).findViewById(R.id.exo_progress);
        //timeBar.setEnabled(false);
        //timeBar.setDuration(C.TIME_UNSET);
        //timeBar.setPosition(0);
    }

    private void showHideLoadingMessage(int playbackState) {
        int visibility = playbackState == Player.STATE_READY ? View.GONE : View.VISIBLE;
        mLoadingView.setVisibility(visibility);
    }

    public AutoFrameRateManager getAutoFrameRateManager() {
        return mAutoFrameRateManager;
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        // NOTE: moving to ExoPlayer 2.5.3
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        if (mNeedRetrySource) {
            // This will only occur if the user has performed a seek whilst in the error state. Update the
            // resume position so that if the user then retries, playback will resume from the position to
            // which they seeked.
            updateResumePosition();
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        // Do nothing.
    }

    @Override
    public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {
        // NOP
    }

    /**
     * At this point user've changed video quality
     */
    void retryIfNeeded() {
        if (mNeedRetrySource) {
            initializePlayer();
        }
    }

    public void setHidePlaybackErrors(boolean hideErrors) {
        ExoPreferences prefs = ExoPreferences.instance(getActivity());
        prefs.setHidePlaybackErrors(hideErrors);
    }

    public boolean getHidePlaybackErrors() {
        ExoPreferences prefs = ExoPreferences.instance(getActivity());
        return prefs.getHidePlaybackErrors();
    }

    public void setRepeatEnabled(final boolean enabled) {
        if (mPlayer == null) {
            new Handler().postDelayed(() -> ExoPlayerBaseFragment.this.setRepeatEnabled(enabled), 1000);
            return;
        }

        int repeatMode = enabled ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF;
        mPlayer.setRepeatMode(repeatMode);
    }

    public void onSpeedClicked() {
        CombinedChoiceSelectorDialog.create(getActivity(), new SpeedDialogSource((ExoPlayerFragment) this), R.style.AppDialog);
    }

    @Override
    public boolean isUiVisible() {
        return mInterfaceVisibilityState == View.VISIBLE;
    }

    private void restoreSpeed() {
        ExoPreferences prefs = ExoPreferences.instance(getActivity());

        if (prefs.getRestoreSpeed()) {
            mPlayer.setPlaybackParameters(new PlaybackParameters(Float.parseFloat(prefs.getCurrentSpeed()), 1.0f));
        }
    }

    private void restorePlayerStateIfNeeded() {
        if (mTrackSelector != null && mTrackSelector.getCurrentMappedTrackInfo() != null && !mIsDurationSet) {
            mIsDurationSet = true; // run once per video

            if (mStateManager != null) {
                // stateManage should be initialized here
                mStateManager.restoreState();
            }

            if (mPlayer != null) {
                mPlayer.setPlayWhenReady(mShouldAutoPlay);
            }
        }
    }
}
