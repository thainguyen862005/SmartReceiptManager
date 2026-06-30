package com.example.smartreceiptmanager.auth;

/**
 * Model đại diện cho thông tin user lưu trên Realtime Database.
 */
public class UserProfile {
    private String email;
    private String phone_number;
    private String facebook_id;
    private String google_id;
    private long created_at;
    private Profile profile;

    // Constructor rỗng bắt buộc cho Firebase
    public UserProfile() {
        this.profile = new Profile();
    }

    public UserProfile(String email, String phone_number, String facebook_id, String google_id, Profile profile) {
        this.email = email;
        this.phone_number = phone_number != null ? phone_number : "";
        this.facebook_id = facebook_id != null ? facebook_id : "";
        this.google_id = google_id != null ? google_id : "";
        this.created_at = System.currentTimeMillis();
        this.profile = profile != null ? profile : new Profile();
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone_number() { return phone_number; }
    public void setPhone_number(String phone_number) { this.phone_number = phone_number; }

    public String getFacebook_id() { return facebook_id; }
    public void setFacebook_id(String facebook_id) { this.facebook_id = facebook_id; }

    public String getGoogle_id() { return google_id; }
    public void setGoogle_id(String google_id) { this.google_id = google_id; }

    public long getCreated_at() { return created_at; }
    public void setCreated_at(long created_at) { this.created_at = created_at; }

    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }

    public static class Profile {
        private String full_name;
        private String dob;
        private String gender;
        private String address;
        private String nationality;
        private String marital_status;
        private String avatar_url;

        // Constructor rỗng bắt buộc cho Firebase
        public Profile() {}

        public Profile(String full_name, String avatar_url) {
            this.full_name = full_name != null ? full_name : "";
            this.avatar_url = avatar_url != null ? avatar_url : "";
            this.dob = "";
            this.gender = "";
            this.address = "";
            this.nationality = "";
            this.marital_status = "";
        }

        // Getters and Setters
        public String getFull_name() { return full_name; }
        public void setFull_name(String full_name) { this.full_name = full_name; }

        public String getDob() { return dob; }
        public void setDob(String dob) { this.dob = dob; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getNationality() { return nationality; }
        public void setNationality(String nationality) { this.nationality = nationality; }

        public String getMarital_status() { return marital_status; }
        public void setMarital_status(String marital_status) { this.marital_status = marital_status; }

        public String getAvatar_url() { return avatar_url; }
        public void setAvatar_url(String avatar_url) { this.avatar_url = avatar_url; }
    }
}