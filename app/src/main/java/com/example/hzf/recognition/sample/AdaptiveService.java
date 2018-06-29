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
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.example.hzf.recognition.MainActivity;
import com.example.hzf.recognition.R;
import com.example.hzf.recognition.classifier.SVM;
import com.example.hzf.recognition.common.ContextUtil;
import com.example.hzf.recognition.common.FileOperateUtil;
import com.example.hzf.recognition.common.LogUtil;
import com.example.hzf.recognition.common.MyApplication;
import com.example.hzf.recognition.common.Preprocess;
import com.example.hzf.recognition.feature.FeatureCore;
import com.example.hzf.recognition.model.FeaVector;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * 自适应采样频率
 */
public class AdaptiveService extends Service implements SensorEventListener {
    public static final int ADAPTIVE_ID = 2;

    //传感器对象
    private SensorManager mSensorManager;
    private Sensor mSensor;

    //时序数据
    private ArrayList<Double> xList = new ArrayList<>();
    private ArrayList<Double> yList = new ArrayList<>();
    private ArrayList<Double> zList = new ArrayList<>();
    private ArrayList<Long> timeList = new ArrayList<>();

    //最低采样率
    public static final int BEST_FREQ = 20;
    public static final int BEST_DELAY = 50000;

    //唤醒CPU
    private PowerManager.WakeLock mWakeLock;

    //熄屏接收器
    private ScreenReceiver mReceiver;

    //一次工作的分类次数
    public int peakCount = MainActivity.GROUP_NUM;

    //数据条数计数器
    private int count = 0;
    private boolean isDestroyed = false;

    //最后一条数据的id，用于记录响应时间等信息
    private int lastIndex;

    //提示音播放
    private MediaPlayer mp;

    public AdaptiveService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        LogUtil.d(AdaptiveService.class.getSimpleName(), "onCreate");
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

        //前台服务
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(AdaptiveService.class.getSimpleName())
                .setContentText("Working...")
                .setSmallIcon(R.mipmap.monitor)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.monitor))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pi)
                .build();
        startForeground(ADAPTIVE_ID, notification);

        //获取传感器管理对象
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        //获取加速度传感器
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //为加速度传感器注册监听器
        mSensorManager.registerListener(this, mSensor, OriginService.SAMPLE_DELAY);

        //电源管理
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, AdaptiveService.class.getSimpleName());
        mWakeLock.acquire();

        //打印电池电量
        FileOperateUtil.printBattery();

        //广播接收器
//        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("com.example.hzf.recognition.ADAPTIVE");
//        ResultReceiver resultReceiver = new ResultReceiver();
//        localBroadcastManager.registerReceiver(resultReceiver, intentFilter);

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
        Intent i = new Intent(this, AdaptiveService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        if(mSensorManager != null) {
            refreshListener();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        float[] values = event.values;
        long time = System.currentTimeMillis();
        if(count >= peakCount) {
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
                    FileOperateUtil.saveMemoryAndTime(lastIdBackup, MainActivity.ALGORITHM_ADAPTIVE);
                }
            });
            count = 0;
        }else {

            if (xList.size() == 2 * BEST_FREQ) {
                //提取一个时间序列的特征并保存到数据库
                lastIndex = FeatureCore.calculateFeature(true, MainActivity.ALGORITHM_ADAPTIVE,
                        Preprocess.medFilt(xList), Preprocess.medFilt(yList), Preprocess.medFilt(zList), timeList.get(0), timeList.get(xList.size() - 1));

                xList.clear();
                yList.clear();
                zList.clear();
                timeList.clear();
                count++;
            } else {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    xList.add((double) values[0]);
                    yList.add((double) values[1]);
                    zList.add((double) values[2]);
                    timeList.add(time);
                }
            }
        }
    }


    /**
     * 广播接收器
     */
    class ResultReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
//            Bundle bundle = intent.getExtras();
//            int res = bundle.getInt("result");
//            if(res == ) {
//                //采样频率发生变化，已采集的数据丢弃
//                xList.clear();
//                yList.clear();
//                zList.clear();
//                timeList.clear();
//
//                refreshListener();
//            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onDestroy() {
        LogUtil.d(AdaptiveService.class.getSimpleName(), "onDestroy");

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

    public class ScreenReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                LogUtil.d(AdaptiveService.class.getSimpleName(), "ACTION_SCREEN_OFF received");
                refreshListener();
            }
        }
    }

    public void refreshListener(){
        if(mWakeLock != null && mSensorManager != null) {
            mWakeLock.release();
            mSensorManager.unregisterListener(this);
            mSensorManager.registerListener(this, mSensor, BEST_DELAY);
            mWakeLock.acquire();
        }
    }
}
