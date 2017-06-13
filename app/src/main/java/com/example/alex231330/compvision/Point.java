package com.example.alex231330.compvision;

/**
 * Created by alex231330 on 03.06.17.
 */

public class Point {
    int x;
    int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {

        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getValue(){
        return (x + y);
    }
}
