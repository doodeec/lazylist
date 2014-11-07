package com.doodeec.lazylist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Dusan Doodeec Bartos on 7.11.2014.
 *
 * Lazy loading list adapter
 */
public class LazyAdapter<T> extends BaseAdapter {

    protected List<T> mData;
    protected LayoutInflater mInflater;
    protected WeakReference<LazyListFragment> mLazyList;

    public LazyAdapter(List<T> data, LazyListFragment list) {
        mData = data;
        mLazyList = new WeakReference<LazyListFragment>(list);

        mInflater = (LayoutInflater) list.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * Subclasses should override this method to show custom view
     * @see android.widget.BaseAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @SuppressWarnings("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        LazyListHolder holder;

        // init holder pattern
        if (convertView == null) {
            v = mInflater.inflate(android.R.layout.simple_list_item_1, null);
            holder = new LazyListHolder(v);
            v.setTag(holder);
        }

        holder = (LazyListHolder) v.getTag();

        holder.setText(String.valueOf(mData.get(position)));
        return v;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public T getItem(int position) {
        return mData.get(position);
    }
}
