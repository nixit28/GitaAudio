package co.shunya.gita.view.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import co.shunya.gita.DownloadService;
import co.shunya.gita.R;
import co.shunya.gita.db.DB;

public class AdhyayListAdapter extends CursorAdapter implements View.OnClickListener{

    public AdhyayListAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view.getTag() instanceof ViewHolder) {
            ViewHolder holder = (ViewHolder) view.getTag();
            String title = cursor.getString(cursor.getColumnIndex(DB.TrackTable.title));
            Long id = cursor.getLong(cursor.getColumnIndex(DB.TrackTable._id));
            holder.txtTitle.setText(title);
            holder.imgMenu.setTag(id);
            holder.imgMenu.setOnClickListener(this);
//          TODO  current player setup
//           if (CurrentDataHolder.getInstant().getId() == id) {
//                holder.impPlayIndicator.setVisibility(View.VISIBLE);
//            } else {
//                holder.impPlayIndicator.setVisibility(View.INVISIBLE);
//            }
        }
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.view_adhyay_list_item, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.impPlayIndicator = (ImageView) view.findViewById(R.id.imgPlayIndicator);
        holder.imgMenu= (ImageView) view.findViewById(R.id.menuList);
        holder.txtTitle = (TextView) view.findViewById(R.id.txtTitle);

        view.setTag(holder);
        return view;
    }

    @Override
    public void onClick(final View view) {
        view.post(new Runnable() {
            @Override
            public void run() {
                showPopupMenu(view);
            }
        });
    }

    private void showPopupMenu(View view) {
        // Retrieve the clicked item from view's tag
        final Long id = (Long) view.getTag();

        // Create a PopupMenu, giving it the clicked view for an anchor
        PopupMenu popup = new PopupMenu(mContext, view);

        // Inflate our menu resource into the PopupMenu's Menu
        popup.getMenuInflater().inflate(R.menu.menu_list, popup.getMenu());

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_download:
                        Intent intent = new Intent(mContext, DownloadService.class);
                        intent.setAction(DownloadService.ACTION_DOWNLOAD);
                        intent.putExtra(DownloadService.INTENT_DOWNLOAD_ID_EXTRA, id);
                        mContext.startService(intent);
                        Toast.makeText(mContext, "Downloading Track :"+id,Toast.LENGTH_LONG).show();
                        return true;
                }
                return false;
            }
        });

        // Finally show the PopupMenu
        popup.show();
    }

    private class ViewHolder {
        ImageView impPlayIndicator;
        ImageView imgMenu;
        TextView txtTitle;
    }
}
