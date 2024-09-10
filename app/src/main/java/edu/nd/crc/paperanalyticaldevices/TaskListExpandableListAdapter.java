package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TaskListExpandableListAdapter extends BaseExpandableListAdapter implements Filterable {

    private static List<String> tasks;

    private List<String> filteredTasks;

    private HashMap<String, List<String>> expandableDetailList;

    private TasksFilter taskFilter;

    private Context context;

    //private final LayoutInflater mInflater;
    static class ViewHolder {
        TextView sampleId;
        TextView manufacturer;
        TextView dosage;
        TextView drug;
        TextView taskId;
    }

    public TaskListExpandableListAdapter(Context context, List<String> expandableListTitle, HashMap<String, List<String>> expandableDetailList){
        this.context = context;
        tasks = expandableListTitle;
        filteredTasks = expandableListTitle;
        this.expandableDetailList = expandableDetailList;

        getFilter();
    }

    @Override
    public int getGroupCount() {
        return filteredTasks.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.expandableDetailList.get(filteredTasks.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return filteredTasks.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.expandableDetailList.get(filteredTasks.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String listTitle = (String) getGroup(groupPosition);
        if(convertView == null){
            LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.task_list_row_group, null);
        }
        TextView titleTextView = (TextView) convertView.findViewById(R.id.expandable_sampleid);
        titleTextView.setTypeface(null, Typeface.BOLD);
        titleTextView.setText(listTitle);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        final String taskId = (String) getChild(groupPosition, childPosition);
        if(convertView == null){
            LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.task_list_row_child, null);
        }
        TextView detailTaskId = (TextView) convertView.findViewById(R.id.expandable_taskid);
        detailTaskId.setText(taskId);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public Filter getFilter() {
        if(taskFilter == null){
            taskFilter = new TasksFilter();
        }

        return taskFilter;
    }

    private class TasksFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            FilterResults filterResults = new FilterResults();
            if(constraint != null && constraint.length() > 0){
                ArrayList<String> tempList = new ArrayList<String>();

                for(String task: tasks){
                    if(task.toLowerCase().contains(constraint.toString().toLowerCase())){
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
            filteredTasks = (ArrayList<String>)  results.values;
            notifyDataSetChanged();
        }
    }
}
