package cn.edu.sustech.cs209.chatting.common;

import com.google.gson.Gson;

public class Message {

    private Long timestamp;
    private Chat chat=new Chat();

    public Chat getChat() {
        return chat;
    }

    private String sentBy;
    private String mima;

    private String sendTo;

    private String data;

    private MessageType type;



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
