package io.homo.superresolution.gui;

public class Rect {
    public int x;
    public int y;
    public int width;
    public int height;

    public Rect(int x, int y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean in(int x, int y){
        return x >= this.x && x < (this.x + this.width) && y >= this.y && y < (this.y + this.height);
    }

    public boolean in(double x, double y){
        return x >= this.x && x < (this.x + this.width) && y >= this.y && y < (this.y + this.height);
    }

    public int getLimitX() {
        return this.x + this.width;
    }

    public int getLimitY() {
        return this.y + this.height;
    }
    public int getCenterX() {
        return this.x + this.width / 2;
    }

    public int getCenterY() {
        return this.y + this.height / 2;
    }
}
