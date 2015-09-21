package co.shunya.gita.view.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import co.shunya.gita.Constant;
import co.shunya.gita.OnBlankEvent;
import co.shunya.gita.R;
import co.shunya.gita.db.DB;
import co.shunya.gita.db.TrackDao;
import co.shunya.gita.db.model.Track;
import co.shunya.gita.utils.IOUtils;
import co.shunya.gita.view.activity.util.SystemUiHider;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class StartActivity extends Activity {

    private static final String TAG = StartActivity.class.getSimpleName();
    private static Handler handler = new Handler();
    private Bus listLoadBus;
    private ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start);

        //Setting Up App for the first use
        SharedPreferences pf = PreferenceManager.getDefaultSharedPreferences(this);
        listLoadBus = new Bus(ThreadEnforcer.MAIN);
        listLoadBus.register(this);

        boolean isFirst = pf.getBoolean(Constant.PREF_ISFIRST, true);
        Log.i(TAG, "Check is first:" + isFirst);
        if (isFirst) {
            dialog = ProgressDialog.show(StartActivity.this, "", "Loading...");
            new Thread(dataLoadThread).start();
            SharedPreferences.Editor editor = pf.edit();
            editor.putBoolean(Constant.PREF_ISFIRST, false);
            editor.commit();
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(StartActivity.this, MainActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }
            }, 5000);
        }
    }

    @Subscribe
    public void listResult(OnBlankEvent o) {
        Toast.makeText(this, "I got the data", Toast.LENGTH_LONG).show();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(StartActivity.this, MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        }, 3000);

    }

    private Runnable dataLoadThread = new Runnable() {

        @Override
        public void run() {
            final InputStream is = getResources().openRawResource(R.raw.gita);
            final InputStream isbhaj = getResources().openRawResource(R.raw.bhaj_govindam);
            String strJson = IOUtils.getStringFromInputStream(is);
            String strJsonBhaj = IOUtils.getStringFromInputStream(isbhaj);
            try {
                JSONArray jsonArray = new JSONObject(strJson).getJSONArray("Gita");
                TrackDao trackDao = new TrackDao(StartActivity.this);
                for (int i = 0; i < jsonArray.length(); i++) {
                    Track track = new Track(jsonArray.getJSONObject(i));
                    track.setAlbum(DB.ALBUM_GITA);
                    Log.i(TAG, track.getTitle());
                    trackDao.insert(track);
                }
                ArrayList<Track> tracks = new ArrayList<Track>();
                jsonArray = new JSONObject(strJsonBhaj).getJSONArray("BhajGovindam");
                for (int i = 0; i < jsonArray.length(); i++) {
                    Track track = new Track(jsonArray.getJSONObject(i));
                    track.setAlbum(DB.ALBUM_BHAJ);
                    track.getFileNameFromUrl();
                    tracks.add(track);
//                    trackDao.insert(track);
                }
                Collections.sort(tracks, trackComparator);
                for (Track track : tracks) {

                    trackDao.insert(track);
                }

                File dir = StartActivity.this.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                for (File f : dir.listFiles()) {
                    if (f.isFile()) {
                        String name = f.getName();
                        String lang = name.split("_", 2)[0];
                        String fName = name.split("_", 2)[1];
                        String query = "Select _id from "+DB.TrackTable.TABLE_NAME+" where "+ DB.TrackTable.lang+" = '"+lang+"' and "+ DB.TrackTable.url+" like '%"+fName+"%';";
                        Cursor cursor = trackDao.query(query);
                        if(cursor.getCount()>0) {
                            long id = cursor.getLong(cursor.getColumnIndex(DB.TrackTable._id));
                            ContentValues cv = new ContentValues();
                            cv.put(DB.TrackTable.downloded, 1);
                            trackDao.update(cv,id);
                        }
                        cursor.close();
                    }
                    // Do your stuff
                }
                trackDao.close();
                StartActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listLoadBus.post(new OnBlankEvent());
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    };

    private Comparator<Track> trackComparator = new Comparator<Track>() {
        @Override
        public int compare(Track track, Track track2) {
            return Integer.valueOf(track.getTrackNo()).compareTo(Integer.valueOf(track2.getTrackNo()));
        }
    };


}
