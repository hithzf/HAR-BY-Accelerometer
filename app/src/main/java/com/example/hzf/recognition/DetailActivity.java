package com.example.hzf.recognition;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.hzf.recognition.classifier.SVM;
import com.example.hzf.recognition.common.LogUtil;
import com.example.hzf.recognition.model.FeaVector;

import org.litepal.crud.DataSupport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class DetailActivity extends AppCompatActivity {

    private EditText mStartTime;
    private EditText mEndTime;
    private EditText mCategory;
    private Spinner mOrigin;

    private Button mButtonEdit;
    private Button mButtonDelete;
    private Button mButtonEditAll;

    private ProgressDialog progressDialog;

    private int select;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mStartTime = (EditText) findViewById(R.id.start_time);
        mEndTime = (EditText) findViewById(R.id.end_time);
        mCategory = (EditText) findViewById(R.id.category);
        mOrigin = (Spinner) findViewById(R.id.origin);

        ArrayList<String> dataList = new ArrayList<>();
        dataList.add("unknown");
        dataList.add("sitting");
        dataList.add("standing");
        dataList.add("lying on left side");
        dataList.add("upstairs");
        dataList.add("downstairs");
        dataList.add("walking");
        dataList.add("running");
        dataList.add("quickWalk");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dataList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mOrigin.setAdapter(adapter);

        mButtonEdit = (Button) findViewById(R.id.id_btn_edit);
        mButtonDelete = (Button) findViewById(R.id.id_btn_delete);
        mButtonEditAll = (Button) findViewById(R.id.id_btn_edit_all);

        //设置ToolBar
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        //获取上一个Intent传入的数据
        Intent intent = getIntent();
        int id = intent.getIntExtra("id", 0);
        final String startId = intent.getStringExtra("startId");
        final String endId = intent.getStringExtra("endId");
        final FeaVector feaVector = DataSupport.find(FeaVector.class, id);

        mStartTime.setText(new SimpleDateFormat("MM-dd HH:mm:ss").format(feaVector.getStartTime()));
        mStartTime.setEnabled(false);
        mEndTime.setText(new SimpleDateFormat("MM-dd HH:mm:ss").format(feaVector.getEndTime()));
        mEndTime.setEnabled(false);
        mCategory.setText(RecognitionActivity.activityName(feaVector.getCategory()));
        mCategory.setEnabled(false);
        mOrigin.setSelection(feaVector.getOrigin());

        mOrigin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                select = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mButtonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressDialog();
                SVM.exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        int id = feaVector.getId();
                        feaVector.setOrigin(select);
                        feaVector.updateAll("id = ?", String.valueOf(id));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                closeProgressDialog();
                            }
                        });
                    }
                });

                Toast.makeText(DetailActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
            }
        });

        mButtonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                DataSupport.delete(FeaVector.class, feaVector.getId());
//
//                Toast.makeText(DetailActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        mButtonEditAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressDialog();
                SVM.exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        FeaVector fea = new FeaVector();
                        fea.setOrigin(select);
                        fea.updateAll("id >= ? and id <= ?", startId, endId);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                closeProgressDialog();
                            }
                        });
                    }
                });

                Toast.makeText(DetailActivity.this, "一键修改成功", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }
        return true;
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
}
