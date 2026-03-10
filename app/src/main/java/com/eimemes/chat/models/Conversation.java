package com.eimemes.chat.models;

public class Conversation {
    private final String id, title;
    public Conversation(String id, String title) { this.id = id; this.title = title; }
    public String getId()    { return id; }
    public String getTitle() { return title != null && !title.isEmpty() ? title : "New conversation"; }
}
