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

import android.content.Context;
import android.util.Log;

import co.shunya.gita.db.DB;
import co.shunya.gita.db.TrackDao;
import co.shunya.gita.db.model.Track;

import java.util.HashMap;

/**
 * Retrieves and organizes media to play. Before being used, you must call {@link #prepare()},
 * which will retrieve all of the music on the user's device (by performing a query on a content
 * resolver). After that, it's ready to retrieve a random song, with its title and URI, upon
 * request.
 */
public class MusicRetriever {

    public static int mAlbumId;
    private static final String TAG = "MusicRetriever";
    private final Context mContext;
    public static MusicRetriever musicRetriever;
    private static String langName = "";

    private HashMap<Long, Track> gitaMap;

    private MusicRetriever(Context c, String langName, int albumId) {
        mContext = c;
        this.langName = langName;
        this.mAlbumId = albumId;
    }

    public static final MusicRetriever getInstance(Context c, String lName, int albumId) {
        if (!langName.equals(lName) || albumId!=mAlbumId || musicRetriever == null) {
            musicRetriever = new MusicRetriever(c, lName, albumId);
        }
        return musicRetriever;
    }

    /**
     * Loads music data. This method may take long, so be sure to call it asynchronously without
     * blocking the main thread.
     */
    public void prepare() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Select * from ");
        stringBuilder.append(DB.TrackTable.TABLE_NAME);
        stringBuilder.append(" where ");
        stringBuilder.append(DB.TrackTable.lang);
        stringBuilder.append(" = '");
        stringBuilder.append(langName);
        stringBuilder.append("' and ");
        stringBuilder.append(DB.TrackTable.album);
        stringBuilder.append(" = ");
        stringBuilder.append(mAlbumId);
        stringBuilder.append(" order by ");
        stringBuilder.append(DB.TrackTable.trackNo);
        stringBuilder.append(";");
        String query = stringBuilder.toString();
        Log.i(MusicRetriever.class.getSimpleName(),"Query: "+query);
        TrackDao gitaDao = new TrackDao(mContext);
        gitaMap = gitaDao.selectHash(query);
    }

    public Track getItem(long id) {
        if (gitaMap != null) {
            if (gitaMap.containsKey(id))
                return gitaMap.get(id);
        }
        return null;
    }
}
