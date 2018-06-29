package com.example.hzf.recognition;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hzf.recognition.common.LogUtil;
import com.example.hzf.recognition.model.FeaVector;
import com.example.hzf.recognition.model.LineChartModel;
import com.example.hzf.recognition.model.Result;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.litepal.crud.DataSupport;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.example.hzf.recognition.RecognitionActivity.activityName;

public class ChartActivity extends AppCompatActivity {

    private PieChart mPieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_chart);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        }

        Intent intent = getIntent();
        String date = intent.getStringExtra("date");
        ArrayList<PieEntry> entries = initData(date);
        if(entries == null || entries.size() == 0){
            Toast.makeText(this, "当日无运动记录", Toast.LENGTH_SHORT).show();
            finish();
        }
        TextView textView = (TextView)findViewById(R.id.id_text_title);
        textView.setText(date + "活动统计");

        mPieChart = (PieChart)findViewById(R.id.id_chart);

        mPieChart.setUsePercentValues(true);
        mPieChart.getDescription().setEnabled(false);
        mPieChart.setExtraOffsets(0, 0, 0, 0);

        mPieChart.setDragDecelerationFrictionCoef(0.95f);

        mPieChart.setDrawHoleEnabled(true);
        mPieChart.setHoleColor(Color.WHITE);

        mPieChart.setTransparentCircleColor(Color.WHITE);
        mPieChart.setTransparentCircleAlpha(110);

        mPieChart.setHoleRadius(38f);
        mPieChart.setTransparentCircleRadius(42f);

        mPieChart.setRotationAngle(0);
        // 触摸旋转
        mPieChart.setRotationEnabled(true);
        mPieChart.setHighlightPerTapEnabled(true);

        mPieChart.setDrawEntryLabels(false);

        mPieChart.animateY(1400, Easing.EasingOption.EaseInOutQuad);

        Legend legend = mPieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setWordWrapEnabled(true);
        legend.setDrawInside(false);
        legend.setXEntrySpace(10f);
        legend.setYEntrySpace(5f);
        legend.setYOffset(0f);

        mPieChart.setEntryLabelColor(Color.WHITE);
        mPieChart.setEntryLabelTextSize(12f);

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();
        for (int i = 2; i < ColorTemplate.VORDIPLOM_COLORS.length; i++) {
            colors.add(ColorTemplate.VORDIPLOM_COLORS[i]);
        }
        for (int i = 0; i < ColorTemplate.JOYFUL_COLORS.length; i++) {
            if(i != 2) {
                colors.add(ColorTemplate.JOYFUL_COLORS[i]);
            }
        }
        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);
        colors.add(ColorTemplate.getHoloBlue());
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        mPieChart.setData(data);
        mPieChart.highlightValues(null);
        //刷新
        mPieChart.invalidate();
    }

    private ArrayList<PieEntry> initData(String date) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Result> resultList = new ArrayList<>();

        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            long begin = sf.parse(date).getTime();
            long end = begin + 24 * 60 * 60 * 1000;
            List<FeaVector> list = DataSupport.select("id", "startTime", "endTime", "category", "origin")
                    .where("startTime >= ? and startTime < ?", String.valueOf(begin), String.valueOf(end))
                    .find(FeaVector.class);
            if(list == null || list.size() == 0){
                return entries;
            }
            //将连续的活动组合
            Iterator<FeaVector> iterator = list.iterator();
            FeaVector pre = list.get(0), head = list.get(0);
            while (iterator.hasNext()){
                FeaVector cur = iterator.next();
                if(cur.getOrigin() != pre.getOrigin()){
                    Result result = new Result(pre.getOrigin(), head.getStartTime(), pre.getEndTime(), head.getId(), pre.getId());
                    resultList.add(result);
                    head = cur;
                }
                pre = cur;
            }
            Result result = new Result(pre.getOrigin(), head.getStartTime(), pre.getEndTime(), head.getId(), pre.getId());
            resultList.add(result);
            long[] duration = new long[9];
            for(Result res : resultList){
                duration[res.getCategory()] += (res.getEnd() - res.getStart()) / 1000;
            }
            for(int i = 1; i < duration.length; i++){
                if(duration[i] != 0) {
                    entries.add(new PieEntry(duration[i], activityName(i) + " " + duration[i] + "s"));
                }
            }

            //存储当天每个活动的总时间
            LineChartModel totalTime = DataSupport.where("today = ?", date).findFirst(LineChartModel.class);
            if(totalTime == null){
                LineChartModel total = new LineChartModel(date, duration[1], duration[6], duration[7]);
                total.save();
            }else {
                totalTime.setSitTime(duration[1]);
                totalTime.setWalkTime(duration[6]);
                totalTime.setRunTime(duration[7]);
                totalTime.save();
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return entries;
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
