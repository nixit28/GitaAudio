package co.shunya.gita.view.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import co.shunya.gita.BusProvider;
import co.shunya.gita.DownloadService;
import co.shunya.gita.R;
import co.shunya.gita.db.model.Track;
import co.shunya.gita.player.MusicService;
import com.squareup.otto.Subscribe;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by nixit on 8/21/14.
 */
public class PlayerFragment extends Fragment implements PlaceholderFragment.ListItemClickListener {

    private static final String TAG = PlayerFragment.class.getSimpleName();
    private ImageButton btnPlay;
    private Track mTrack;
    private UpdateMainScreen updateMainScreen;
    private SeekBar seekBarCtrl;
    private TextView txtDuration;
    private TextView txtCurrentTime;

    public static PlayerFragment newInstance() {
        PlayerFragment fragment = new PlayerFragment();
        return fragment;
    }

    @Override
    public void onClick(Track track) {
        this.mTrack = track;
        Intent intent = new Intent();
        intent.setAction(MusicService.ACTION_PLAY);
        musicIntentHelper(track, intent);

    }

    private void musicIntentHelper(Track track, Intent intent) {
        intent.putExtra(MusicService.INTENT_LANG_NAME_EXTRA, track.getLanguage());
        intent.putExtra(MusicService.INTENT_ID_EXTRA, track.getId());
        intent.putExtra(MusicService.INTENT_ALBUM_EXTRA, track.getAlbum());
        getActivity().startService(intent);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        updateMainScreen = (UpdateMainScreen) activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "Registering Serive");
        BusProvider.getMediaEventBus().register(this);
    }

    @Override
    public void onPause() {
        super.onDestroy();
        BusProvider.getMediaEventBus().unregister(this);
        Log.i(TAG, "UNRegistering Serive");
    }

    @Subscribe
    public void onUIUpdate(MusicService.AudioUpdateListener listener) {
        if (listener != null && listener.track != null) {

            mTrack = listener.track;
            switch (listener.state) {
                case Retrieving:
                    break;
                case Stopped:
                    break;
                case Preparing:
                    Animation rotation_anim = AnimationUtils.loadAnimation(getActivity(), R.anim.translate_rotate);
                    btnPlay.startAnimation(rotation_anim);
                    break;
                case Playing:
                    btnPlay.clearAnimation();
                    btnPlay.setSelected(true);
                    break;
                case Paused:
                    btnPlay.clearAnimation();
                    btnPlay.setSelected(false);
                    break;
            }
            updateMainScreen.updateMainScreen(listener);
        }
    }

    @Subscribe
    public void onSeekBarUpdate(MusicService.SeekBarListener seekBarListener) {
        if (seekBarListener.duration != 0) {
            int progress = seekBarListener.currentTime * 100 / seekBarListener.duration;
//            Log.i(TAG, "Progress is..." + progress);
            SimpleDateFormat format = new SimpleDateFormat("mm:ss");

            txtDuration.setText(format.format(new Date(seekBarListener.duration)));
            txtCurrentTime.setText(format.format(new Date(seekBarListener.currentTime)));
            seekBarCtrl.setProgress(progress);
        } else {
            seekBarCtrl.setProgress(0);

        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View playerView = inflater.inflate(R.layout.fragment_player, container, false);
        initView(playerView);
        return playerView;
    }

    private void initView(View playerView) {
        btnPlay = (ImageButton) playerView.findViewById(R.id.btnPlayCtrl);
        btnPlay.setOnClickListener(btnPlayClickListener);
        seekBarCtrl = (SeekBar) playerView.findViewById(R.id.seekBarCtrl);
        playerView.findViewById(R.id.btnBackCtrl).setOnClickListener(btnBackClickListener);
        playerView.findViewById(R.id.btnDownloadCtrl).setOnClickListener(btnDownloadClickListener);
        playerView.findViewById(R.id.btnNextCtrl).setOnClickListener(btnNextClickListener);
        playerView.findViewById(R.id.btnShareCtrl).setOnClickListener(btnShareClickListener);
        playerView.findViewById(R.id.btnStopCtrl).setOnClickListener(btnStopClickListener);
        txtDuration = (TextView) playerView.findViewById(R.id.txtDuration);
        txtCurrentTime = (TextView) playerView.findViewById(R.id.txtCurrentTime);
        seekBarCtrl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Log.i(TAG, "Progree is..." + progress);
                    Intent intent = new Intent();
                    intent.setAction(MusicService.ACTION_SEEKBAR_CHANGE);
                    intent.putExtra(MusicService.INTENT_PROGRESS_EXTRA, progress);
                    musicIntentHelper(mTrack, intent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private View.OnClickListener btnPlayClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setAction(MusicService.ACTION_TOGGLE_PLAYBACK);
            musicIntentHelper(mTrack, intent);

        }
    };

    private View.OnClickListener btnDownloadClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(getActivity(), DownloadService.class);
            intent.setAction(DownloadService.ACTION_DOWNLOAD);
            intent.putExtra(DownloadService.INTENT_DOWNLOAD_ID_EXTRA, mTrack.getId());
            getActivity().startService(intent);
            Toast.makeText(getActivity(), "Downloading Track:" + mTrack.getId(), Toast.LENGTH_LONG).show();

        }
    };

    private View.OnClickListener btnStopClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setAction(MusicService.ACTION_STOP);
            musicIntentHelper(mTrack, intent);
            updateMainScreen.onClickStop();
        }
    };

    private View.OnClickListener btnShareClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);//Uri.parse(mGita.getUrl())
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(mTrack.getUrl()));
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Gita adhayah from : https://play.google.com/store/apps/details?id=com.shunya.gita");
            shareIntent.setType("Audio/*");
            startActivity(Intent.createChooser(shareIntent, "Bhagvad Gita " + mTrack.getTitle()));
        }
    };

    private View.OnClickListener btnNextClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setAction(MusicService.ACTION_SKIP);
            getActivity().startService(intent);
        }
    };

    private View.OnClickListener btnBackClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setAction(MusicService.ACTION_REWIND);
            getActivity().startService(intent);
        }
    };

    public interface UpdateMainScreen {
        public void updateMainScreen(MusicService.AudioUpdateListener listener);
        public void onClickStop();
    }
}
