package com.eimemes.chat.models;

public class Message {
    public static final String ROLE_USER      = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_TYPING    = "typing";

    private String  role, content, time, model;
    private boolean disclaimer;

    public Message(String role, String content, String time) {
        this.role = role; this.content = content; this.time = time;
    }

    public String  getRole()       { return role; }
    public String  getContent()    { return content; }
    public String  getTime()       { return time; }
    public String  getModel()      { return model; }
    public boolean isDisclaimer()  { return disclaimer; }

    public void setRole(String r)        { this.role = r; }
    public void setContent(String c)     { this.content = c; }
    public void setModel(String m)       { this.model = m; }
    public void setDisclaimer(boolean d) { this.disclaimer = d; }
}
