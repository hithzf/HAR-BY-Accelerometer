package com.example.hzf.recognition.sample;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.example.hzf.recognition.MainActivity;
import com.example.hzf.recognition.R;
import com.example.hzf.recognition.RecognitionActivity;
import com.example.hzf.recognition.classifier.SVM;
import com.example.hzf.recognition.common.ContextUtil;
import com.example.hzf.recognition.common.FileOperateUtil;
import com.example.hzf.recognition.common.LogUtil;
import com.example.hzf.recognition.common.MyApplication;
import com.example.hzf.recognition.common.Preprocess;
import com.example.hzf.recognition.feature.FeatureCore;

import java.util.ArrayList;

/**
 * 固定采集频率50Hz
 */
public class OriginService extends Service implements SensorEventListener{

    public static final int ORIGIN_ID = 1;

    private SensorManager mSensorManager;

    private Sensor mSensorAcc;

    //一个时间窗口内加速度数据以及时间
    private ArrayList<Double> accX = new ArrayList<>();
    private ArrayList<Double> accY = new ArrayList<>();
    private ArrayList<Double> accZ = new ArrayList<>();
    private ArrayList<Long> timeList = new ArrayList<>();

    //正常采样率
    public static final int SAMPLE_FREQ = 50;
    public static final int SAMPLE_DELAY = 20000;

    //2秒采集到的数据量
    public int windowLength = 2 * SAMPLE_FREQ;

    //Service工作次数
    public int peakCount = MainActivity.GROUP_NUM;

    //计数器
    private int count = 0;

    private boolean isDestroyed = false;
    private PowerManager.WakeLock mWakeLock;

    private ScreenReceiver mReceiver;

    //最近的一条数据ID
    private int lastIndex;

    private MediaPlayer mp;

    public OriginService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        LogUtil.d(OriginService.class.getSimpleName(), "onCreate");

        //前台服务
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(OriginService.class.getSimpleName())
                .setContentText("Working...")
                .setSmallIcon(R.mipmap.monitor)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.monitor))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pi)
                .build();
        startForeground(ORIGIN_ID, notification);

        //播放提示音
        SVM.exec.execute(new Runnable() {
            @Override
            public void run() {
                Vibrator vibrator = (Vibrator)MyApplication.getContext().getSystemService(MyApplication.getContext().VIBRATOR_SERVICE);
                vibrator.vibrate(1000);
            }
        });
        SVM.exec.execute(new Runnable() {
            @Override
            public void run() {
                mp = MediaPlayer.create(MyApplication.getContext(), R.raw.alarm);
                mp.start();
            }
        });

        //获取传感器管理对象
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensorAcc, SAMPLE_DELAY);

        //电源管理
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, OriginService.class.getSimpleName());
        mWakeLock.acquire();

        //打印电池电量
        FileOperateUtil.printBattery();

        //监听熄屏
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(count >= MainActivity.PRINT_BATTERY_INTERVAL){

            FileOperateUtil.printBattery();
            count = 0;
        }

        //定时任务
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long triggerAtTime = SystemClock.elapsedRealtime() + 10000;
        Intent i = new Intent(this, OriginService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        if(mSensorManager != null) {
            refreshListener();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        LogUtil.d(OriginService.class.getSimpleName(), "onDestroy");

        //取消监听加速度传感器
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        if(mReceiver != null){
            unregisterReceiver(mReceiver);
        }
        if(mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }

        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        //当前时间
        long time = System.currentTimeMillis();
        //一次活动识别的运行时间 = peakCount * 2
        if(count >= peakCount){
            /**
             * case 1:应用每次只运行几分钟
             */
//            if(isDestroyed == false) {
//                isDestroyed = true;
//                //播放提示音
//                SVM.exec.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        Vibrator vibrator = (Vibrator)MyApplication.getContext().getSystemService(MyApplication.getContext().VIBRATOR_SERVICE);
//                        vibrator.vibrate(1000);
//                    }
//                });
//                SVM.exec.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        mp = MediaPlayer.create(MyApplication.getContext(), R.raw.alarm);
//                        mp.start();
//                    }
//                });
//                //发广播
//                Intent i = new Intent("com.example.hzf.recognition.BROADCAST_EXIT");
//                i.putExtra("lastIndex", lastIndex);
//                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MyApplication.getContext());
//                localBroadcastManager.sendBroadcast(i);
//            }

            /**
             * case 2:应用一直运行
             */
            final int lastIdBackup = lastIndex;
            SVM.exec.execute(new Runnable() {
                @Override
                public void run() {
                    FileOperateUtil.saveMemoryAndTime(lastIdBackup, MainActivity.ALGORITHM_ORIGIN);
                }
            });
            count = 0;
        }else {
            if (accX.size() == windowLength) {
                LogUtil.d(OriginService.class.getSimpleName(), "startTime = " + timeList.get(0) + ",endTime = " + timeList.get(timeList.size() - 1));
                lastIndex = FeatureCore.calculateFeature(true, MainActivity.ALGORITHM_ORIGIN,
                        Preprocess.medFilt(accX), Preprocess.medFilt(accY), Preprocess.medFilt(accZ), timeList.get(0), timeList.get(timeList.size() - 1));
                clearAllList();
                count++;
                LogUtil.d(OriginService.class.getSimpleName(), "count = " + count);
            } else {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    accX.add((double) values[0]);
                    accY.add((double) values[1]);
                    accZ.add((double) values[2]);
                    timeList.add(time);
                }
            }
        }
    }

    /**
     * 清空所有List数据
     */
    private void clearAllList(){
        accX.clear();
        accY.clear();
        accZ.clear();
        timeList.clear();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * 熄屏监听
     */
    public class ScreenReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                LogUtil.d(OriginService.class.getSimpleName(), "ACTION_SCREEN_OFF received");
                refreshListener();
            }
        }
    }

    public void refreshListener(){
        if(mWakeLock != null && mSensorManager != null) {
            mWakeLock.release();
            mSensorManager.unregisterListener(this);
            mSensorManager.registerListener(this, mSensorAcc, SAMPLE_DELAY);
            mWakeLock.acquire();
            LogUtil.d(OriginService.class.getSimpleName(), "refreshListener success");
        }
    }
}
