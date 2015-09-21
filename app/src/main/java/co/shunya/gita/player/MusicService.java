/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.shunya.gita.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import co.shunya.gita.BusProvider;
import co.shunya.gita.Constant;
import co.shunya.gita.R;
import co.shunya.gita.db.model.Track;
import co.shunya.gita.view.activity.MainActivity;
import com.squareup.otto.Produce;

import java.io.IOException;

/**
 * Service that handles media playback. This is the Service through which we perform all the media
 * handling in our application. Upon initialization, it starts a {@link MusicRetriever} to scan
 * the user's media. Then, it waits for Intents (which come from our main activity,
 * {@link MainActivity}, which signal the service to perform specific operations: Play, Pause,
 * Rewind, Skip, etc.
 */
public class MusicService extends Service implements OnCompletionListener, OnPreparedListener,
        OnErrorListener, MusicFocusable,
        PrepareMusicRetrieverTask.MusicRetrieverPreparedListener {

    // The tag we put on debug messages
    final static String TAG = "GitaPlayer";

    // These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
    public static final String ACTION_TOGGLE_PLAYBACK = "co.shunya.gita.player.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "co.shunya.gita.player.action.PLAY";
    public static final String ACTION_PAUSE = "co.shunya.gita.player.action.PAUSE";
    public static final String ACTION_STOP = "co.shunya.gita.player.action.STOP";
    public static final String ACTION_SKIP = "co.shunya.gita.player.action.SKIP";
    public static final String ACTION_REWIND = "co.shunya.gita.player.action.REWIND";
    public static final String ACTION_SEEKBAR_CHANGE = "co.shunya.gita.player.action.SEEKBAR_CHANGE";

    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;
    public static final String INTENT_LANG_NAME_EXTRA = "intent_lang_extra";
    public static final String INTENT_ID_EXTRA = "intent_id_extra";
    public static final String INTENT_ALBUM_EXTRA = "album_extra";
    public static final String INTENT_PROGRESS_EXTRA = "intent_progress_extra";
    public static final int SEEKBAR_UPDATE = 1;

    private static final Handler HANDLER = new Handler(new Handler.Callback() {


        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case SEEKBAR_UPDATE: {
                    SeekBarListener seekBarListener = (SeekBarListener) message.obj;
                    BusProvider.getMediaEventBus().post(seekBarListener);
                    return true;
                }
            }
            return false;
        }
    });


    // our media player
    MediaPlayer mPlayer = null;

    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;
    private Long mCurrentID = -1l;
    private ProgressUpdateTask progressUpdateTask;

    // indicates the state our service:
    public enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
        // paused in this state if we don't have audio focus. But we stay in this state
        // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    }


    State mState = State.Retrieving;

    // if in Retrieving mode, this flag indicates whether we should start playing immediately
    // when we are ready or not.
    boolean mStartPlayingAfterRetrieve = false;

    // if mStartPlayingAfterRetrieve is true, this variable indicates the URL that we should
    // start playing when we are ready. If null, we should play a random song from the device
    Long mWhatToPlayAfterRetrieve = null;

    enum PauseReason {
        UserRequest,  // paused by user request
        FocusLoss,    // paused because of audio focus loss
    }

    // why did we pause? (only relevant if mState == State.Paused)
    PauseReason mPauseReason = PauseReason.UserRequest;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    // title of the song we are currently playing
    String mSongTitle = "";

    // whether the song we are playing is streaming from the network
    boolean mIsStreaming = false;

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiLock mWifiLock;

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    final int NOTIFICATION_ID = 1;
    final int NOTIFICATION_ID_COMPACT = 2;

    // Our instance of our MusicRetriever, which handles scanning for media and
    // providing titles and URIs as we need.
    MusicRetriever mRetriever;

    // our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClientCompat mRemoteControlClientCompat;

    // Dummy album art we will pass to the remote control (if the APIs are available).
    Bitmap mDummyAlbumArt;

    // The component name of MusicIntentReceiver, for use with media button and remote control
    // APIs
    ComponentName mMediaButtonReceiverComponent;

    AudioManager mAudioManager;
    NotificationManager mNotificationManager;

    Notification mNotification = null;

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
        } else
            mPlayer.reset();
    }

    @Override
    public void onCreate() {
//        Log.i(TAG, "debug: Creating service");

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        else
            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus

        mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.krishnanarayana);

        mMediaButtonReceiverComponent = new ComponentName(this, MusicIntentReceiver.class);

        BusProvider.getMediaEventBus().register(this);

    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest(intent);
        else if (action.equals(ACTION_PLAY)) {
            String lName = intent.getStringExtra(INTENT_LANG_NAME_EXTRA);
            int album = intent.getIntExtra(INTENT_ALBUM_EXTRA, -1);
            if (lName == null || album < 1) {
                throw new IllegalArgumentException("Must Pass Lang Name in intent");
            }
            if (!lName.equals(MusicRetriever.musicRetriever) || album == MusicRetriever.mAlbumId) {
                setState(State.Retrieving);
                mRetriever = MusicRetriever.getInstance(this, lName, album);
                new PrepareMusicRetrieverTask(mRetriever, this).execute();
            }

            processPlayRequest(intent);
        } else if (action.equals(ACTION_PAUSE)) processPauseRequest();
        else if (action.equals(ACTION_SKIP)) processSkipRequest();
        else if (action.equals(ACTION_STOP)) processStopRequest();
        else if (action.equals(ACTION_REWIND)) processRewindRequest();
        else if (action.equals(ACTION_SEEKBAR_CHANGE)) {
            int progress = intent.getIntExtra(INTENT_PROGRESS_EXTRA, 0);
            processSeekbarChange(progress);
        }

        return START_NOT_STICKY; // Means we started the service, but don't want it to
        // restart in case it's killed.
    }

    void processTogglePlaybackRequest(Intent intent) {
        if (mState == State.Paused || mState == State.Stopped) {
            processPlayRequest(intent);
        } else {
            processPauseRequest();
        }
    }

    void processPlayRequest(Intent intent) {
        Long _id = intent.getLongExtra(INTENT_ID_EXTRA, -1);
//        if (_id < 0) {
//            throw new IllegalArgumentException("You must pass the id and language to the intent");
//        }

        if (mState == State.Retrieving) {
            // If we are still retrieving media, just set the flag to start playing when we're
            // ready
            if (_id < 0) {
                throw new IllegalArgumentException("something went wrong intent");
            }

            mWhatToPlayAfterRetrieve = _id; // play a requested song
            mStartPlayingAfterRetrieve = true;
            return;
        }

        tryToGetAudioFocus();

        // actually play the song
        //TODO set the playing mechanism
        Log.i(TAG, "Current State of player:" + mState.toString());
        if (mState == State.Stopped) {
            if (_id < 0) {
                throw new IllegalArgumentException("something went wrong with intent");
            }
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong(_id);
        } else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            setState(State.Playing);
            updateUI();
            setUpAsForeground(mSongTitle + " (playing)");
            configAndStartMediaPlayer();
        }


        // Tell any remote controls that our playback state is 'playing'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
    }

    void processPauseRequest() {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, clear the flag that indicates we should start
            // playing when we're ready
            mStartPlayingAfterRetrieve = false;
            return;
        }

        Log.i(TAG, "Current State of player:" + mState.toString());
        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            setState(State.Paused);
            mPlayer.pause();
            updateUI();
            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
        }
        updateNotification(mSongTitle + " (pause)", R.drawable.ic_state_pause);

        // Tell any remote controls that our playback state is 'paused'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }

    private void processSeekbarChange(int progress) {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, clear the flag that indicates we should start
            // playing when we're ready
            mStartPlayingAfterRetrieve = false;
            return;
        }

        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.

            mPlayer.seekTo(mPlayer.getDuration() * progress / 100);
            // do not give up audio focus
        }
    }


    void processRewindRequest() {
        if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();
            if (mCurrentID >= 2)
                playNextSong(mCurrentID - 1);
        }
    }

    void processSkipRequest() {
        if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();
            playNextSong(mCurrentID + 1);
        }
    }

    void processStopRequest() {
        processStopRequest(false);
    }

    void processStopRequest(boolean force) {
        if (mState == State.Playing || mState == State.Paused || force) {
            setState(State.Stopped);

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // Tell any remote controls that our playback state is 'paused'.
            if (mRemoteControlClientCompat != null) {
                mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }

            // service is no longer necessary. Will be started again if needed.
            BusProvider.getMediaEventBus().unregister(this);
            stopSelf();

        }
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();
    }

    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mPlayer.isPlaying()) mPlayer.pause();
            return;
        } else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
        else
            mPlayer.setVolume(1.0f, 1.0f); // we can be loud

        if (!mPlayer.isPlaying()) {
            mPlayer.start();
        }

        //starting Seekbar
//        Log.i(TAG, "Thread will stated");
        if (progressUpdateTask != null) progressUpdateTask.setWildCard(false);
        progressUpdateTask = new ProgressUpdateTask();
        new Thread(progressUpdateTask).start();
//        Log.i(TAG, "Thread is started");
    }

    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    void playNextSong(Long id) {
        if (id == null || id < 0) {
            throw new IllegalArgumentException("id must be non nul +ve");
        }
        setState(State.Stopped);
        relaxResources(false); // release everything except MediaPlayer

        try {
            // set the source of the media player to a manual URL or path
            Track playingItem = mRetriever.getItem(id);
            createMediaPlayerIfNeeded();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            if (playingItem == null) {
                Toast.makeText(this,
                        "All Tracks of current category is been played", Toast.LENGTH_LONG).show();
                processStopRequest(true); // stop everything!
                return;
            }
//            BusProvider.getInstance().post(playingItem);
            mCurrentID = id;
            mPlayer.setDataSource(playingItem.isDownloded()?playingItem.getLocalUrl(this):playingItem.getUrl());
            mIsStreaming = playingItem.getUrl().startsWith("http:") || playingItem.getUrl().startsWith("https:");
            mSongTitle = playingItem.getTitle();

            setState(State.Preparing);
            updateUI();

            setUpAsForeground(mSongTitle + " (loading)");

            // Use the media button APIs (if available) to register ourselves for media button
            // events

            MediaButtonHelper.registerMediaButtonEventReceiverCompat(mAudioManager, mMediaButtonReceiverComponent);

            // Use the remote control APIs (if available) to set the playback state

            if (mRemoteControlClientCompat == null) {
                Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                intent.setComponent(mMediaButtonReceiverComponent);
                mRemoteControlClientCompat = new RemoteControlClientCompat(
                        PendingIntent.getBroadcast(this /*context*/,
                                0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
                RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                        mRemoteControlClientCompat);
            }

            mRemoteControlClientCompat.setPlaybackState(
                    RemoteControlClient.PLAYSTATE_PLAYING);

            mRemoteControlClientCompat.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                            RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                            RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                            RemoteControlClient.FLAG_KEY_MEDIA_STOP);

            // Update the remote controls
            mRemoteControlClientCompat.editMetadata(true)
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, "Gita")
                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, playingItem.getTitle())
                    .putBitmap(
                            RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                            mDummyAlbumArt)
                    .apply();

            // starts preparing the media player in the background. When it's done, it will call
            // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
            // the listener to 'this').
            //
            // Until the media player is prepared, we *cannot* call start() on it!
            mPlayer.prepareAsync();

            // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
            // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
            // we are *not* streaming, we want to release the lock if we were holding it before.
            if (mIsStreaming) mWifiLock.acquire();
            else if (mWifiLock.isHeld()) mWifiLock.release();
        } catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void updateUI() {
        AudioUpdateListener aListener = new AudioUpdateListener();
        aListener.track = mRetriever.getItem(mCurrentID);
        aListener.state = mState;
        BusProvider.getMediaEventBus().post(aListener);
    }

    @Produce
    public AudioUpdateListener getAudioUpdateListener() {
        if (mRetriever != null) {
            AudioUpdateListener aListener = new AudioUpdateListener();
            aListener.track = mRetriever.getItem(mCurrentID);
            aListener.state = mState;
//            Log.i(TAG, "Producing the result");
            return aListener;
        }
        return null;
    }

    public void setState(State mState) {
        this.mState = mState;
    }

    /**
     * Called when media player is done playing current song.
     */
    public void onCompletion(MediaPlayer player) {
        // The media player finished playing the current song, so we go ahead and start the next.
        playNextSong(mCurrentID + 1);
    }

    /**
     * Called when media player is done preparing.
     */
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing!
        setState(State.Playing);
        updateUI();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(Constant.PREF_LAST_PLAY, mRetriever.getItem(mCurrentID).getTitle());
        editor.commit();
//        player.setOnBufferingUpdateListener(bufferListener);
//        mAudioUpdateClass.state = mState;
//        BusProvider.getMediaEventBus().post(mAudioUpdateClass);
        updateNotification(mSongTitle + " (playing)", R.drawable.ic_stat_playing);
        configAndStartMediaPlayer();
        new Thread(new ProgressUpdateTask()).start();
    }

    private class ProgressUpdateTask implements Runnable {

        private boolean wildCard = true;

        @Override
        public void run() {
            while (mPlayer != null && mPlayer.isPlaying() && wildCard) {
                try {
//                    Log.i(TAG, "in side run");
                    int duration = mPlayer.getDuration();
                    int current = mPlayer.getCurrentPosition();
                    SeekBarListener seekBarListener = new SeekBarListener();
                    seekBarListener.duration = duration;
                    seekBarListener.currentTime = current;
                    Message message = new Message();
                    message.what = SEEKBAR_UPDATE;
                    message.obj = seekBarListener;
                    HANDLER.sendMessage(message);
//                    Log.i(TAG, "Thread Sleeping time:" + mPlayer.getDuration() / 100);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setWildCard(boolean wildCard) {
            this.wildCard = wildCard;
        }
    }

    @Produce
    public SeekBarListener provideInitialSeekbar() {
        SeekBarListener seekBarListener = new SeekBarListener();
        seekBarListener.duration = mPlayer != null ? mPlayer.getDuration() : 0;
        seekBarListener.currentTime = mPlayer != null ? mPlayer.getCurrentPosition() : 0;
        return seekBarListener;
    }

    /**
     * Updates the notification.
     */
    void updateNotification(String text, int rsdId) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.gopinathji);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setAutoCancel(false)
                .setSmallIcon(rsdId)
                .setLargeIcon(icon)
                .setContentTitle(getString(R.string.app_name_reader))
                .setContentText(text).setOngoing(true);
        builder.setContentIntent(pi);
        issueNotification(builder);
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.gopinathji);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_stat_playing)
                .setLargeIcon(icon)
                .setContentTitle(getString(R.string.app_name_reader))
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(pi);
        issueNotification(builder);
        startForeground(NOTIFICATION_ID, mNotification);
    }

    private void issueNotification(NotificationCompat.Builder builder) {
        mNotification = builder.build();
        // Including the notification ID allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));
        if (extra == MediaPlayer.MEDIA_ERROR_IO) {
            Toast.makeText(getApplicationContext(), "Connectivity Problem" + (mIsStreaming ? "\nError Connecting with internet" : ""), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Unknon Error\nMedia Player resetting", Toast.LENGTH_SHORT).show();
        }

        setState(State.Stopped);
        relaxResources(true);
        giveUpAudioFocus();
        return true; // true indicates we handled the error
    }

    public void onGainedAudioFocus() {
        Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    public void onLostAudioFocus(boolean canDuck) {
        Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" :
                "no duck"), Toast.LENGTH_SHORT).show();
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mPlayer != null && mPlayer.isPlaying())
            configAndStartMediaPlayer();
    }

    public void onMusicRetrieverPrepared() {
        // Done retrieving!
        setState(State.Stopped);

        // If the flag indicates we should start playing after retrieving, let's do that now.
        if (mStartPlayingAfterRetrieve) {
            tryToGetAudioFocus();
            playNextSong(mWhatToPlayAfterRetrieve);
        }
    }


    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        setState(State.Stopped);
        relaxResources(true);
        giveUpAudioFocus();
    }

    public class AudioUpdateListener {
        public Track track;
        public State state;
    }

    public class SeekBarListener {
        public int currentTime;
        public int duration;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
