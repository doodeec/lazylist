package com.doodeec.lazylist;

import android.view.View;
import android.widget.TextView;

/**
 * Created by Dusan Doodeec Bartos on 7.11.2014.
 *
 * Default holder for Lazy ListView
 * @see com.doodeec.lazylist.LazyListFragment
 * @see com.doodeec.lazylist.LazyAdapter
 */
@SuppressWarnings("unused")
public class LazyListHolder {
    private TextView mText;

    protected LazyListHolder(View v) {
        mText = (TextView) v.findViewById(android.R.id.text1);
        if (mText == null) {
            throw new AssertionError("Lazy List holder has incorrect layout");
        }
    }

    public void setText(String text) {
        mText.setText(text);
    }
}
