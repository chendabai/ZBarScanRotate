package com.dabai.zbarscanrotate.scan;

public interface ScreenOrientationChange {


    public final static int TOP = 1;
    public final static int BOTTOM = 2;
    public final static int LEFT = 3;
    public final static int RIGHT = 4;

    void change(int orientation);

}
