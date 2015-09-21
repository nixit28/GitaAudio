package co.shunya.gita.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by nixit on 8/18/14.
 */
public class DatabaseHelper extends SQLiteOpenHelper{



    public DatabaseHelper(Context context) {
        super(context, DB.DB_Name, null, DB.DB_Version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.beginTransaction();
            db.execSQL(DB.TrackTable.CREATE_TABLE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(newVersion > 5){
            db.execSQL("Drop table if exists Track;");
            onCreate(db);
        }
    }
}
