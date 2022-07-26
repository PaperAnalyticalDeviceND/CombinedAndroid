package edu.nd.crc.paperanalyticaldevices;

public class ArtifactsTaskObject {

    private Integer id;
    private String sampleId;
    private String drug;
    private String manufacturer;
    private String dosage;

    //public String toString(){
        //return sampleId;
    //}

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setDrug(String drug) {
        this.drug = drug;
    }

    public String getDrug() {
        return drug;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDosage() {
        return dosage;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
}
