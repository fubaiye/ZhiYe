package com.feng.freader.view.fragment.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.feng.freader.R;
import com.feng.freader.adapter.BookshelfNovelsAdapter;
import com.feng.freader.base.BaseFragment;
import com.feng.freader.base.BasePresenter;
import com.feng.freader.bookshelf.BookshelfMetaManager;
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
    private static final int REQUEST_IMPORT_BOOK = 1;
    private static final int REQUEST_PICK_COVER = 2;

    private RecyclerView mBookshelfNovelsRv;
    private TextView mLocalAddTv;
    private ImageView mLocalAddIv;
    private RelativeLayout mLoadingRv;

    private RelativeLayout mMultiDeleteRv;
    private TextView mSelectAllTv;
    private TextView mCancelTv;
    private TextView mDeleteTv;
    private View mContinueCard;
    private ImageView mContinueCoverIv;
    private TextView mContinueNameTv;
    private TextView mContinueProgressTv;

    private List<BookshelfNovelDbData> mDataList = new ArrayList<>();
    private List<Boolean> mCheckedList = new ArrayList<>();
    private BookshelfNovelsAdapter mBookshelfNovelsAdapter;
    private boolean mIsDeleting = false;
    private final Set<String> mEnrichingLocalBooks = new HashSet<>();

    private DatabaseManager mDbManager;
    private BookshelfMetaManager mBookshelfMetaManager;
    private String mPendingCoverNovelUrl;

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
        mBookshelfMetaManager = new BookshelfMetaManager(getActivity());
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
        mContinueCard = getActivity().findViewById(R.id.cl_bookshelf_continue);
        mContinueCard.setOnClickListener(this);
        mContinueCoverIv = getActivity().findViewById(R.id.iv_bookshelf_continue_cover);
        mContinueNameTv = getActivity().findViewById(R.id.tv_bookshelf_continue_name);
        mContinueProgressTv = getActivity().findViewById(R.id.tv_bookshelf_continue_progress);
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
                startActivityForResult(intent, REQUEST_IMPORT_BOOK);
                break;
            case R.id.cl_bookshelf_continue:
                if (!mDataList.isEmpty()) {
                    openBook(0);
                }
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
            if (requestCode == REQUEST_PICK_COVER) {
                updatePickedCover(uri);
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
    private void updatePickedCover(Uri uri) {
        if (TextUtils.isEmpty(mPendingCoverNovelUrl)) {
            return;
        }
        try {
            getActivity().getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {
        }
        for (BookshelfNovelDbData book : mDataList) {
            if (mPendingCoverNovelUrl.equals(book.getNovelUrl())) {
                String cover = uri.toString();
                mDbManager.updateBookshelfNovelMetadata(book.getNovelUrl(), book.getName(), cover);
                book.setCover(cover);
                if (mBookshelfNovelsAdapter != null) {
                    mBookshelfNovelsAdapter.notifyDataSetChanged();
                }
                showShortToast("封面已更新");
                break;
            }
        }
        mPendingCoverNovelUrl = null;
    }

    @Override
    public void queryAllBookSuccess(List<BookshelfNovelDbData> dataList) {
        dataList = mBookshelfMetaManager.sort(dataList, BookshelfMetaManager.SORT_DEFAULT);
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
        updateContinueReadingCard();
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

    private void updateContinueReadingCard() {
        if (mContinueNameTv == null || mContinueProgressTv == null || mContinueCoverIv == null) {
            return;
        }
        if (mDataList == null || mDataList.isEmpty()) {
            mContinueNameTv.setText("还没有正在阅读的书");
            mContinueProgressTv.setText("导入一本书后会显示阅读进度");
            mContinueCoverIv.setImageResource(R.drawable.default_cover);
            return;
        }
        BookshelfNovelDbData book = mDataList.get(0);
        mContinueNameTv.setText(book.getName());
        int chapter = Math.max(0, book.getChapterIndex()) + 1;
        mContinueProgressTv.setText("阅读到：第 " + chapter + " 章");
        String cover = book.getCover() == null ? "" : book.getCover();
        if (book.getType() == 1) {
            mContinueCoverIv.setImageResource(R.drawable.local_txt);
        } else if (book.getType() == 2 && cover.length() == 0) {
            mContinueCoverIv.setImageResource(R.drawable.local_epub);
        } else if (cover.startsWith("http") || cover.startsWith("content:") || cover.startsWith("file:")) {
            Glide.with(getActivity())
                    .load(cover)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.default_cover)
                            .error(R.drawable.default_cover))
                    .into(mContinueCoverIv);
        } else if (cover.length() > 0) {
            Bitmap bitmap = FileUtil.loadLocalPicture(cover);
            if (bitmap != null) {
                mContinueCoverIv.setImageBitmap(bitmap);
            } else {
                mContinueCoverIv.setImageResource(R.drawable.default_cover);
            }
        } else {
            mContinueCoverIv.setImageResource(R.drawable.default_cover);
        }
    }

    private void openBook(int position) {
        if (position < 0 || position >= mDataList.size()) {
            return;
        }
        if (mDataList.get(position).getType() == 0 && !NetUtil.hasInternet(getActivity())) {
            showShortToast("当前无网络，请检查网络后重试");
            return;
        }
        if (mIsDeleting) {
            return;
        }
        BookshelfNovelDbData book = mDataList.get(position);
        mBookshelfMetaManager.markRecent(book.getNovelUrl());
        Intent intent = new Intent(getActivity(), ReadActivity.class);
        intent.putExtra(ReadActivity.KEY_NOVEL_URL, book.getNovelUrl());
        intent.putExtra(ReadActivity.KEY_NAME, book.getName());
        intent.putExtra(ReadActivity.KEY_COVER, book.getCover());
        intent.putExtra(ReadActivity.KEY_TYPE, book.getType());
        intent.putExtra(ReadActivity.KEY_CHAPTER_INDEX, book.getChapterIndex());
        intent.putExtra(ReadActivity.KEY_POSITION, book.getPosition());
        intent.putExtra(ReadActivity.KEY_SECOND_POSITION, book.getSecondPosition());
        startActivity(intent);
    }

    private void initAdapter() {
        mBookshelfNovelsAdapter = new BookshelfNovelsAdapter(getActivity(), mDataList, mCheckedList,
                new BookshelfNovelsAdapter.BookshelfNovelListener() {
                    @Override
                    public void clickItem(int position) {
                        if (mDataList.get(position).getType() == 0
                                && !NetUtil.hasInternet(getActivity())) {
                            showShortToast("当前无网络，请检查网络后重试");
                            return;
                        }
                        if (mIsDeleting) {
                            return;
                        }
                        mBookshelfMetaManager.markRecent(mDataList.get(position).getNovelUrl());
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
        popupMenu.getMenu().add(0, 10, 0, "编辑书籍");
        popupMenu.getMenu().add(0, 1, 1, "导出 TXT");
        popupMenu.getMenu().add(0, 2, 2, "导出 EPUB");
        popupMenu.getMenu().add(0, 3, 3, "多选删除");
        popupMenu.getMenu().add(0, 4, 4, "置顶/取消置顶");
        popupMenu.getMenu().add(0, 5, 5, "收藏/取消收藏");
        popupMenu.getMenu().add(0, 6, 6, "设置分类");
        popupMenu.getMenu().add(0, 7, 7, "按最近排序");
        popupMenu.getMenu().add(0, 8, 8, "按收藏排序");
        popupMenu.getMenu().add(0, 9, 9, "按进度排序");
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 10) {
                    showEditBookDialog(position);
                    return true;
                }
                if (item.getItemId() == 1) {
                    exportBook(position, ExportFormat.TXT);
                    return true;
                }
                if (item.getItemId() == 2) {
                    exportBook(position, ExportFormat.EPUB);
                    return true;
                }
                if (item.getItemId() == 4) {
                    mBookshelfMetaManager.togglePinned(mDataList.get(position).getNovelUrl());
                    applyBookshelfSort(BookshelfMetaManager.SORT_DEFAULT);
                    return true;
                }
                if (item.getItemId() == 5) {
                    mBookshelfMetaManager.toggleFavorite(mDataList.get(position).getNovelUrl());
                    applyBookshelfSort(BookshelfMetaManager.SORT_DEFAULT);
                    return true;
                }
                if (item.getItemId() == 6) {
                    showCategoryDialog(position);
                    return true;
                }
                if (item.getItemId() == 7) {
                    applyBookshelfSort(BookshelfMetaManager.SORT_RECENT);
                    return true;
                }
                if (item.getItemId() == 8) {
                    applyBookshelfSort(BookshelfMetaManager.SORT_FAVORITE);
                    return true;
                }
                if (item.getItemId() == 9) {
                    applyBookshelfSort(BookshelfMetaManager.SORT_PROGRESS);
                    return true;
                }
                startMultiDelete();
                return true;
            }
        });
        popupMenu.show();
    }

    private void showEditBookDialog(final int position) {
        if (position < 0 || position >= mDataList.size()) {
            return;
        }
        final String[] actions = new String[]{"修改书名", "更换封面", "删除书籍"};
        new AlertDialog.Builder(getActivity())
                .setTitle(mDataList.get(position).getName())
                .setItems(actions, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which == 0) {
                            showRenameBookDialog(position);
                        } else if (which == 1) {
                            pickBookCover(position);
                        } else {
                            confirmDeleteBook(position);
                        }
                    }
                })
                .show();
    }

    private void showRenameBookDialog(final int position) {
        if (position < 0 || position >= mDataList.size()) {
            return;
        }
        final BookshelfNovelDbData book = mDataList.get(position);
        final EditText editText = new EditText(getActivity());
        editText.setSingleLine(true);
        editText.setText(book.getName());
        editText.setSelection(editText.length());
        new AlertDialog.Builder(getActivity())
                .setTitle("修改书名")
                .setView(editText)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        String newName = editText.getText().toString().trim();
                        if (TextUtils.isEmpty(newName)) {
                            showShortToast("书名不能为空");
                            return;
                        }
                        mDbManager.updateBookshelfNovelMetadata(book.getNovelUrl(), newName, book.getCover());
                        book.setName(newName);
                        if (mBookshelfNovelsAdapter != null) {
                            mBookshelfNovelsAdapter.notifyDataSetChanged();
                        }
                        showShortToast("书名已更新");
                    }
                })
                .show();
    }

    private void pickBookCover(final int position) {
        if (position < 0 || position >= mDataList.size()) {
            return;
        }
        mPendingCoverNovelUrl = mDataList.get(position).getNovelUrl();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_PICK_COVER);
    }

    private void confirmDeleteBook(final int position) {
        if (position < 0 || position >= mDataList.size()) {
            return;
        }
        new TipDialog.Builder(getActivity())
                .setContent("确定要删除《" + mDataList.get(position).getName() + "》吗？")
                .setCancel("不了")
                .setEnsure("删除")
                .setOnClickListener(new TipDialog.OnClickListener() {
                    @Override
                    public void clickEnsure() {
                        deleteSingleBook(position);
                    }

                    @Override
                    public void clickCancel() {
                    }
                })
                .build()
                .show();
    }

    private void deleteSingleBook(int position) {
        if (position < 0 || position >= mDataList.size()) {
            return;
        }
        mDbManager.deleteBookshelfNovel(mDataList.get(position).getNovelUrl());
        mDataList.remove(position);
        if (position < mCheckedList.size()) {
            mCheckedList.remove(position);
        }
        if (mBookshelfNovelsAdapter != null) {
            mBookshelfNovelsAdapter.notifyDataSetChanged();
        }
        showShortToast("已删除");
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

    private void showCategoryDialog(final int position) {
        if (position < 0 || position >= mDataList.size()) {
            return;
        }
        final EditText editText = new EditText(getActivity());
        editText.setHint("例如：玄幻、都市、待读");
        new AlertDialog.Builder(getActivity())
                .setTitle("设置分类")
                .setView(editText)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        mBookshelfMetaManager.setCategory(mDataList.get(position).getNovelUrl(),
                                editText.getText().toString());
                        applyBookshelfSort(BookshelfMetaManager.SORT_DEFAULT);
                    }
                })
                .show();
    }

    private void applyBookshelfSort(int mode) {
        List<BookshelfNovelDbData> sorted = mBookshelfMetaManager.sort(mDataList, mode);
        mDataList.clear();
        mDataList.addAll(sorted);
        mCheckedList.clear();
        for (int i = 0; i < mDataList.size(); i++) {
            mCheckedList.add(false);
        }
        if (mBookshelfNovelsAdapter != null) {
            mBookshelfNovelsAdapter.notifyDataSetChanged();
        }
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
