package com.feng.freader.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.feng.freader.R;

import java.util.List;

/**
 * @author Feng Zhaohao
 * Created on 2019/11/7
 */
public class HotRankAdapter extends RecyclerView.Adapter<HotRankAdapter.HotRankViewHolder> {

    private Context mContext;
    private List<String> mHotRankNameList;
    private List<List<String>> mHotRankNovelList;
    private HotRankListener mListener;

    public interface HotRankListener {
        void clickFirstNovel(String name);
        void clickSecondNovel(String name);
        void clickThirdNovel(String name);
    }

    public HotRankAdapter(Context mContext, List<String> mHotRankNameList,
                          List<List<String>> mHotRankNovelList, HotRankListener mListener) {
        this.mContext = mContext;
        this.mHotRankNameList = mHotRankNameList;
        this.mHotRankNovelList = mHotRankNovelList;
        this.mListener = mListener;
    }

    @NonNull
    @Override
    public HotRankViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new HotRankViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_hot_rank, null));
    }

    @Override
    public void onBindViewHolder(@NonNull HotRankViewHolder hotRankViewHolder, int i) {
        hotRankViewHolder.hotRankName.setText(formatRankTitle(mHotRankNameList.get(i), i));
        List<String> novelList = mHotRankNovelList.get(i);
        final String firstName = novelList.size() > 0 ? novelList.get(0) : "";
        final String secondName = novelList.size() > 1 ? novelList.get(1) : "";
        final String thirdName = novelList.size() > 2 ? novelList.get(2) : "";
        hotRankViewHolder.firstNovelName.setText(formatRankNovel("\u2460", firstName));
        hotRankViewHolder.firstNovelName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.clickFirstNovel(firstName);
            }
        });
        hotRankViewHolder.secondNovelName.setText(formatRankNovel("\u2461", secondName));
        hotRankViewHolder.secondNovelName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.clickSecondNovel(secondName);
            }
        });
        hotRankViewHolder.thirdNovelName.setText(formatRankNovel("\u2462", thirdName));
        hotRankViewHolder.thirdNovelName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.clickThirdNovel(thirdName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mHotRankNameList.size();
    }

    private String formatRankTitle(String name, int position) {
        String prefix = position % 2 == 0 ? "TOP " : "HOT ";
        return prefix + name;
    }

    private String formatRankNovel(String rank, String name) {
        if (name == null || name.length() == 0) {
            return rank;
        }
        return rank + "  " + name;
    }

    class HotRankViewHolder extends RecyclerView.ViewHolder {

        TextView hotRankName;
        TextView firstNovelName;
        TextView secondNovelName;
        TextView thirdNovelName;

        public HotRankViewHolder(@NonNull View itemView) {
            super(itemView);
            hotRankName = itemView.findViewById(R.id.tv_item_hot_rank_rank_name);
            firstNovelName = itemView.findViewById(R.id.tv_item_hot_rank_first_novel_name);
            secondNovelName = itemView.findViewById(R.id.tv_item_hot_rank_second_novel_name);
            thirdNovelName = itemView.findViewById(R.id.tv_item_hot_rank_third_novel_name);
        }
    }
}
