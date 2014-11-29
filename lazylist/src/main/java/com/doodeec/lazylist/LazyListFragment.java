package com.doodeec.lazylist;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dusan Doodeec Bartos on 26.10.2014.
 *
 * Lazy loading list fragment
 * Extends {@link android.support.v4.app.ListFragment}
 * Features loading indicator at the bottom of the list to enable lazy loading of data
 */
@SuppressWarnings("unused")
public class LazyListFragment<Type> extends ListFragment {

    public static final int REASON_LIST_END = 1;
    public static final int REASON_SERVER_ERROR = 2;
    public static final int REASON_CONNECTION_LOST = 4;
    public static final int REASON_REQUEST_CANCELLED = 8;

    private static final float SHOWN_LOADER_POSITION = 0;
    private static final float SHOWN_LOADER_ALPHA = 1f;
    private static final float HIDDEN_LOADER_ALPHA = 0;
    private static final int LOADER_ANIMATION_DURATION = 300;

    private static final String SAVED_PAGE = "Current_page";
    private static final String REPEAT_DIALOG_BUNDLE = "Repeat_dialog";

    protected Integer maxDataLength;
    // last currently loaded page
    protected int mPage = 0;
    protected boolean mLoading = false;
    protected boolean mBlockLazyLoad = false;
    protected final List<Type> mData = new ArrayList<Type>();
    protected LazyAdapter mAdapter;
    protected RelativeLayout mProgress;
    protected AlertDialog mRepeatDialog;

    private final AbsListView.OnScrollListener mScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (!mBlockLazyLoad && !mLoading && visibleItemCount > 0 && firstVisibleItem + visibleItemCount == totalItemCount) {
                loadPage(++mPage);
            }
        }
    };

    public LazyListFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.lazy_fragment, container, false);
        mProgress = (RelativeLayout) v.findViewById(R.id.loading_progress);

        if (mProgress == null) {
            throw new AssertionError("Lazy list has invalid layout defined");
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            setLoadingProgress(false);
            mPage = savedInstanceState.getInt(SAVED_PAGE);
            reviveData();

            if (savedInstanceState.getBoolean(REPEAT_DIALOG_BUNDLE)) {
                showRepeatDialog(mPage + 1);
            }
        } else {
            setLoadingProgress(true);
            initData();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_PAGE, mPage);
        outState.putBoolean(REPEAT_DIALOG_BUNDLE, mRepeatDialog != null);
        if (mRepeatDialog != null) {
            mRepeatDialog.dismiss();
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Subclasses should override this method to specify adapter at this point
     */
    protected void initAdapter() {
        mAdapter = new LazyAdapter<Type>(mData, LazyListFragment.this);
    }

    /**
     * Initializes data list, initializes and sets the adapter
     */
    private void initData() {
        mData.clear();
        initAdapter();
        setListAdapter(mAdapter);
        getListView().setOnScrollListener(mScrollListener);
    }

    /**
     * Revives currently available data and last page number
     */
    protected void reviveData() {
        mData.clear();

        // can not call initAdapter directly, as it can be overridden to do different stuff
        mAdapter = new LazyAdapter<Type>(mData, LazyListFragment.this);
        setMaxDataLength(null);

        setListAdapter(mAdapter);
        getListView().setOnScrollListener(mScrollListener);
    }

    /**
     * Reloads all the data completely
     */
    public void reloadData() {
        setMaxDataLength(null);
        initData();
    }

    /**
     * Subclasses should override this method to load specific page - data/URL
     *
     * @param page page to load
     */
    protected synchronized void loadPage(int page) {
        mLoading = true;
        setLoadingProgress(true);
    }

    /**
     * Notifies adapter when page was loaded
     * Should be called from loadPage callback, so it supports asynchronous loading
     *
     * @param pageNumber page loaded
     */
    protected synchronized void onDataLoadingCompleted(int pageNumber) {
        mLoading = false;
        mPage = pageNumber;

        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setLoadingProgress(false);
                    mAdapter.notifyDataSetChanged();
                    checkMaxDataLength();
                }
            });
        }
    }

    /**
     * Loading failed
     * Should be called from loadPage callback, if asynchronous call was not successful
     *
     * @param reason reason ID
     */
    protected synchronized void onDataLoadingFailed(int reason, final Integer page) {
        mLoading = false;
        boolean showRepeatDialog = false;

        switch (reason) {
            case REASON_LIST_END:
                Toast.makeText(getActivity(), R.string.lazy_load_list_end, Toast.LENGTH_SHORT).show();
                mBlockLazyLoad = true;
                break;
            case REASON_SERVER_ERROR:
                Toast.makeText(getActivity(), R.string.lazy_load_server_error_msg, Toast.LENGTH_SHORT).show();
                break;
            case REASON_CONNECTION_LOST:
                showRepeatDialog = true;
                break;
            case REASON_REQUEST_CANCELLED:
                Toast.makeText(getActivity(), R.string.lazy_load_cancelled, Toast.LENGTH_SHORT).show();
                break;
            default:
                showRepeatDialog = true;
                Toast.makeText(getActivity(), R.string.lazy_load_unknown_error, Toast.LENGTH_SHORT).show();
                break;
        }

        final boolean showRepeatDialogDelegate = showRepeatDialog;
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setLoadingProgress(false);

                    //prompt request repeat
                    if (page != null && showRepeatDialogDelegate) {
                        showRepeatDialog(page);
                    }
                }
            });
        }
    }

    /**
     * Sets maximal data length to be possible to load with lazy loading
     * If data length oversizes max length, lazy loading by scroll will be further disabled
     *
     * @param maxLength max length to set
     */
    public void setMaxDataLength(Integer maxLength) {
        maxDataLength = maxLength;
        mBlockLazyLoad = maxDataLength != null && mData.size() <= maxLength;
    }

    /**
     * If max length is defined,
     */
    private void checkMaxDataLength() {
        if (maxDataLength != null && mData.size() >= maxDataLength) {
            mBlockLazyLoad = true;
            Toast.makeText(getActivity(), R.string.lazy_load_list_end, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows/hides loading progress
     * For old API it's just changing visibility
     * New APIs use animation
     *
     * @param visible true to show progress, false to hide
     */
    @SuppressLint("NewApi")
    protected void setLoadingProgress(boolean visible) {
        if (visible) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                mProgress.animate()
                        .translationY(SHOWN_LOADER_POSITION)
                        .alpha(SHOWN_LOADER_ALPHA)
                        .setDuration(LOADER_ANIMATION_DURATION);
            }
            mProgress.setVisibility(View.VISIBLE);
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                mProgress.animate()
                        .translationY(mProgress.getHeight())
                        .alpha(HIDDEN_LOADER_ALPHA)
                        .setDuration(LOADER_ANIMATION_DURATION);
            } else {
                mProgress.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Shows dialog that prompts to repeat unsuccessful request
     *
     * @param page page request was made to
     */
    private void showRepeatDialog(final int page) {
        if (getActivity() != null) {
            mRepeatDialog = new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.lazy_load_repeat_req_msg)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            loadPage(page);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
            mRepeatDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mRepeatDialog = null;
                }
            });
        }
    }
}