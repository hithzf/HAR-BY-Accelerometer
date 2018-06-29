package com.example.hzf.recognition;

import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.example.hzf.recognition.common.FileOperateUtil;
import com.example.hzf.recognition.common.LogUtil;
import com.github.mikephil.charting.charts.BubbleChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BubbleData;
import com.github.mikephil.charting.data.BubbleDataSet;
import com.github.mikephil.charting.data.BubbleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBubbleDataSet;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class BubbleActivity extends AppCompatActivity implements OnChartValueSelectedListener {
    private ScatterChart mScatterChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bubble);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_bubble_chart);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        //散点图
        mScatterChart = (ScatterChart) findViewById(R.id.id_bubble_chart);
        mScatterChart.getDescription().setEnabled(false);
        mScatterChart.setOnChartValueSelectedListener(this);

        mScatterChart.setDrawGridBackground(false);
        mScatterChart.setTouchEnabled(true);
        mScatterChart.setMaxHighlightDistance(10f);

        // 支持缩放和拖动
        mScatterChart.setDragEnabled(true);
        mScatterChart.setScaleEnabled(true);

        mScatterChart.setMaxVisibleValueCount(10);
        mScatterChart.setPinchZoom(true);


        Legend l = mScatterChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(true);
        l.setXOffset(5f);

        YAxis yl = mScatterChart.getAxisLeft();
        yl.setSpaceTop(30f);
        yl.setSpaceBottom(30f);
        yl.setDrawZeroLine(false);

        mScatterChart.getAxisRight().setEnabled(false);

        XAxis xl = mScatterChart.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);

        setData();
    }

    //设置数据
    private void setData() {

        ArrayList<Entry> yVals1 = new ArrayList<>();
        ArrayList<Entry> yVals2 = new ArrayList<>();
        ArrayList<Entry> yVals3 = new ArrayList<>();
        List<String> clusterList = FileOperateUtil.readCluster();
        int scale = 50;
        for (int i = 0; i < clusterList.size(); i++){
            String[] split = clusterList.get(i).split(",");
            switch (split[0]){
                case "0":
                    yVals1.add(new Entry(Float.parseFloat(split[1]) * scale, Float.parseFloat(split[2]) * scale));
                    break;
                case "1":
                    yVals2.add(new Entry(Float.parseFloat(split[1]) * scale, Float.parseFloat(split[2]) * scale));
                    break;
                case "2":
                    yVals3.add(new Entry(Float.parseFloat(split[1]) * scale, Float.parseFloat(split[2]) * scale));
                    break;
            }
        }

        ArrayList<IScatterDataSet> dataSets = new ArrayList<>();

        if(yVals1 != null && yVals1.size() != 0) {
            ScatterDataSet set1 = new ScatterDataSet(yVals1, "子群1");
            set1.setScatterShape(ScatterChart.ScatterShape.SQUARE);
            set1.setColor(ColorTemplate.COLORFUL_COLORS[0]);
            set1.setDrawValues(true);
            set1.setValueTextColor(Color.BLACK);
            set1.setScatterShapeSize(20f);
            dataSets.add(set1);
        }

        if(yVals2 != null && yVals2.size() != 0) {
            ScatterDataSet set2 = new ScatterDataSet(yVals2, "子群2");
            set2.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            set2.setColor(ColorTemplate.COLORFUL_COLORS[1]);
            set2.setDrawValues(true);
            set2.setValueTextColor(Color.BLACK);
            set2.setScatterShapeSize(40f);
            dataSets.add(set2);
        }

        if(yVals3 != null && yVals3.size() != 0) {
            ScatterDataSet set3 = new ScatterDataSet(yVals3, "子群3");
            set3.setScatterShape(ScatterChart.ScatterShape.CROSS);
            set3.setColor(ColorTemplate.COLORFUL_COLORS[2]);
            set3.setDrawValues(true);
            set3.setValueTextColor(Color.BLACK);
            set3.setScatterShapeSize(60f);
            dataSets.add(set3);
        }

        ScatterData data = new ScatterData(dataSets);

        mScatterChart.setData(data);
        mScatterChart.invalidate();

        //默认动画
//        mBubbleChart.animateXY(3000, 3000);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }
}
