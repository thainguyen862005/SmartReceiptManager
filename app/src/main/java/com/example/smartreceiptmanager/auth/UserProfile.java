package com.example.smartreceiptmanager.auth;

/**
 * Model đại diện cho thông tin user lưu trên Firestore.
 * Collection path: users/{uid}
 */
public class UserProfile {
    private String uid;
    private String email;
    private String displayName;
    private String photoUrl;
    private long createdAt;
    private long updatedAt;

    // Firestore yêu cầu constructor không tham số
    public UserProfile() {}

    public UserProfile(String uid, String email, String displayName, String photoUrl) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName != null ? displayName : "";
        this.photoUrl = photoUrl != null ? photoUrl : "";
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
