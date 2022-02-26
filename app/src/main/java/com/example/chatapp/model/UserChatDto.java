package com.example.chatapp.model;


public class UserChatDto {
    private String userid;
    private long time;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public UserChatDto(String userid, long time) {
        this.userid = userid;
        this.time = time;
    }

    public UserChatDto() {
    }

    @Override
    public String toString() {
        return "UserChatDto{" +
                "userid='" + userid + '\'' +
                ", sendDatetime=" + time +
                '}';
    }
}
