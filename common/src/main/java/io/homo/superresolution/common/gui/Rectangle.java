package io.homo.superresolution.common.gui;

public class Rectangle {
    public int x;
    public int y;
    public int width;
    public int height;

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle() {
        this(0, 0, 0, 0);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.reshape(x, y, width, height);
    }

    public void setBounds(Rectangle rectangle) {
        this.reshape(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    public Rectangle intersection(Rectangle var1) {
        int var2 = this.x;
        int var3 = this.y;
        int var4 = var1.x;
        int var5 = var1.y;
        long var6;
        var6 = var2 + (long) this.width;
        long var8;
        var8 = var3 + (long) this.height;
        long var10;
        var10 = var4 + (long) var1.width;
        long var12;
        var12 = var5 + (long) var1.height;
        if (var2 < var4) {
            var2 = var4;
        }

        if (var3 < var5) {
            var3 = var5;
        }

        if (var6 > var10) {
            var6 = var10;
        }

        if (var8 > var12) {
            var8 = var12;
        }

        var6 -= var2;
        var8 -= var3;
        if (var6 < -2147483648L) {
            var6 = -2147483648L;
        }

        if (var8 < -2147483648L) {
            var8 = -2147483648L;
        }

        return new Rectangle(var2, var3, (int) var6, (int) var8);
    }

    public void reshape(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }


    public boolean intersects(Rectangle var1) {
        int var2 = this.width;
        int var3 = this.height;
        int var4 = var1.width;
        int var5 = var1.height;
        if (var4 > 0 && var5 > 0 && var2 > 0 && var3 > 0) {
            int var6 = this.x;
            int var7 = this.y;
            int var8 = var1.x;
            int var9 = var1.y;
            var4 += var8;
            var5 += var9;
            var2 += var6;
            var3 += var7;
            return (var4 < var8 || var4 > var6) && (var5 < var9 || var5 > var7) && (var2 < var6 || var2 > var8) && (var3 < var7 || var3 > var9);
        } else {
            return false;
        }
    }

    public boolean in(int x, int y) {
        return x >= this.x && x < (this.x + this.width) && y >= this.y && y < (this.y + this.height);
    }

    public boolean in(double x, double y) {
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

    public Rectangle clone() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    public boolean isEmpty() {
        return this.width <= 0 || this.height <= 0;
    }
}
