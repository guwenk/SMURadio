package com.github.guwenk.smuradio;

class Tracks {
    private int num;
    private String artist;
    private String title;
    private String duration;
    private String filename;

    String getFilename() {
        return filename;
    }

    void setFilename(String filename) {
        this.filename = filename;
    }

    String getArtist() {
        return artist;
    }

    void setArtist(String artist) {
        this.artist = artist;
    }

    String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    public String getDuration() {
        return duration;
    }

    void setDuration(String duration) {
        this.duration = duration;
    }

    public int getNum() {
        return num;
    }

    void setNum(int num) {
        this.num = num;
    }
}
