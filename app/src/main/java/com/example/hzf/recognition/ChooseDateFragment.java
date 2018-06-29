package com.example.hzf.recognition;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 选择日期Fragment
 */
public class ChooseDateFragment extends Fragment {
    public static final int LEVEL_YEAR = 0;
    public static final int LEVEL_MONTH = 1;
    public static final int LEVEL_DAY = 2;
    private static final int[] DAYS_OF_MONTH = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private TextView titleText;//标题文字

    private Button backButton;//返回按钮

    private TextView todayText;

    private ListView listView;//列表

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();//显示在列表中的数据

    private int currentLevel;//当前级别

    private int selectedYear;
    private List<Integer> yearList;

    private String selectedMonth;
    private List<String> monthList;

    private List<Integer> dayList;
    public String today;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choose, container, false);
        titleText = (TextView)view.findViewById(R.id.title_text);
        backButton = (Button)view.findViewById(R.id.back_button);
        todayText = (TextView)view.findViewById(R.id.text_today);
        listView = (ListView)view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        today = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_YEAR){
                    selectedYear = yearList.get(position);
                    queryMonths();
                }else if(currentLevel == LEVEL_MONTH){
                    selectedMonth = monthList.get(position);
                    queryDays();
                }else if(currentLevel == LEVEL_DAY){
                    String date = selectedYear + "-" + monthChange(selectedMonth) + "-" + format(dayList.get(position));

                    MainActivity activity = (MainActivity) getActivity();
                    activity.drawerLayout.closeDrawers();
                    activity.loadResult(date);
                    activity.adapter.notifyDataSetChanged();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_MONTH){
                    queryYears();
                }else if(currentLevel == LEVEL_DAY){
                    queryMonths();
                }
            }
        });
        todayText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                activity.drawerLayout.closeDrawers();
                activity.loadResult(today);
                activity.adapter.notifyDataSetChanged();
            }
        });
        init();
        queryYears();
    }

    private String monthChange(String s) {
        switch (s){
            case "Jan":
                return "01";
            case "Feb":
                return "02";
            case "Mar":
                return "03";
            case "Apr":
                return "04";
            case "May":
                return "05";
            case "June":
                return "06";
            case "July":
                return "07";
            case "Aug":
                return "08";
            case "Sept":
                return "09";
            case "Oct":
                return "10";
            case "Nov":
                return "11";
            case "Dec":
                return "12";
            default:
                return "01";
        }
    }

    private String format(int d){
        if(d < 10){
            return "0" + d;
        }
        return String.valueOf(d);
    }

    /**
     * 初始化数据
     */
    private void init() {
        yearList = new ArrayList<>();
        Calendar date = Calendar.getInstance();
        int year = date.get(Calendar.YEAR);
        //前后三年
        for(int i = year - 3; i <= year + 3; i++){
            yearList.add(i);
        }

        monthList = new ArrayList<>();
        monthList.add("Jan");
        monthList.add("Feb");
        monthList.add("Mar");
        monthList.add("Apr");
        monthList.add("May");
        monthList.add("June");
        monthList.add("July");
        monthList.add("Aug");
        monthList.add("Sept");
        monthList.add("Oct");
        monthList.add("Nov");
        monthList.add("Dec");
    }

    /**
     * 查询年数据
     */
    private void queryYears() {
        titleText.setText("Year");
        backButton.setVisibility(View.GONE);

        dataList.clear();
        for (Integer year : yearList){
            dataList.add(year.toString());
        }
        adapter.notifyDataSetChanged();
        listView.setSelection(0);//设置被选中项
        currentLevel = LEVEL_YEAR;
    }

    /**
     * 查询月数据
     */
    private void queryMonths() {
        titleText.setText(String.valueOf(selectedYear));
        backButton.setVisibility(View.VISIBLE);

        dataList.clear();
        for(String month : monthList){
            dataList.add(month);
        }
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        currentLevel = LEVEL_MONTH;
    }

    /**
     * 查询日数据
     */
    private void queryDays() {
        titleText.setText(selectedMonth);
        backButton.setVisibility(View.VISIBLE);

        dayList = new ArrayList<>();
        int maxDay = 0;
        if(selectedMonth.equals("Feb")){
            if((selectedYear % 4 == 0 && selectedYear % 100 != 0) || selectedYear % 400 ==0){
                maxDay = 29;
            }else{
                maxDay = 28;
            }
        }else{
            maxDay = DAYS_OF_MONTH[Integer.parseInt(monthChange(selectedMonth))];
        }
        for(int i = 1; i <= maxDay; i++){
            dayList.add(i);
        }

        dataList.clear();
        for (Integer day : dayList){
            dataList.add(day.toString());
        }
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        currentLevel = LEVEL_DAY;
    }
}
