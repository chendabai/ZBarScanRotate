package com.dabai.zbarscanrotate.scan;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.dabai.zbarscanrotate.scan.ScreenOrientationChange;


public class ScreenSwitchUtils {

    private static final String TAG = ScreenSwitchUtils.class.getSimpleName();

    private volatile static ScreenSwitchUtils mInstance;

    private SensorManager sm;
    private OrientationSensorListener listener;
    private Sensor sensor;

    private SensorManager sm1;
    private Sensor sensor1;
    private ScreenOrientationChange screenOrientationChange;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 888:
                    int orientation = msg.arg1;
                    if (orientation > 45 && orientation < 135) {
                        Log.e(TAG, "左");
                        if (null != screenOrientationChange) {
                            screenOrientationChange.change(ScreenOrientationChange.LEFT);
                        }
                    } else if (orientation > 135 && orientation < 225) {
                        Log.e(TAG, "下");
                        if (null != screenOrientationChange) {
                            screenOrientationChange.change(ScreenOrientationChange.BOTTOM);
                        }
                    } else if (orientation > 225 && orientation < 315) {
                        Log.e(TAG, "右");
                        if (null != screenOrientationChange) {
                            screenOrientationChange.change(ScreenOrientationChange.RIGHT);
                        }

                    } else if ((orientation > 315 && orientation < 360) || (orientation > 0 && orientation < 45)) {
                        Log.e(TAG, "上");
                        if (null != screenOrientationChange) {
                            screenOrientationChange.change(ScreenOrientationChange.TOP);
                        }
                    }
                    break;
                default:
                    break;
            }

        }

        ;
    };

    private ScreenSwitchUtils(Context context) {
        Log.d(TAG, "init orientation listener.");
        // 注册重力感应器,监听屏幕旋转
        if (context instanceof ScreenOrientationChange && null != context) {
            screenOrientationChange = (ScreenOrientationChange) context;
        } else {
            new Throwable("未实现ScreenOrientationChange接口");

        }
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        listener = new OrientationSensorListener(mHandler);


    }

    /**
     * 返回ScreenSwitchUtils单例
     **/
    public static ScreenSwitchUtils init(Context context) {
        if (mInstance == null) {
            synchronized (ScreenSwitchUtils.class) {
                if (mInstance == null) {
                    mInstance = new ScreenSwitchUtils(context);
                }
            }
        }
        return mInstance;
    }

    /**
     * 开始监听
     */
    public void start(Activity activity) {
        Log.d(TAG, "start orientation listener.");
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * 停止监听
     */
    public void stop() {
        Log.d(TAG, "stop orientation listener.");
        sm.unregisterListener(listener);
    }

    public void destroy() {
        screenOrientationChange = null;
        mInstance = null;
    }


    /**
     * 重力感应监听者
     */
    public class OrientationSensorListener implements SensorEventListener {
        public static final int ORIENTATION_UNKNOWN = -1;
        private static final int _DATA_X = 0;
        private static final int _DATA_Y = 1;
        private static final int _DATA_Z = 2;
        private Handler rotateHandler;

        public OrientationSensorListener(Handler handler) {
            rotateHandler = handler;
        }

        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }

        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            int orientation = ORIENTATION_UNKNOWN;
            float X = -values[_DATA_X];
            float Y = -values[_DATA_Y];
            float Z = -values[_DATA_Z];
            float magnitude = X * X + Y * Y;
            // Don't trust the angle if the magnitude is small compared to the y
            // value
            if (magnitude * 4 >= Z * Z) {
                // 屏幕旋转时
                float OneEightyOverPi = 57.29577957855f;
                float angle = (float) Math.atan2(-Y, X) * OneEightyOverPi;
                orientation = 90 - (int) Math.round(angle);
                // normalize to 0 - 359 range
                while (orientation >= 360) {
                    orientation -= 360;
                }
                while (orientation < 0) {
                    orientation += 360;
                }
            }
            if (rotateHandler != null) {
                rotateHandler.obtainMessage(888, orientation, 0).sendToTarget();
            }
        }
    }


}
