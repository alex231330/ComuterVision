package com.example.alex231330.compvision.libs;

public class CameraData {
    private byte[] data;
    private int dataWidth, dataHeight;
    private int width, height, dataRotation;

    CameraData(int width, int height, int dataRotation) {
        this.data = null;
        this.width = dataWidth = width;
        this.height = dataHeight = height;
        this.dataRotation = dataRotation;
        if (dataRotation % 180 != 0) {
            this.width = height;
            this.height = width;
        }
    }

    void setData(byte[] data) {
        this.data = data;
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public int getColor(int x, int y) {
        if (data == null) throw new IllegalStateException("No data array is available");
        int nx, ny;
        switch (dataRotation) {
            case 0: nx = x; ny = y; break;
            case 90: nx = y; ny = width - 1 - x; break;
            case 180: nx = width - 1 - x; ny = height - 1 - y; break;
            case 270: nx = height - 1 - y; ny = x; break;
            default: throw new IllegalStateException("Invalid dataRotation");
        }
        return CameraSurface.getColor(data, nx, ny, dataWidth, dataHeight);
    }
}

