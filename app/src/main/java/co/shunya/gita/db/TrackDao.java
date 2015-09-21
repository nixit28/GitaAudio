package co.shunya.gita.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import co.shunya.gita.db.model.Track;
import co.shunya.gita.utils.db.DBUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by nixit on 8/18/14.
 */
public class TrackDao implements com.shunya.lib.DAO<Track> {

    private final SQLiteDatabase db;

    public TrackDao(Context context){
        this.db = DBUtils.getWriteableDatabase(context);
    }


    @Override
    public void close() {
        db.close();
    }

    @Override
    public void insert(Track track) {
        db.insert(DB.TrackTable.TABLE_NAME, null, track.getDefaultContentValues());
    }

    @Override
    public Track select(long id) {
        Cursor cursor = db.query(DB.TrackTable.TABLE_NAME,null, DB.TrackTable._id+"=?", new String[]{Long.toString(id)},null,null,null);
        cursor.moveToFirst();
        Track track = new Track(cursor);
        return track;
    }

    @Override
    public List<Track> select(String query) {
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.getCount()>0)
        cursor.moveToFirst();
        ArrayList<Track> tracks = new ArrayList<Track>();
        do{
            tracks.add(new Track(cursor));
        }while (cursor.moveToNext());
        cursor.close();
        return tracks;
    }

    public HashMap<Long, Track> selectHash(String query) {
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.getCount()>0)
            cursor.moveToFirst();
        HashMap gitaMap = new HashMap<Long, Track>();
        do{
            Track g = new Track(cursor);
            gitaMap.put(g.getId(), g);
        }while (cursor.moveToNext());
        cursor.close();
        return gitaMap;
    }


    public Cursor query(String query){
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        return cursor;
    }

    @Override
    public boolean update(ContentValues cv, long id) {

        return db.update(DB.TrackTable.TABLE_NAME,cv, DB.TrackTable._id+"='"+id+"'",null)>0;
    }

    @Override
    public boolean update(Track track, long id) {
        return false;
    }

    @Override
    public boolean delete(long id) {
        return false;
    }
}
