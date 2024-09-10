package edu.nd.crc.paperanalyticaldevices.api.utils;

import edu.nd.crc.paperanalyticaldevices.api.TasksList;
import retrofit2.Call;
import retrofit2.Response;

public interface ResponseCallBack {

    public void onResponse(Response<TasksList> response);
    public void onFailure(Throwable t);
}
