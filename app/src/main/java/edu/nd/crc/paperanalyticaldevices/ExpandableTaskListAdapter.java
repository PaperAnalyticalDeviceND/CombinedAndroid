package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

public class ExpandableTaskListAdapter extends BaseExpandableListAdapter {


    private static List<String> tasks;

    private HashMap<String, List<String>> expandableDetailList;

    private Context context;


    public ExpandableTaskListAdapter(Context context, List<String> expandableListTitle, HashMap<String, List<String>> expandableDetailList){
        this.context = context;
        tasks = expandableListTitle;

        this.expandableDetailList = expandableDetailList;

    }

    @Override
    public int getGroupCount() {
        return tasks.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.expandableDetailList.get(tasks.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return tasks.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.expandableDetailList.get(tasks.get(groupPosition)).get(childPosition);
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
}
