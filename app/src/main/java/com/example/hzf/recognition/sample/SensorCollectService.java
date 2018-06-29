package com.example.hzf.recognition.sample;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;

import com.example.hzf.recognition.R;
import com.example.hzf.recognition.classifier.SVM;
import com.example.hzf.recognition.common.FileOperateUtil;
import com.example.hzf.recognition.common.LogUtil;
import com.example.hzf.recognition.common.MediaPlayerManager;
import com.example.hzf.recognition.common.MyApplication;
import com.example.hzf.recognition.model.Accelerometer;
import com.example.hzf.recognition.model.Gyroscope;
import com.example.hzf.recognition.model.Magnetometer;

import java.util.ArrayList;

import static com.example.hzf.recognition.CollectActivity.DELAY;

/**
 * 采集训练数据
 */
public class SensorCollectService extends Service implements SensorEventListener {

    private SensorManager mSensorManager;

    private Sensor mSensorAcc;
    private Sensor mSensorGyro;
    private Sensor mSensorMag;

    private ArrayList<Accelerometer> accList = new ArrayList<>();
    private ArrayList<Gyroscope> gyroList = new ArrayList<>();
    private ArrayList<Magnetometer> magList = new ArrayList<>();

    private PowerManager.WakeLock mWakeLock;
    private PowerManager powerManager;

    //延迟采样的微秒数
    public final int sampleDelay = 20000;

    //5秒采集到的数据量
    public final int five_seconds_data_length = 5 * 50;

    //一次采集的数据量
    private int data_length;

    //是否关闭服务
    private boolean isDestroyed = false;

    private int label = 0;
    private int id = 0;

    private MediaPlayer mp;

    public SensorCollectService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //获取传感器管理对象
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        //获取加速度传感器
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensorAcc, sampleDelay);

        //陀螺仪和磁力计
        mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mSensorGyro, sampleDelay);

        mSensorMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, mSensorMag, sampleDelay);

        //电源管理
        powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorCollectLock");
        mWakeLock.acquire();

        //播放提示音
        SVM.exec.execute(new Runnable() {
            @Override
            public void run() {
                Vibrator vibrator = (Vibrator)MyApplication.getContext().getSystemService(MyApplication.getContext().VIBRATOR_SERVICE);
                vibrator.vibrate(5000);
            }
        });
        SVM.exec.execute(new Runnable() {
            @Override
            public void run() {
                mp = MediaPlayer.create(MyApplication.getContext(), R.raw.alarm);
                mp.start();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d(SensorCollectService.class.getSimpleName(), "onStartCommand");
        if(intent != null && label == 0 && id == 0){
            label = intent.getIntExtra("label", 0);
            LogUtil.d(SensorCollectService.class.getSimpleName(), "label = " + label);
            id = intent.getIntExtra("id", 0);
            LogUtil.d(SensorCollectService.class.getSimpleName(), "id = " + id);
            //上下楼梯采80秒，其他采3分钟
            if(label == 4 || label == 5){
                data_length = 80 * 50;
            }else{
                data_length = 3 * 60 * 50;
            }
        }

        if(mSensorManager != null) {
            mWakeLock.release();
            mSensorManager.unregisterListener(this);

            mSensorManager.registerListener(this, mSensorAcc, sampleDelay);
            mSensorManager.registerListener(this, mSensorGyro, sampleDelay);
            mSensorManager.registerListener(this, mSensorMag, sampleDelay);

            mWakeLock.acquire();
        }

        if (Math.min(Math.min(accList.size(), gyroList.size()), magList.size()) + five_seconds_data_length * 1.1 < data_length) {
            //定时任务
            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            long triggerAtTime = SystemClock.elapsedRealtime() + 5000;
            Intent i = new Intent(this, SensorCollectService.class);
            PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
            manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //取消监听加速度传感器
        mSensorManager.unregisterListener(this);
        mWakeLock.release();

        mp.release();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;

        //当前时间
        long time = System.currentTimeMillis();

        if(Math.min(Math.min(accList.size(), gyroList.size()), magList.size())  >= data_length){

            if(isDestroyed == false) {
                isDestroyed = true;
                FileOperateUtil.printSensorData(label, id, accList, gyroList, magList);
                //播放提示音
                SVM.exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        Vibrator vibrator = (Vibrator)MyApplication.getContext().getSystemService(MyApplication.getContext().VIBRATOR_SERVICE);
                        vibrator.vibrate(5000);
                    }
                });
                SVM.exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        mp = MediaPlayer.create(MyApplication.getContext(), R.raw.alarm);
                        mp.start();
                    }
                });
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopSelf();
                    }
                }, DELAY);
            }
        }else {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                Accelerometer accelerometer = new Accelerometer(time, values[0], values[1], values[2]);
                accList.add(accelerometer);
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                Gyroscope gyroscope = new Gyroscope(time, values[0], values[1], values[2]);
                gyroList.add(gyroscope);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                Magnetometer magnetometer = new Magnetometer(time, values[0], values[1], values[2]);
                magList.add(magnetometer);
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
