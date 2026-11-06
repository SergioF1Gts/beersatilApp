package com.example.beersatil.model;

public class TaskItem {
    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private double latitude;
    private double longitude;
    private String address;
    private String authorEmail;
    private String authorUid;
    private String role;
    private String type;     // Revisión de colmena, Alimentación, Cosecha de miel, Plaga/Incidente, Tratamiento sanitario
    private String status;   // Pendiente, En proceso, Resuelta
    private String apiary;   // Colmena o apiario opcional
    private long timestamp;

    public TaskItem() {
        // Required for Firestore
    }

    public TaskItem(String id, String title, String description, String imageUrl, double latitude, double longitude, String address, String authorEmail, String authorUid, String role, String type, String status, String apiary, long timestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.authorEmail = authorEmail;
        this.authorUid = authorUid;
        this.role = role;
        this.type = type != null ? type : "Revisión de colmena";
        this.status = status != null ? status : "Pendiente";
        this.apiary = apiary != null ? apiary : "";
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public String getAuthorUid() { return authorUid; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getType() { return type != null ? type : "Revisión de colmena"; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status != null ? status : "Pendiente"; }
    public void setStatus(String status) { this.status = status; }

    public String getApiary() { return apiary != null ? apiary : ""; }
    public void setApiary(String apiary) { this.apiary = apiary; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
