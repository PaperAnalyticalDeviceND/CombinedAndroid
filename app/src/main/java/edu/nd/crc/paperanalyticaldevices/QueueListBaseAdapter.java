package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class QueueListBaseAdapter extends BaseAdapter {

    //List adapter for the Upload Queue, displays PADDataObject models

    private static ArrayList<PADDataObject> resultItems;

    private LayoutInflater mInflater;

    public QueueListBaseAdapter(Context context, ArrayList<PADDataObject> results){
        resultItems = results;
        mInflater = LayoutInflater.from(context);
    }

    public int getCount(){
        return resultItems.size();
    }

    public Object getItem(int position){
        return resultItems.get(position);
    }

    public long getItemId(int position){
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent){
        ViewHolder holder;

        if(convertView == null){
            convertView = mInflater.inflate(R.layout.queue_listview, null);
            holder = new ViewHolder();

            holder.drugName = (TextView) convertView.findViewById(R.id.queue_drug);
            holder.datetime = (TextView) convertView.findViewById(R.id.queue_time);
            holder.padId = (TextView) convertView.findViewById(R.id.pad_info);

            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }

        holder.drugName.setText(resultItems.get(position).getDrugName());
        holder.datetime.setText(resultItems.get(position).getDatetime());
        holder.padId.setText(resultItems.get(position).getPadId());

        return convertView;
    }

    static class ViewHolder {
        TextView datetime;
        TextView drugName;
        TextView padId;

    }

}
