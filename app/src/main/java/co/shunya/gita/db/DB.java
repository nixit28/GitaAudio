package co.shunya.gita.db;

/**
 * Created by nixit on 8/18/14.
 */
public class DB {
    public static final String DB_Name = "gita.db";
    public static final int DB_Version = 11;
    public static final int ALBUM_GITA = 1;
    public static final int ALBUM_BHAJ = 2;

    public interface TrackTable {
        String TABLE_NAME = "Track";
        String title = "title";
        String url = "url";
        String lang = "lang";
        String album = "album";
        String downloded = "downloded";
        String _id = "_id";
        String trackNo = "trackNo";
        String CREATE_TABLE = "CREATE TABLE \"" +TABLE_NAME+"\" (\n" +
                _id + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                title + " TEXT,\n" +
                album + " INTEGER,\n" +
                url + " TEXT NOT NULL,\n" +
                downloded + " INTEGER DEFAULT (0),\n" +
                trackNo + " INTEGER DEFAULT (0),\n" +
                lang + " TEXT\n" + ");";
    }
    public interface Album{
        String TABLE_NAME = "Album";
        String name = "name";
        String _id = "_id";

        String CREATE_TABLE = "CREATE TABLE \"" +TABLE_NAME+"\" (\n" +
                _id + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                name + " TEXT\n" + ");";
    }
}
