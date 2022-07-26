package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import edu.nd.crc.paperanalyticaldevices.api.TasksList;

public class TaskListBaseAdapter extends BaseAdapter implements Filterable {

    private static ArrayList<ArtifactsTaskObject> tasks;

    private ArrayList<ArtifactsTaskObject> filteredTasks;

    private TaskFilter taskFilter;

    private final LayoutInflater mInflater;

    //Context context;

    public TaskListBaseAdapter(Context context, ArrayList<ArtifactsTaskObject> tasklist){
        tasks = tasklist;
        filteredTasks = tasklist;
        mInflater = LayoutInflater.from(context);
        //this.context = context;

        getFilter();
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
        //ArtifactsTaskObject obj = tasks.get(position);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.tasks_listview, null);
            holder = new TaskListBaseAdapter.ViewHolder();

            holder.drug = convertView.findViewById(R.id.sample_drug_name);
            holder.sampleId = convertView.findViewById(R.id.sample_id);
            //holder.testButton = (Button) convertView.findViewById(R.id.task_test_button);


            convertView.setTag(holder);
        } else {
            holder = (TaskListBaseAdapter.ViewHolder) convertView.getTag();
        }

        holder.sampleId.setText(tasks.get(position).getSampleId());
        holder.drug.setText(tasks.get(position).getDrug());

/*
        holder.testButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //testTask(obj.getSampleId());
                Log.d("ARTIFACTS", obj.getSampleId());
            }
        });
*/
        return convertView;
    }

    @Override
    public Filter getFilter() {
        if(taskFilter == null){
            taskFilter = new TaskFilter();
        }

        return taskFilter;
    }

    static class ViewHolder {
        TextView sampleId;
        TextView manufacturer;
        TextView dosage;
        TextView drug;
        //Button testButton;
    }

    private class TaskFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            FilterResults filterResults = new FilterResults();
            if(constraint != null && constraint.length() > 0){
                ArrayList<ArtifactsTaskObject> tempList = new ArrayList<ArtifactsTaskObject>();

                for(ArtifactsTaskObject task: tasks){
                    if(task.getSampleId().toLowerCase().contains(constraint.toString().toLowerCase())){
                        tempList.add(task);
                    }
                }

                filterResults.count = tempList.size();
                filterResults.values = tempList;

            }else{
                filterResults.count = tasks.size();
                filterResults.values = tasks;
            }

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredTasks = (ArrayList<ArtifactsTaskObject>)  results.values;
            notifyDataSetChanged();
        }
    }
}
