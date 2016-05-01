package com.jeson.baiduapi.model;

import java.io.Serializable;

/**
 * Created by 蒋凌轩 on 2016/4/30.
 */
public class Joke implements Serializable{

    private String title;
    private String text;
    private String ct;
    private int type;

    public String getCt() {
        return ct;
    }

    public void setCt(String ct) {
        this.ct = ct;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
