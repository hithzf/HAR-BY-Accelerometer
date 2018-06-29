package com.example.hzf.recognition;

import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.example.hzf.recognition.common.LogUtil;
import com.example.hzf.recognition.model.LineChartModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.litepal.crud.DataSupport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class LineChartActivity extends AppCompatActivity {

    private LineChart mLineChart;
    private List<String> mDateList;
    private LineData data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_chart);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_line_chart);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        mLineChart = (LineChart)findViewById(R.id.id_line_chart);
        initData();

        mLineChart.setDrawBorders(false);
        //x轴
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(7, true);
        xAxis.setDrawGridLines(false);

        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return mDateList.get((int) value);
            }
        });
        //y轴
        YAxis rightAxis = mLineChart.getAxisRight();
        rightAxis.setEnabled(false);
        YAxis leftAxis = mLineChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        //图例
        Legend legend = mLineChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setWordWrapEnabled(true);
        legend.setDrawInside(false);
        legend.setXEntrySpace(10f);
        legend.setYEntrySpace(5f);
        //描述
        mLineChart.getDescription().setEnabled(false);
        //动画
        mLineChart.animateXY(1500, 1500);
        
        mLineChart.setData(data);
    }

    private void initData() {
        mDateList = new ArrayList<>();
        mDateList.add("");

        long[] sitTime = new long[7];
        long[] walkTime = new long[7];
        long[] runTime = new long[7];

        for(int i = 6; i >= 0; i--){
            String cur = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis() - i * 24 * 3600 * 1000);
            mDateList.add(cur.substring(cur.indexOf("-") + 1));

            LineChartModel totalTime = DataSupport.where("today = ?", cur).findFirst(LineChartModel.class);
            if(totalTime != null) {
                sitTime[6 - i] = totalTime.getSitTime();
                walkTime[6 - i] = totalTime.getWalkTime();
                runTime[6 - i] = totalTime.getRunTime();
            }
        }

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        List<Entry> entries = new ArrayList<>();
        for (int j = 1; j < 8; j++) {
            entries.add(new Entry(j, sitTime[j - 1]));
        }
        LineDataSet lineDataSet = new LineDataSet(entries, "sitting");
        //线条设置
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setCircleColor(ColorTemplate.VORDIPLOM_COLORS[2]);
        lineDataSet.setValueTextSize(11f);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setColor(ColorTemplate.VORDIPLOM_COLORS[2]);
        dataSets.add(lineDataSet);

        entries = new ArrayList<>();
        for (int j = 1; j < 8; j++) {
            entries.add(new Entry(j, walkTime[j - 1]));
        }
        lineDataSet = new LineDataSet(entries, "walking");
        //线条设置
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setCircleColor(ColorTemplate.VORDIPLOM_COLORS[3]);
        lineDataSet.setValueTextSize(11f);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setColor(ColorTemplate.VORDIPLOM_COLORS[3]);
        dataSets.add(lineDataSet);

        entries = new ArrayList<>();
        for (int j = 1; j < 8; j++) {
            entries.add(new Entry(j, runTime[j - 1]));
        }
        lineDataSet = new LineDataSet(entries, "running");
        //线条设置
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setCircleColor(ColorTemplate.VORDIPLOM_COLORS[4]);
        lineDataSet.setValueTextSize(11f);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setColor(ColorTemplate.VORDIPLOM_COLORS[4]);
        dataSets.add(lineDataSet);

        data = new LineData(dataSets);
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
