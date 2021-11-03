package edu.nd.crc.paperanalyticaldevices;

public class PADDataObject {

    private String padId;
    private String drugName;
    private String datetime;
    private String notes;
    private String drugOk;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getDrugOk() {
        return drugOk;
    }

    public void setDrugOk(String drugOk) {
        this.drugOk = drugOk;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
}
