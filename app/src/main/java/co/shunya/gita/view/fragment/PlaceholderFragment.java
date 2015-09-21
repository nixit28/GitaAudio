package co.shunya.gita.view.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import co.shunya.gita.BusProvider;
import co.shunya.gita.DownloadService;
import co.shunya.gita.R;
import co.shunya.gita.db.DB;
import co.shunya.gita.db.TrackDao;
import co.shunya.gita.db.model.Track;
import co.shunya.gita.view.activity.MainActivity;
import co.shunya.gita.view.adapter.AdhyayListAdapter;
import co.shunya.gita.view.adapter.DownloadListAdapter;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaceholderFragment extends ListFragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String ARG_ALBUM_ID = "album_id";
    private TrackDao trackDao;
    private List<ListItemClickListener> list;
    private CursorAdapter adapter;
    private String mQuery;
    private String mName;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PlaceholderFragment newInstance(int sectionNumber, int albumID) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putInt(ARG_ALBUM_ID, albumID);
        fragment.setArguments(args);
        return fragment;
    }

    public PlaceholderFragment() {
        list = new ArrayList<ListItemClickListener>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] array = getResources().getStringArray(R.array.array_list_item);
        int position = getArguments().getInt(ARG_SECTION_NUMBER);
        mName = array[position];
        int albumId = getArguments().getInt(ARG_ALBUM_ID);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Select * from ");
        stringBuilder.append(DB.TrackTable.TABLE_NAME);
        stringBuilder.append(" where ");
        if (mName.equals("Downloaded")) {
            stringBuilder.append(DB.TrackTable.downloded);
            stringBuilder.append(" = '");
            stringBuilder.append(1 + "'");
        } else {
            stringBuilder.append(DB.TrackTable.lang);
            stringBuilder.append(" = '");
            stringBuilder.append(mName);
            stringBuilder.append("' and ");
            stringBuilder.append(DB.TrackTable.album);
            stringBuilder.append(" = ");
            stringBuilder.append(albumId);
            stringBuilder.append(" order by ");
            stringBuilder.append(DB.TrackTable.trackNo);
        }
        stringBuilder.append(";");
        mQuery = stringBuilder.toString();
        trackDao = new TrackDao(getActivity());
        Cursor cursor = trackDao.query(mQuery);
        if (mName.equals("Downloaded")) {
            adapter = new DownloadListAdapter(getActivity(), cursor);
            setListAdapter(adapter);
        } else {
            adapter = new AdhyayListAdapter(getActivity(), cursor);
            setListAdapter(adapter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        trackDao.close();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }


    @Override
    public void onPause() {
        super.onDestroy();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mName.equals("Downloaded")) {
            Track t = trackDao.select(id);
            Toast.makeText(getActivity(), "File will be delete: " + t.getTitle(), Toast.LENGTH_LONG).show();
            File file = t.getLocalFile(getActivity());
            if (file.exists()) {
                boolean a = file.delete();
                Log.i(PlaceholderFragment.class.getSimpleName(), "File is deleted from the SD Card" + a);
                t.setDownloded(false);
                trackDao.update(t.getDefaultContentValues(), t.getId());
                onFileDeleted();
            }else{
                Toast.makeText(getActivity(), "File not exist: " + t.getTitle(), Toast.LENGTH_LONG).show();
            }
        } else {
            notifySubscribers(trackDao.select(id));
        }
    }


    public void onFileDeleted() {
        updateList();
    }

    private void updateList() {
        if (mName.equals("Downloaded")) {
            Cursor cursor = trackDao.query(mQuery);
            adapter.swapCursor(cursor);
            adapter.notifyDataSetChanged();
        }
    }

    @Subscribe
    public void onFileDownloaded(DownloadService.DownloadUpdateClass downloadUpdateClass) {
        updateList();
    }

    public void addListClickSubscriber(ListItemClickListener listItemClickListener) {
        list.add(listItemClickListener);
    }

    private void notifySubscribers(Track track) {
        for (ListItemClickListener listener : list) {
            listener.onClick(track);
        }
    }

    public interface ListItemClickListener {
        public void onClick(Track track);
    }
}
