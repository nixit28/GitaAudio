package co.shunya.gita.db.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import co.shunya.gita.db.DB;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by nixit on 8/18/14.
 */
public class Track {
    private long _id;
    private String title;
    private String url;
    private int album;
    private String language;
    private boolean downloded;
    private int trackNo;

    public Track() {
    }

    public Track(Cursor cursor) {
        this._id = cursor.getLong(cursor.getColumnIndex(DB.TrackTable._id));
        this.title = cursor.getString(cursor.getColumnIndex(DB.TrackTable.title));
        this.url = cursor.getString(cursor.getColumnIndex(DB.TrackTable.url));
        this.language = cursor.getString(cursor.getColumnIndex(DB.TrackTable.lang));
        this.downloded = cursor.getInt(cursor.getColumnIndex(DB.TrackTable.downloded)) == 1;
        this.trackNo = cursor.getInt(cursor.getColumnIndex(DB.TrackTable.trackNo));
        this.album = cursor.getInt(cursor.getColumnIndex(DB.TrackTable.album));
    }

    public Track(JSONObject jObject) throws JSONException {
        this.title = jObject.getString("title");
        this.url = jObject.getString("url");
        this.language = jObject.getString("lang");
        this.trackNo = jObject.getInt("track");
    }

    public ContentValues getDefaultContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DB.TrackTable.title, getTitle());
        cv.put(DB.TrackTable.lang, getLanguage());
        cv.put(DB.TrackTable.url, getUrl());
        cv.put(DB.TrackTable.trackNo, getTrackNo());
        cv.put(DB.TrackTable.album, getAlbum());
        cv.put(DB.TrackTable.downloded, isDownloded()?1:0);
        return cv;
    }

    public String getLocalUrl(Context context) {
        Log.i(Track.class.getSimpleName(), url);
        File file = getLocalFile(context);
        if (file.exists()) {
            Log.i(url, file.getPath());
            Log.i(url, file.exists() + "");
            return Uri.fromFile(file).toString();
        }
        return url;
    }

    public File getLocalFile(Context context) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(dir, getFileNameFromUrl());
        return file;
    }

    public long getId() {
        return _id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getLanguage() {
        return language;
    }

    public int getLanguageCode() {
        if (language.equals("Gujarati")) {
            return 0;
        } else if (language.equals("Hindi")) {
            return 1;
        } else if (language.equals("Sanskrit")) {
            return 2;
        } else if (language.equals("Downloaded")) {
            return 3;
        } else
            return 0;
    }

    public void setDownloded(boolean downloded) {
        this.downloded = downloded;
    }

    public boolean isDownloded() {
        return downloded;
    }

    public int getTrackNo() {
        return trackNo;
    }

    public int getAlbum() {
        return album;
    }

    public void setAlbum(int album) {
        this.album = album;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Track)
            return ((Track) o)._id == this.getId();
        return false;
    }

    public String getFileNameFromUrl() {
        String url = getUrl();
        return getLanguage()+"_" + url.split("/")[url.split("/").length - 1];
    }

    public int getIdFromUrl(){

        return -1;
    }


    @Override
    public int hashCode() {
        return Long.valueOf(_id).hashCode();
    }
}
