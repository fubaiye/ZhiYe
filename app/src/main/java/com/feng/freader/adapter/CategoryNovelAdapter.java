package com.feng.freader.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.feng.freader.R;

import java.util.List;

/**
 * @author Feng Zhaohao
 * Created on 2019/12/21
 */
public class CategoryNovelAdapter extends RecyclerView.Adapter<CategoryNovelAdapter.CategoryNovelViewHolder> {

    private Context mContext;
    private List<String> mCoverList;
    private List<String> mNameList;

    private CategoryNovelListener mListener;

    public interface CategoryNovelListener {
        void clickItem(String novelName);
    }

    public void setOnCategoryNovelListener(CategoryNovelListener listener) {
        mListener = listener;
    }

    public CategoryNovelAdapter(Context mContext, List<String> mCoverList, List<String> mNameList) {
        this.mContext = mContext;
        this.mCoverList = mCoverList;
        this.mNameList = mNameList;
    }

    @NonNull
    @Override
    public CategoryNovelViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new CategoryNovelViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.item_category_novel, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryNovelViewHolder categoryNovelViewHolder, final int position) {
        String coverUrl = position < safeSize(mCoverList) ? mCoverList.get(position) : "";
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .transform(new RoundedCorners(dpToPx(12)))
                .placeholder(R.drawable.cover_place_holder)
                .error(R.drawable.cover_error);
        if (isBlankCover(coverUrl)) {
            Glide.with(mContext)
                    .load(R.drawable.cover_place_holder)
                    .apply(options)
                    .into(categoryNovelViewHolder.cover);
        } else {
            Glide.with(mContext)
                    .load(coverUrl)
                    .apply(options)
                    .into(categoryNovelViewHolder.cover);
        }
        categoryNovelViewHolder.cover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null && position < safeSize(mNameList)) {
                    mListener.clickItem(mNameList.get(position));
                }
            }
        });
        categoryNovelViewHolder.name.setText(position < safeSize(mNameList) ? mNameList.get(position) : "");
    }

    @Override
    public int getItemCount() {
        return safeItemCount(mNameList, mCoverList);
    }

    public static int safeItemCount(List<String> names, List<String> covers) {
        return safeSize(names);
    }

    public static boolean isBlankCover(String coverUrl) {
        return coverUrl == null || coverUrl.trim().length() == 0;
    }

    private static int safeSize(List<String> list) {
        return list == null ? 0 : list.size();
    }

    private int dpToPx(int dp) {
        return (int) (dp * mContext.getResources().getDisplayMetrics().density + 0.5f);
    }

    class CategoryNovelViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView name;

        public CategoryNovelViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.iv_item_category_novel_cover);
            name = itemView.findViewById(R.id.tv_item_category_novel_name);
        }
    }
}
