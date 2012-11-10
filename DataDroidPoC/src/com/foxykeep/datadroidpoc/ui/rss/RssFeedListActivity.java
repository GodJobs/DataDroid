/**
 * 2011 Foxykeep (http://datadroid.foxykeep.com)
 * <p>
 * Licensed under the Beerware License : <br />
 * As long as you retain this notice you can do whatever you want with this stuff. If we meet some
 * day, and you think this stuff is worth it, you can buy me a beer in return
 */

package com.foxykeep.datadroidpoc.ui.rss;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.foxykeep.datadroid.model.RssFeed;
import com.foxykeep.datadroid.model.RssItem;
import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.requestmanager.RequestManager.RequestListener;
import com.foxykeep.datadroidpoc.R;
import com.foxykeep.datadroidpoc.data.requestmanager.PoCRequestFactory;
import com.foxykeep.datadroidpoc.dialogs.ConnexionErrorDialogFragment;
import com.foxykeep.datadroidpoc.ui.DataDroidActivity;

public final class RssFeedListActivity extends DataDroidActivity implements RequestListener,
        OnClickListener, OnItemClickListener {

    private Spinner mSpinnerFeedUrl;
    private Button mButtonLoad;
    private Button mButtonClearMemory;
    private ListView mListView;
    private RssItemListAdapter mListAdapter;

    private String[] mFeedUrlArray;

    private LayoutInflater mInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.rss_feed_list);
        bindViews();

        mFeedUrlArray = getResources().getStringArray(R.array.rss_feed_url);

        mInflater = getLayoutInflater();

        Object data = getLastNonConfigurationInstance();
        if (data != null) {
            RetainData retainData = (RetainData) data;

            if (retainData.rssItemArray != null & retainData.rssItemArray.length > 0) {
                mListAdapter.setNotifyOnChange(false);
                for (RssItem rssItem : retainData.rssItemArray) {
                    mListAdapter.add(rssItem);
                }
                mListAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (int i = 0, length = mRequestList.size(); i < length; i++) {
            Request request = mRequestList.get(i);

            if (mRequestManager.isRequestInProgress(request)) {
                mRequestManager.addRequestListener(this, request);
                setProgressBarIndeterminateVisibility(true);
            } else {
                mRequestManager.callListenerWithCachedData(this, request);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!mRequestList.isEmpty()) {
            mRequestManager.removeRequestListener(this);
        }
    }

    class RetainData {
        public RssItem[] rssItemArray;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        int count = mListAdapter.getCount();

        RetainData retainData = new RetainData();
        retainData.rssItemArray = new RssItem[count];

        for (int i = 0; i < count; i++) {
            retainData.rssItemArray[i] = mListAdapter.getItem(i);
        }

        return retainData;
    }

    private void bindViews() {
        mSpinnerFeedUrl = (Spinner) findViewById(R.id.sp_url);

        mButtonLoad = (Button) findViewById(R.id.b_load);
        mButtonLoad.setOnClickListener(this);

        mButtonClearMemory = (Button) findViewById(R.id.b_clear_memory);
        mButtonClearMemory.setOnClickListener(this);

        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setAdapter(new RssItemListAdapter(this));
        mListView.setOnItemClickListener(this);
    }

    private void callRssFeedWS() {
        (mListAdapter).clear();
        setProgressBarIndeterminateVisibility(true);

        Request request = PoCRequestFactory.createGetRssFeedRequest(
                mFeedUrlArray[mSpinnerFeedUrl.getSelectedItemPosition()]);
        mRequestManager.execute(request, this);
        mRequestList.add(request);
    }

    @Override
    public void onClick(View view) {
        if (view == mButtonLoad) {
            callRssFeedWS();
        } else if (view == mButtonClearMemory) {
            (mListAdapter).clear();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        RssItem rssItem = ((RssItemListAdapter) parent.getAdapter()).getItem(position);
        if (rssItem != null) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(rssItem.link)));
        }
    }

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {
        if (mRequestList.contains(request)) {
            setProgressBarIndeterminateVisibility(false);
            mRequestList.remove(request);

            RssFeed rssFeed = resultData
                    .getParcelable(PoCRequestFactory.BUNDLE_EXTRA_RSS_FEED_DATA);

            mListAdapter.setNotifyOnChange(false);
            for (RssItem rssItem : rssFeed.rssItemList) {
                mListAdapter.add(rssItem);
            }
            mListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestConnectionError(Request request) {
        if (mRequestList.contains(request)) {
            setProgressBarIndeterminateVisibility(false);
            mRequestList.remove(request);

            ConnexionErrorDialogFragment.show(this, request, this);
        }
    }

    @Override
    public void onRequestDataError(Request request) {
        if (mRequestList.contains(request)) {
            setProgressBarIndeterminateVisibility(false);
            mRequestList.remove(request);

            showBadDataErrorDialog();
        }
    }

    class ViewHolder {
        private TextView mTextViewTitle;
        private TextView mTextViewDescription;

        public ViewHolder(View view) {
            mTextViewTitle = (TextView) view.findViewById(R.id.tv_title);
            mTextViewDescription = (TextView) view.findViewById(R.id.tv_description);
        }

        public void populateViews(RssItem rssItem) {
            mTextViewTitle.setText(rssItem.title);
            mTextViewDescription.setText(Html.fromHtml(rssItem.description));
        }
    }

    class RssItemListAdapter extends ArrayAdapter<RssItem> {

        public RssItemListAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.rss_feed_list_item, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.populateViews(getItem(position));

            return convertView;
        }
    }
}
