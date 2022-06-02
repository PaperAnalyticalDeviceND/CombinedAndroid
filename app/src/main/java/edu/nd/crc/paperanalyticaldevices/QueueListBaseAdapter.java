package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.io.File;

public class QueueListBaseAdapter extends BaseAdapter {

    //List adapter for the Upload Queue, displays PADDataObject models

    private static ArrayList<PADDataObject> resultItems;

    private final LayoutInflater mInflater;

    public QueueListBaseAdapter(Context context, ArrayList<PADDataObject> results) {
        resultItems = results;
        mInflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return resultItems.size();
    }

    public Object getItem(int position) {
        return resultItems.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.queue_listview, null);
            holder = new ViewHolder();

            holder.drugName = convertView.findViewById(R.id.queue_drug);
            holder.datetime = convertView.findViewById(R.id.queue_time);
            holder.padId = convertView.findViewById(R.id.pad_info);
            holder.image = convertView.findViewById(R.id.pad_queue_image);
            holder.predicted = convertView.findViewById(R.id.predicted_text);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.drugName.setText(resultItems.get(position).getDrugName());
        holder.datetime.setText(resultItems.get(position).getDatetime());
        holder.padId.setText(resultItems.get(position).getPadId());
        holder.predicted.setText(resultItems.get(position).getPredicted());
        //holder.image.setImageURI(Uri.fromFile(new File(resultItems.get(position).getImageFile())));
        holder.image.setImageURI(Uri.parse(resultItems.get(position).getImageFile()));

        return convertView;
    }

    static class ViewHolder {
        TextView datetime;
        TextView drugName;
        TextView padId;
        TextView predicted;
        ImageView image;

    }

}
