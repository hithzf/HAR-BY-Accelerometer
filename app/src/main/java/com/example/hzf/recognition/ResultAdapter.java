package com.example.hzf.recognition;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.hzf.recognition.common.LogUtil;
import com.example.hzf.recognition.common.MyApplication;
import com.example.hzf.recognition.model.Result;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Created by hzf on 2018/4/4.
 */

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {

    List<Result> resultList;

    Context mContext;

    public ResultAdapter(List<Result> resultList){
        this.resultList = resultList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(mContext == null){
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_result, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                Result result = resultList.get(position);
                Intent intent = new Intent(mContext, RecognitionActivity.class);
                intent.putExtra("startId", String.valueOf(result.getBeginId()));
                intent.putExtra("endId", String.valueOf(result.getEndId()));

                mContext.startActivity(intent);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Result result = resultList.get(position);
        holder.activityImage.setImageResource(getResource("ic_" + result.getCategory()));
        holder.activityName.setText(RecognitionActivity.activityName(result.getCategory()));
        SimpleDateFormat sf = new SimpleDateFormat("KK:mm aa", Locale.ENGLISH);
        holder.textTime.setText(sf.format(result.getStart()) + " ~ " + sf.format(result.getEnd()));
        holder.textDuration.setText(String.format("%.1f",(result.getEnd() - result.getStart()) / 60000.0));
        holder.textWrong.setText(String.valueOf(result.getWrong()));
    }

    @Override
    public int getItemCount() {
        return resultList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        LinearLayout linearLayout;
        ImageView activityImage;
        TextView activityName;
        TextView textTime;
        TextView textDuration;
        TextView textWrong;

        public ViewHolder(View itemView){
            super(itemView);
            linearLayout = (LinearLayout)itemView;
            activityImage = (ImageView) itemView.findViewById(R.id.id_activity_image);
            activityName = (TextView) itemView.findViewById(R.id.id_activity_name);
            textTime = (TextView) itemView.findViewById(R.id.id_text_time);
            textDuration = (TextView) itemView.findViewById(R.id.id_text_duration);
            textWrong = (TextView) itemView.findViewById(R.id.id_wrong);
        }
    }

    /**
     * 根据图片名称回去资源id
     * @param imageName
     * @return
     */
    public int  getResource(String imageName){
        Context ctx = MyApplication.getContext();
        int resId = ctx.getResources().getIdentifier(imageName, "drawable", ctx.getPackageName());
        //如果没有在"mipmap"下找到imageName,将会返回0
        return resId;
    }
}
