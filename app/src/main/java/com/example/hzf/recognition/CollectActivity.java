package com.example.hzf.recognition;

import android.content.Intent;
import android.os.Handler;
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

import com.example.hzf.recognition.common.MyApplication;
import com.example.hzf.recognition.sample.SensorCollectService;

import java.util.ArrayList;

public class CollectActivity extends AppCompatActivity {
    private Spinner mSpinner;
    private EditText mEditText;
    private Button mButtonCollect;

    private int select;

    private Intent startIntent;

    //延迟采集数据的毫秒数
    public static final int DELAY = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_collect);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        mSpinner = (Spinner) findViewById(R.id.id_spinner);
        mEditText = (EditText) findViewById(R.id.id_edit);

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
        mSpinner.setAdapter(adapter);

        mButtonCollect = (Button) findViewById(R.id.id_btn_collect);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                select = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mButtonCollect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startIntent = new Intent(CollectActivity.this, SensorCollectService.class);
                        startIntent.putExtra("label", select);
                        startIntent.putExtra("id", Integer.parseInt(mEditText.getText().toString()));
                        startService(startIntent);
                    }
                }, DELAY);
                Toast.makeText(MyApplication.getContext(), "10s后启动服务SensorCollectService", Toast.LENGTH_SHORT).show();
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
