package com.feng.freader.view.fragment.main;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.feng.freader.R;
import com.feng.freader.adapter.BookshelfNovelsAdapter;
import com.feng.freader.base.BaseFragment;
import com.feng.freader.base.BasePresenter;
import com.feng.freader.constant.EventBusCode;
import com.feng.freader.constract.IBookshelfContract;
import com.feng.freader.db.DatabaseManager;
import com.feng.freader.entity.data.BookshelfNovelDbData;
import com.feng.freader.entity.epub.OpfData;
import com.feng.freader.entity.eventbus.Event;
import com.feng.freader.export.BookExportResult;
import com.feng.freader.export.BookExporter;
import com.feng.freader.export.ExportFormat;
import com.feng.freader.model.LocalBookMetadata;
import com.feng.freader.model.LocalBookMetadataFetcher;
import com.feng.freader.presenter.BookshelfPresenter;
import com.feng.freader.util.FileUtil;
import com.feng.freader.util.NetUtil;
import com.feng.freader.util.RecyclerViewUtil;
import com.feng.freader.util.StatusBarUtil;
import com.feng.freader.view.activity.ReadActivity;
import com.feng.freader.widget.TipDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Feng Zhaohao
 * Created on 2019/10/20
 */
public class BookshelfFragment extends BaseFragment<BookshelfPresenter>
        implements View.OnClickListener, IBookshelfContract.View {

    private static final String TAG = "BookshelfFragment";

    private RecyclerView mBookshelfNovelsRv;
    private TextView mLocalAddTv;
    private ImageView mLocalAddIv;
    private RelativeLayout mLoadingRv;

    private RelativeLayout mMultiDeleteRv;
    private TextView mSelectAllTv;
    private TextView mCancelTv;
    private TextView mDeleteTv;

    private List<BookshelfNovelDbData> mDataList = new ArrayList<>();
    private List<Boolean> mCheckedList = new ArrayList<>();
    private BookshelfNovelsAdapter mBookshelfNovelsAdapter;
    private boolean mIsDeleting = false;
    private final Set<String> mEnrichingLocalBooks = new HashSet<>();

    private DatabaseManager mDbManager;

    @Override
    protected void doInOnCreate() {
        StatusBarUtil.setLightColorStatusBar(getActivity());
        mPresenter.queryAllBook();
    }

    @Override
    protected BookshelfPresenter getPresenter() {
        return new BookshelfPresenter();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_bookshelf;
    }

    @Override
    protected void initData() {
        mDbManager = DatabaseManager.getInstance();
    }

    @Override
    protected void initView() {
        mBookshelfNovelsRv = getActivity().findViewById(R.id.rv_bookshelf_bookshelf_novels_list);
        final GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3);
        mBookshelfNovelsRv.setLayoutManager(gridLayoutManager);

        mLocalAddTv = getActivity().findViewById(R.id.tv_bookshelf_add);
        mLocalAddTv.setOnClickListener(this);
        mLocalAddIv = getActivity().findViewById(R.id.iv_bookshelf_add);
        mLocalAddIv.setOnClickListener(this);

        mLoadingRv = getActivity().findViewById(R.id.rv_bookshelf_loading);

        mMultiDeleteRv = getActivity().findViewById(R.id.rv_bookshelf_multi_delete_bar);
        mSelectAllTv = getActivity().findViewById(R.id.tv_bookshelf_multi_delete_select_all);
        mSelectAllTv.setOnClickListener(this);
        mCancelTv = getActivity().findViewById(R.id.tv_bookshelf_multi_delete_cancel);
        mCancelTv.setOnClickListener(this);
        mDeleteTv = getActivity().findViewById(R.id.tv_bookshelf_multi_delete_delete);
        mDeleteTv.setOnClickListener(this);
    }

    @Override
    protected boolean isRegisterEventBus() {
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCome(Event event) {
        switch (event.getCode()) {
            case EventBusCode.BOOKSHELF_UPDATE_LIST:
                mPresenter.queryAllBook();
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_bookshelf_add:
            case R.id.iv_bookshelf_add:
                // 导入本机小说
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");      // 最近文件（任意类型）
                intent.putExtra(Intent.EXTRA_MIME_TYPES,
                        new String[]{"text/*", "application/epub+zip", "application/octet-stream"});
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
                break;
            case R.id.tv_bookshelf_multi_delete_select_all:
                // 全选
                for (int i = 0; i < mCheckedList.size(); i++) {
                    mCheckedList.set(i, true);
                }
                mBookshelfNovelsAdapter.notifyDataSetChanged();
                break;
            case R.id.tv_bookshelf_multi_delete_cancel:
                // 取消多选删除
                cancelMultiDelete();
                break;
            case R.id.tv_bookshelf_multi_delete_delete:
                // 删除操作
                if (!deleteCheck()) {
                    break;
                }
                new TipDialog.Builder(getActivity())
                        .setContent("确定要删除这些小说吗？")
                        .setCancel("不了")
                        .setEnsure("确定")
                        .setOnClickListener(new TipDialog.OnClickListener() {
                            @Override
                            public void clickEnsure() {
                                multiDelete();
                            }

                            @Override
                            public void clickCancel() {

                            }
                        })
                        .build()
                        .show();
                break;
            default:
                break;
        }
    }

    /**
     * 取消多选删除
     */
    private void cancelMultiDelete() {
        for (int i = 0; i < mCheckedList.size(); i++) {
            mCheckedList.set(i, false);
        }
        mBookshelfNovelsAdapter.setIsMultiDelete(false);
        mBookshelfNovelsAdapter.notifyDataSetChanged();
        mMultiDeleteRv.setVisibility(View.GONE);
    }

    /**
     * 多选删除
     */
    private void multiDelete() {
        mIsDeleting = true;
        for (int i = mDataList.size()-1; i >= 0; i--) {
            if (mCheckedList.get(i)) {
                // 从数据库中删除该小说
                mDbManager.deleteBookshelfNovel(mDataList.get(i).getNovelUrl());
                mDataList.remove(i);
            }
        }
        mCheckedList.clear();
        for (int i = 0; i < mDataList.size(); i++) {
            mCheckedList.add(false);
        }
        mBookshelfNovelsAdapter.setIsMultiDelete(false);
        mBookshelfNovelsAdapter.notifyDataSetChanged();
        mMultiDeleteRv.setVisibility(View.GONE);
        mIsDeleting = false;
    }

    private boolean deleteCheck() {
        for (int i = 0; i < mCheckedList.size(); i++) {
            if (mCheckedList.get(i)) {
                return true;
            }
        }
        showShortToast("请先选定要删除的小说");
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) { // 选择了才继续
            if (data == null) {
                return;
            }
            Uri uri = data.getData();
            if (uri == null) {
                return;
            }
            try {
                getActivity().getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Throwable ignored) {
            }
            File file = FileUtil.uriToReadableFile(getActivity(), uri);
            String filePath = file == null ? "" : file.getPath();
            if (file == null || TextUtils.isEmpty(filePath)) {
                return;
            }

            String fileName = file.getName();
            if (fileName.lastIndexOf(".") < 0) {
                fileName = fileName + ".tmp";
            }
            fileName = fileName.toLowerCase();
            Log.d(TAG, "onActivityResult: fileLen = " + file.length());
            String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);  // 后缀名
            if (suffix.equals("txt")) {
                if (mDbManager.isExistInBookshelfNovel(filePath)) {
                    showShortToast("该小说已导入");
                    return;
                }
                if (FileUtil.getFileSize(file) > 100) {
                    showShortToast("文件过大");
                    return;
                }
                String cleanTitle = LocalBookMetadata.cleanTitle(file.getName());
                // 将该小说的数据存入数据库
                BookshelfNovelDbData dbData = new BookshelfNovelDbData(filePath, cleanTitle,
                        "", 0, 0, 1);
                mDbManager.insertBookshelfNovel(dbData);
                enrichLocalBookMetadata(dbData);
                // 更新列表
                mPresenter.queryAllBook();
            }
            else if (suffix.equals("epub")) {
                if (mDbManager.isExistInBookshelfNovel(filePath)) {
                    showShortToast("该小说已导入");
                    return;
                }
                if (FileUtil.getFileSize(file) > 100) {
                    showShortToast("文件过大");
                    return;
                }
                // 在子线程中解压该 epub 文件
                mLoadingRv.setVisibility(View.VISIBLE);
                mPresenter.unZipEpub(filePath);
            }
            else {
                showShortToast("不支持该类型");
            }
        }
    }

    /**
     * 查询所有书籍信息成功
     */
    @Override
    public void queryAllBookSuccess(List<BookshelfNovelDbData> dataList) {
        if (mBookshelfNovelsAdapter == null) {
            mDataList = dataList;
            mCheckedList.clear();
            for (int i = 0; i < mDataList.size(); i++) {
                mCheckedList.add(false);
            }
            initAdapter();
            mBookshelfNovelsRv.setAdapter(mBookshelfNovelsAdapter);
        } else {
            mDataList.clear();
            mDataList.addAll(dataList);
            mCheckedList.clear();
            for (int i = 0; i < mDataList.size(); i++) {
                mCheckedList.add(false);
            }
            mBookshelfNovelsAdapter.notifyDataSetChanged();
        }
        enrichLocalBooks(dataList);
    }

    private void enrichLocalBooks(List<BookshelfNovelDbData> dataList) {
        for (BookshelfNovelDbData data : dataList) {
            if (data.getType() != 1 && data.getType() != 2) {
                continue;
            }
            String cleanTitle = LocalBookMetadata.cleanTitle(data.getName());
            if (!cleanTitle.equals(data.getName())) {
                mDbManager.updateBookshelfNovelMetadata(data.getNovelUrl(),
                        cleanTitle, data.getCover());
                data.setName(cleanTitle);
                if (mBookshelfNovelsAdapter != null) {
                    mBookshelfNovelsAdapter.notifyDataSetChanged();
                }
            }
            if (data.getCover() == null || data.getCover().isEmpty()) {
                enrichLocalBookMetadata(data);
            }
        }
    }

    private void enrichLocalBookMetadata(final BookshelfNovelDbData data) {
        if (data == null || data.getNovelUrl() == null
                || mEnrichingLocalBooks.contains(data.getNovelUrl())
                || !NetUtil.hasInternet(getActivity())) {
            return;
        }
        mEnrichingLocalBooks.add(data.getNovelUrl());
        LocalBookMetadataFetcher.fetch(data.getName(), new LocalBookMetadataFetcher.Callback() {
            @Override
            public void onSuccess(LocalBookMetadata metadata) {
                String title = metadata.mergeTitle(data.getName());
                String cover = metadata.mergeCover(data.getCover());
                mDbManager.updateBookshelfNovelMetadata(data.getNovelUrl(), title, cover);
                data.setName(title);
                data.setCover(cover);
                if (mBookshelfNovelsAdapter != null) {
                    mBookshelfNovelsAdapter.notifyDataSetChanged();
                }
                mEnrichingLocalBooks.remove(data.getNovelUrl());
            }

            @Override
            public void onError(String errorMsg) {
                mEnrichingLocalBooks.remove(data.getNovelUrl());
            }
        });
    }

    private void initAdapter() {
        mBookshelfNovelsAdapter = new BookshelfNovelsAdapter(getActivity(), mDataList, mCheckedList,
                new BookshelfNovelsAdapter.BookshelfNovelListener() {
                    @Override
                    public void clickItem(int position) {
                        if (!NetUtil.hasInternet(getActivity())) {
                            showShortToast("当前无网络，请检查网络后重试");
                            return;
                        }
                        if (mIsDeleting) {
                            return;
                        }
                        Intent intent = new Intent(getActivity(), ReadActivity.class);
                        // 小说 url
                        intent.putExtra(ReadActivity.KEY_NOVEL_URL, mDataList.get(position).getNovelUrl());
                        // 小说名
                        intent.putExtra(ReadActivity.KEY_NAME, mDataList.get(position).getName());
                        // 小说封面 url
                        intent.putExtra(ReadActivity.KEY_COVER, mDataList.get(position).getCover());
                        // 小说类型
                        intent.putExtra(ReadActivity.KEY_TYPE, mDataList.get(position).getType());
                        // 开始阅读的位置
                        intent.putExtra(ReadActivity.KEY_CHAPTER_INDEX, mDataList.get(position).getChapterIndex());
                        intent.putExtra(ReadActivity.KEY_POSITION, mDataList.get(position).getPosition());
                        intent.putExtra(ReadActivity.KEY_SECOND_POSITION, mDataList.get(position).getSecondPosition());
                        startActivity(intent);
                    }

                    @Override
                    public void longClick(int position) {
                        if (mIsDeleting) {
                            return;
                        }
                        showBookActionMenu(position);
                    }
                });
    }

    /**
     * 查询所有书籍信息失败
     */
    private void showBookActionMenu(final int position) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), mBookshelfNovelsRv);
        popupMenu.getMenu().add(0, 1, 0, "导出 TXT");
        popupMenu.getMenu().add(0, 2, 1, "导出 EPUB");
        popupMenu.getMenu().add(0, 3, 2, "多选删除");
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 1) {
                    exportBook(position, ExportFormat.TXT);
                    return true;
                }
                if (item.getItemId() == 2) {
                    exportBook(position, ExportFormat.EPUB);
                    return true;
                }
                startMultiDelete();
                return true;
            }
        });
        popupMenu.show();
    }

    private void startMultiDelete() {
        mBookshelfNovelsAdapter.setIsMultiDelete(true);
        mBookshelfNovelsAdapter.notifyDataSetChanged();
        mMultiDeleteRv.setVisibility(View.VISIBLE);
    }

    private void exportBook(final int position, final ExportFormat format) {
        if (position < 0 || position >= mDataList.size()) {
            return;
        }
        mLoadingRv.setVisibility(View.VISIBLE);
        final BookshelfNovelDbData book = mDataList.get(position);
        new Thread(new Runnable() {
            @Override
            public void run() {
                BookExportResult result = null;
                String error = "";
                try {
                    result = BookExporter.export(getActivity(), book, format);
                } catch (Throwable t) {
                    error = t.getMessage() == null ? "导出失败" : t.getMessage();
                }
                final BookExportResult finalResult = result;
                final String finalError = error;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingRv.setVisibility(View.GONE);
                        if (finalResult != null) {
                            mPresenter.queryAllBook();
                            showShortToast("已导出并导入书架");
                        } else {
                            showShortToast(finalError);
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void queryAllBookError(String errorMsg) {

    }

    /**
     * 解压 epub 文件成功
     */
    @Override
    public void unZipEpubSuccess(String filePath, OpfData opfData) {
        // 将书籍信息写入数据库
        File file = new File(filePath);
        String cleanTitle = LocalBookMetadata.cleanTitle(file.getName());
        BookshelfNovelDbData dbData = new BookshelfNovelDbData(filePath, cleanTitle,
                opfData.getCover(), 0, 0, 2);
        mDbManager.insertBookshelfNovel(dbData);
        if (opfData.getCover() == null || opfData.getCover().isEmpty()) {
            enrichLocalBookMetadata(dbData);
        }
        // 更新列表
        mPresenter.queryAllBook();

        mLoadingRv.setVisibility(View.GONE);
        Log.d(TAG, "unZipEpubSuccess: opfData = " + opfData);
        showShortToast("导入成功");
    }

    /**
     * 解压 epub 文件失败
     */
    @Override
    public void unZipEpubError(String errorMsg) {
        mLoadingRv.setVisibility(View.GONE);
        Log.d(TAG, "unZipEpubError: " + errorMsg);
        showShortToast("导入失败");
    }
}
