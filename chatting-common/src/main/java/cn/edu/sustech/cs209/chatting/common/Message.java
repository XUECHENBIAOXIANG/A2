package cn.edu.sustech.cs209.chatting.common;

import com.google.gson.Gson;

import java.util.List;

public class Message {

    private Long timestamp;
    private Chat chat=new Chat();






    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    private String sentBy;
    private String mima;

    private String sendTo;

    public void setData(String data) {
        this.data = data;
    }

    private String data;

    private MessageType type;
    private List<String> allchat;
    private List<String> askchat;
    private List<String> people;

    public List<String> getPeople() {
        return people;
    }

    public void setPeople(List<String> people) {
        this.people = people;
    }

    public List<String> getAskchat() {
        return askchat;
    }

    public void setAskchat(List<String> askchat) {
        this.askchat = askchat;
    }

    public List<String> getAllchat() {
        return allchat;
    }

    public void setAllchat(List<String> allchat) {
        this.allchat = allchat;
    }

    public Message(Long timestamp, String sentBy, String mima, String sendTo, String data, MessageType type) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.mima = mima;
        this.sendTo = sendTo;
        this.data = data;
        this.type = type;
    }

    public MessageType getType() {
        return type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

    public String getMima() {
        return mima;
    }

    public static String toJson(Message message1) {
        Gson gson = new Gson();
        return gson.toJson(message1);
    }

    public static Message fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Message.class);
    }

}
