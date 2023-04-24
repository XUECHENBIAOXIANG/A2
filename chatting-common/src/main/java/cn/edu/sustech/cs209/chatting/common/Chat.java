package cn.edu.sustech.cs209.chatting.common;

public class Chat {
    private Long timestamp;
    private Long id;
    private String[] talk;
    public String[] a;
    public String[] b;

    public String[] getA() {
        return a;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String[] getTalk() {
        return talk;
    }

    public void setTalk(String[] talk) {
        this.talk = talk;
    }

    public String[] getB() {
        return b;
    }


    public void setA(String[] a) {
        this.a = a;
    }

    public void setB(String[] b) {
        this.b = b;
    }
}
