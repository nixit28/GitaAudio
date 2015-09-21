package co.shunya.gita.view.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import co.shunya.gita.R;
import co.shunya.gita.db.DB;

public class DownloadListAdapter extends CursorAdapter {

    public DownloadListAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view.getTag() instanceof ViewHolder) {
            ViewHolder holder = (ViewHolder) view.getTag();
            String title = cursor.getString(cursor.getColumnIndex(DB.TrackTable.title));
            Long id = cursor.getLong(cursor.getColumnIndex(DB.TrackTable._id));
            holder.txtTitle.setText(title);
            holder.imgDelete.setTag(id);
            holder.imgDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
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
        holder.imgDelete = (ImageView) view.findViewById(R.id.menuList);
        holder.txtTitle = (TextView) view.findViewById(R.id.txtTitle);

        view.setTag(holder);
        return view;
    }

    private class ViewHolder {
        ImageView impPlayIndicator;
        ImageView imgDelete;
        TextView txtTitle;
    }
}
