package com.example.chatapp.model;

public class User {
    private String id;
    private String username;
    private String imageURL;
    private String status;
    private String search;
    private String typingTo;
    private String blocked;
    private String email;

    public User() {
    }

    public User(String id, String username, String imageURL, String status, String search, String typingTo, String blocked, String email) {
        this.id = id;
        this.username = username;
        this.imageURL = imageURL;
        this.status = status;
        this.search = search;
        this.typingTo = typingTo;
        this.blocked = blocked;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getTypingTo() {
        return typingTo;
    }

    public void setTypingTo(String typingTo) {
        this.typingTo = typingTo;
    }

    public String getBlocked() {
        return blocked;
    }

    public void setBlocked(String blocked) {
        this.blocked = blocked;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", imageURL='" + imageURL + '\'' +
                ", status='" + status + '\'' +
                ", search='" + search + '\'' +
                ", typingTo='" + typingTo + '\'' +
                ", blocked='" + blocked + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
