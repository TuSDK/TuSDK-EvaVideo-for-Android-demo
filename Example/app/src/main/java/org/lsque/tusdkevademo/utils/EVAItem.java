package org.lsque.tusdkevademo.utils;

/**
 * TuSDK
 * $
 *
 * @author H.ys
 * @Date $ $
 * @Copyright (c) 2019 tusdk.com. All rights reserved.
 */
public class EVAItem {
    /**
     * nm : 鼠年吉祥
     * id : 140
     */

    private String nm;
    private int id;
    private String vid;

    public EVAItem(String nm, int id, String vid) {
        this.nm = nm;
        this.id = id;
        this.vid = vid;
    }

    public String getNm() {
        return nm;
    }

    public void setNm(String nm) {
        this.nm = nm;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;}

    public String getVid() {
        return vid;
    }
}
