package com.bfourclass.euopendata.ExternalAPI;

public class Article extends GoogleNewsClient{
    private String title;
    private String content;

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }
}