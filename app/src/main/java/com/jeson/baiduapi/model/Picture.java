package com.jeson.baiduapi.model;

import java.io.Serializable;

/**
 * Created by 蒋凌轩 on 2016/5/1.
 */
public class Picture implements Serializable{

    private long time;
    private String title;
    private String img;

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
