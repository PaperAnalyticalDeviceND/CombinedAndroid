package edu.nd.crc.paperanalyticaldevices;

public class PADDataObject {
    //data model to display in a multi-line ListView row
    private String padId;
    private String drugName;
    private String datetime;
    private String project;

    public String getPadId() {
        return padId;
    }

    public void setPadId(String padId) {
        this.padId = padId;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
}
