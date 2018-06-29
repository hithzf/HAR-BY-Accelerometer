package com.example.hzf.recognition;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.hzf.recognition.classifier.SVM;
import com.example.hzf.recognition.common.ContextUtil;
import com.example.hzf.recognition.common.FileOperateUtil;
import com.example.hzf.recognition.common.LogUtil;
import com.example.hzf.recognition.common.MyApplication;
import com.example.hzf.recognition.model.FeaVector;
import com.example.hzf.recognition.model.MemoryTime;
import com.example.hzf.recognition.model.Result;
import com.example.hzf.recognition.sample.AdaptiveService;
import com.example.hzf.recognition.sample.OriginService;

import org.litepal.crud.DataSupport;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public DrawerLayout drawerLayout;
    private Toolbar mToolbar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<Result> resultList = new ArrayList<>();
    public ResultAdapter adapter;

    //选用的算法类型
    public static final int ALGORITHM_ORIGIN = 0;
    public static final int ALGORITHM_ADAPTIVE = 1;

    //延迟采集数据的毫秒数，目的是剔除手机放入口袋这一过程中的干扰数据
    public static final int DELAY = 10000;

    private Intent startIntent;

    //当前显示的日期
    private String date;

    //一次活动识别的运行时间：GROUP_NUM个时间窗口
    public static final int GROUP_NUM = 60;

    //记录手机电量的时间间隔：PRINT_BATTERY_INTERVAL个时间窗口
    public static final int PRINT_BATTERY_INTERVAL = 600;

    //活动识别运行模式：{ORIGIN,ADAPTIVE}
    private static int mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        mToolbar = (Toolbar)findViewById(R.id.id_toolbar_main);
        setSupportActionBar(mToolbar);
        //设置导航按钮
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);//将导航按钮显示
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);//设置导航按钮图标
        }

        //加载数据
        String today = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());
        loadResult(today);

        //RecyclerView
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ResultAdapter(resultList);
        recyclerView.setAdapter(adapter);

        //下拉刷新
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshResult();
            }
        });

        //广播接收器
//        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("com.example.hzf.recognition.BROADCAST_EXIT");
//        LocalReceiver localReceiver = new LocalReceiver();
//        localBroadcastManager.registerReceiver(localReceiver, intentFilter);

        //申请权限
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        //创建文件夹
        FileOperateUtil.createDirs();
    }

    /**
     * 下拉刷新
     */
    private void refreshResult() {
        SVM.exec.execute(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SVM.train();
                        loadResult(date);
                        adapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    /**
     * 查询数据
     * @param date
     */
    public void loadResult(String date) {
        resultList.clear();
        this.date = date;
        setTitle(date);
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            long begin = sf.parse(date).getTime();
            long end = begin + 24 * 60 * 60 * 1000;
            List<FeaVector> list = DataSupport.select("id", "startTime", "endTime", "category", "origin")
                    .where("startTime >= ? and startTime < ?", String.valueOf(begin), String.valueOf(end))
                    .find(FeaVector.class);
            //获取每组最后一条数据的id
            List<MemoryTime> memoryTimeList = DataSupport.select("lastId").find(MemoryTime.class);
            Set<Integer> lastIdSet = new HashSet<>();
            Iterator<MemoryTime> iterator = memoryTimeList.iterator();
            while (iterator.hasNext()){
                lastIdSet.add(iterator.next().getLastId());
            }
            //生成Result列表
            int before = -1;
            for(int i = 0; i < list.size(); i += 1){
                int cur = list.get(i).getId();
                if(lastIdSet.contains(cur)){
                    Result result = new Result(list.get((before+1 + i) / 2).getOrigin(),
                                                list.get(before+1).getStartTime(),
                                                list.get(i).getEndTime(),
                                                list.get(before+1).getId(),
                                                list.get(i).getId());
                    resultList.add(result);
                    before = i;
                }
            }
            if(before + 1 <= list.size() - 1) {
                Result result = new Result(list.get((before + 1 + list.size() - 1) / 2).getOrigin(),
                        list.get(before + 1).getStartTime(),
                        list.get(list.size() - 1).getEndTime(),
                        list.get(before + 1).getId(),
                        list.get(list.size() - 1).getId());
                resultList.add(result);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.start:
                if(startIntent == null) {
                    mode = ALGORITHM_ORIGIN;
                    startIntent = new Intent(MainActivity.this, OriginService.class);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startService(startIntent);
                            //清空battery.txt文件
                            FileOperateUtil.clearFile("battery.txt");
                        }
                    }, DELAY);
                    Toast.makeText(MyApplication.getContext(), "10s后启动服务OriginService", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MyApplication.getContext(), "服务OriginService暂时不可启动", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.collect:
                Intent intent = new Intent(MainActivity.this, CollectActivity.class);
                startActivity(intent);
                break;
            case R.id.chart:
                Intent intentChart = new Intent(MainActivity.this, ChartActivity.class);
                intentChart.putExtra("date", date);
                startActivity(intentChart);
                break;
            case R.id.line_chart:
                Intent intentLine = new Intent(MainActivity.this, LineChartActivity.class);
                startActivity(intentLine);
                break;
            case R.id.bubble_chart:
                Intent intentBubble = new Intent(MainActivity.this, BubbleActivity.class);
                startActivity(intentBubble);
                break;
            case R.id.adaptive:
                if(startIntent == null) {
                    mode = ALGORITHM_ADAPTIVE;
                    startIntent = new Intent(MainActivity.this, AdaptiveService.class);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startService(startIntent);
                            //清空battery.txt文件
                            FileOperateUtil.clearFile("battery.txt");
                        }
                    }, DELAY);
                    Toast.makeText(MyApplication.getContext(), "10s后启动服务AdaptiveService", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MyApplication.getContext(), "服务AdaptiveService暂时不可启动", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return true;
    }

    /**
     * 本地广播接收器
     */
    class LocalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(startIntent != null) {
                stopService(startIntent);
                startIntent = null;
            }
            LogUtil.d("RecognitionActivity", "broadcast received");

            Bundle bundle = intent.getExtras();
            final int lastIndex = bundle.getInt("lastIndex");
            LogUtil.d(MainActivity.class.getSimpleName(), "lastIndex = " + lastIndex);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    LogUtil.d(MainActivity.class.getSimpleName(), "mode = " + mode);
                    FileOperateUtil.saveMemoryAndTime(lastIndex, mode);
//                    android.os.Process.killProcess(android.os.Process.myPid());
//                    System.exit(0);
                }
            }, DELAY);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        ){

                }else {
                    if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                        Toast.makeText(this, "拒绝写权限将无法使用本应用", Toast.LENGTH_SHORT).show();
                    }
                    if(grantResults[1] == PackageManager.PERMISSION_DENIED){
                        Toast.makeText(this, "拒绝读权限将无法使用本应用", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
        }
    }
}
