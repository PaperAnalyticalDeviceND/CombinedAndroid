package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class FinishedListBaseAdapter extends BaseAdapter {

    //List adapter for the Upload Queue, displays PADDataObject models

    private static ArrayList<PADDataObject> resultItems;

    private final LayoutInflater mInflater;

    public FinishedListBaseAdapter(Context context, ArrayList<PADDataObject> results) {
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
        QueueListBaseAdapter.ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.donequeue_listview, null);
            holder = new QueueListBaseAdapter.ViewHolder();

            holder.drugName = convertView.findViewById(R.id.donequeue_drug);
            holder.datetime = convertView.findViewById(R.id.donequeue_time);
            holder.padId = convertView.findViewById(R.id.donepad_info);
            holder.image = convertView.findViewById(R.id.pad_donequeue_image);
            holder.predicted = convertView.findViewById(R.id.donepredicted_text);

            convertView.setTag(holder);
        } else {
            holder = (QueueListBaseAdapter.ViewHolder) convertView.getTag();
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

    }


}
