package co.shunya.gita.utils.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import co.shunya.gita.db.DatabaseHelper;

/**
 * Created by nixit on 8/19/14.
 */
public class DBUtils {

    public synchronized static SQLiteDatabase getWriteableDatabase(Context context){
        SQLiteDatabase sqLiteDatabase = new DatabaseHelper(context).getWritableDatabase();
        return sqLiteDatabase;
    }
}
