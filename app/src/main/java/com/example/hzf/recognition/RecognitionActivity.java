package com.example.hzf.recognition;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.example.hzf.recognition.classifier.SVM;
import com.example.hzf.recognition.common.FileOperateUtil;
import com.example.hzf.recognition.common.LogUtil;
import com.example.hzf.recognition.common.MyApplication;
import com.example.hzf.recognition.model.FeaVector;
import com.example.hzf.recognition.model.MemoryTime;

public class RecognitionActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ListView mListView;
    private FeaVectorAdapter mAdapter;

    private ProgressDialog progressDialog;//对话框

    private List<FeaVector> mData = new ArrayList<>();

    private PowerManager powerManager;
    private PowerManager.WakeLock mWakeLock;

    private String startId;
    private String endId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        mToolbar = (Toolbar)findViewById(R.id.id_toolbar_recognition);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        mListView = (ListView)findViewById(R.id.id_list_recognition_result);

        Intent intent = this.getIntent();
        startId = intent.getStringExtra("startId");
        endId = intent.getStringExtra("endId");

        loadData();
        mAdapter = new FeaVectorAdapter(RecognitionActivity.this, R.layout.item_recognition_result, mData);
        mListView.setAdapter(mAdapter);

        //电源管理
        powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "closeScreenLock");
        mWakeLock.acquire();

        //列表点击事件
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                FeaVector clicked = mData.get(i);
                Intent intent = new Intent(RecognitionActivity.this, DetailActivity.class);
                intent.putExtra("id", clicked.getId());
                intent.putExtra("startId", startId);
                intent.putExtra("endId", endId);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_recognition, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.add_train:
                List<FeaVector> list = DataSupport.where("id >= ? and id <= ?", startId, endId).find(FeaVector.class);
                FileOperateUtil.appendFeaVector(list);
                Toast.makeText(MyApplication.getContext(), "训练数据已添加", Toast.LENGTH_SHORT).show();
                //删除SVM模型
                FileOperateUtil.deleteFile("model.txt");
                break;
            case R.id.classify:
                showProgressDialog();
                List<FeaVector> unclassified = DataSupport.select("id").where("category = ?", "0").find(FeaVector.class);
                final ArrayList<Integer> idList = new ArrayList<>();
                for(int i = 0; i < unclassified.size(); i++){
                    idList.add(unclassified.get(i).getId());
                }

                SVM.exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        SVM.calculate(idList);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                closeProgressDialog();
                            }
                        });
                    }
                });
                break;
            case R.id.clear:
                DataSupport.deleteAll(FeaVector.class, "id >= ? and id <= ?", startId, endId);
                Toast.makeText(MyApplication.getContext(), "数据已清空", Toast.LENGTH_SHORT).show();
                break;
            case R.id.statics:
                Intent intent = new Intent(RecognitionActivity.this, StaticsActivity.class);
                intent.putExtra("startId", startId);
                intent.putExtra("endId", endId);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadData();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /**
     * 加载活动识别结果
     */
    public void loadData() {
        mData = DataSupport.select("id", "startTime", "endTime", "category", "origin")
                .where("id >= ? and id <= ?", startId, endId)
                .find(FeaVector.class);
    }

    /**
     * 将活动类别转换成文字
     * @param catagory
     * @return
     */
    public static String activityName(int catagory) {
        switch (catagory){
            case 1:
                return "sitting";
            case 2:
                return "standing";
            case 3:
                return "lying";
            case 4:
                return "upstairs";
            case 5:
                return "downstairs";
            case 6:
                return "walking";
            case 7:
                return "running";
            case 8:
                return "quickWalk";
        }
        return "unknown";
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
    }


    /**
     * 显示进度对话框
     */
    private void showProgressDialog(){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Processing...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }


    /**
     * 适配器类
     */
    public class FeaVectorAdapter extends ArrayAdapter<FeaVector> {
        private int resourceId;

        public FeaVectorAdapter(Context context, int resource, List<FeaVector> objects){
            super(context, resource, objects);
            resourceId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FeaVector result = getItem(position);
            View view;
            ViewHolder viewHolder;
            if(convertView == null){
                view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.time = (TextView)view.findViewById(R.id.id_recognition_time);
                viewHolder.duration = (TextView)view.findViewById(R.id.id_recognition_duration);
                viewHolder.category = (TextView)view.findViewById(R.id.id_recognition_result);
                viewHolder.origin = (TextView)view.findViewById(R.id.id_recognition_origin);
                view.setTag(viewHolder);
            }else{
                view = convertView;
                viewHolder = (ViewHolder)view.getTag();
            }
            viewHolder.time.setText(new SimpleDateFormat("HH:mm:ss").format(result.getStartTime()));
            DecimalFormat df = new DecimalFormat("#.0");
            String duration = df.format((result.getEndTime() - result.getStartTime() + 1) / (double)1000);
            viewHolder.duration.setText(duration + " s");
            viewHolder.category.setText(activityName(result.getCategory()));
            viewHolder.origin.setText(activityName(result.getOrigin()));
            return view;
        }

        class ViewHolder{
            TextView time;
            TextView duration;
            TextView category;
            TextView origin;
        }
    }

}
