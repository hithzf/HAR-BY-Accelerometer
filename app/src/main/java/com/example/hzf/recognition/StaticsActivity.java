package com.example.hzf.recognition;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.hzf.recognition.common.LogUtil;
import com.example.hzf.recognition.common.MyApplication;
import com.example.hzf.recognition.model.FeaVector;
import com.example.hzf.recognition.model.MemoryTime;

import org.litepal.crud.DataSupport;

public class StaticsActivity extends AppCompatActivity {
    private EditText mAlgorithm;
    private EditText mWrong;
    private EditText mResponse;
    private EditText mSvm;
    private EditText mPower;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statics);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_statics);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        Intent intent = getIntent();
        String startId = intent.getStringExtra("startId");
        String endId = intent.getStringExtra("endId");

        int wrong = DataSupport.where("id >= ? and id <= ? and category != origin", startId, endId).count(FeaVector.class);
        LogUtil.d(RecognitionActivity.class.getSimpleName(), "lastId = " + endId);
        final MemoryTime memoryTime = DataSupport.where("lastId = ?", endId).findFirst(MemoryTime.class);
        String time = "", mem = "", alg = "";
        int timeSize = 0, memSize = 0, svmFreq = -1;
        if(memoryTime != null) {
            time = memoryTime.getTime();
            svmFreq = memoryTime.getSvmFreq();
            timeSize = memoryTime.getTimeSize();
            if(memoryTime.getMode() == 0){
                alg = "ORIGIN";
            }else if(memoryTime.getMode() == 1){
                alg = "LIGHTWEIGHT";
            }
        }

        mAlgorithm = (EditText) findViewById(R.id.id_alg);
        mAlgorithm.setText(alg);
        mAlgorithm.setEnabled(false);
        mWrong = (EditText) findViewById(R.id.id_mis);
        mWrong.setText(String.valueOf(wrong));
        mWrong.setEnabled(false);
        mResponse = (EditText) findViewById(R.id.id_response);
        mResponse.setText(time + "          -" + timeSize);
        mResponse.setEnabled(false);
        mSvm = (EditText) findViewById(R.id.id_svm);
        mSvm.setText(String.valueOf(svmFreq));
        mSvm.setEnabled(false);

        mPower = (EditText) findViewById(R.id.id_power);
        if(memoryTime != null && memoryTime.getPower() != null){
            mPower.setText(memoryTime.getPower());
        }
        mButton = (Button) findViewById(R.id.id_btn_save);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String power = mPower.getText().toString();
                memoryTime.setPower(power);
                if(memoryTime.save()){
                    finish();
                }else{
                    Toast.makeText(MyApplication.getContext(), "修改失败", Toast.LENGTH_SHORT).show();
                }
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
}
