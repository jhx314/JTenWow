package com.jeson.baiduapi.model;

import java.io.Serializable;

/**
 * Created by 蒋凌轩 on 2016/4/30.
 */
public class News implements Serializable {

    private String time;
    private String title;
    private String picUrl;
    private String url;

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
