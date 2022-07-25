package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import edu.nd.crc.paperanalyticaldevices.api.TasksList;

public class TaskListBaseAdapter extends BaseAdapter {

    private static ArrayList<ArtifactsTaskObject> tasks;

    private final LayoutInflater mInflater;

    public TaskListBaseAdapter(Context context, ArrayList<ArtifactsTaskObject> tasklist){
        tasks = tasklist;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public Object getItem(int position) {
        return tasks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TaskListBaseAdapter.ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.tasks_listview, null);
            holder = new TaskListBaseAdapter.ViewHolder();

            holder.drug = convertView.findViewById(R.id.sample_drug_name);
            holder.sampleId = convertView.findViewById(R.id.sample_id);



            convertView.setTag(holder);
        } else {
            holder = (TaskListBaseAdapter.ViewHolder) convertView.getTag();
        }

        holder.sampleId.setText(tasks.get(position).getSampleId());
        holder.drug.setText(tasks.get(position).getDrug());


        return convertView;
    }

    static class ViewHolder {
        TextView sampleId;
        TextView manufacturer;
        TextView dosage;
        TextView drug;
    }
}
