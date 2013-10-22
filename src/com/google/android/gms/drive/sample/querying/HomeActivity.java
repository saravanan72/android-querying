/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.drive.sample.querying;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.OnChildrenRetrievedCallback;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity that demonstrates sample queries to filter the files on the
 * currently authenticated user's Google Drive.
 *
 * @author jbd@google.com (Burcu Dogan)
 */
public class HomeActivity extends BaseDriveActivity
    implements OnItemClickListener, OnChildrenRetrievedCallback {

  private static String TAG = "HomeActivity";

  private static Query[] sQueries = new Query[] {
    // files not shared with me
    new Query.Builder().addFilters(Filters.not(Filters.sharedWithMe())).build(),

    // files shared with me
    new Query.Builder().addFilters(Filters.sharedWithMe()).build(),

    // files with text/plain mimetype
    new Query.Builder().addFilters(Filters.eq(SearchableField.mimeType(), "text/plain")).build(),

    // files with a title containing 'a'
    new Query.Builder().addFilters(Filters.contains(SearchableField.title(), "a")).build(),

    // files starred and with text/plain mimetype
    new Query.Builder().addFilters(Filters.and(
        Filters.eq(SearchableField.mimeType(), "text/plain"),
        Filters.eq(SearchableField.starred(), "true"))).build(),

    // files on the root folder or full text contains 'Hello'
    new Query.Builder().addFilters(Filters.or(
        Filters.in(SearchableField.parents(), "root"),
        Filters.eq(SearchableField.mimeType(), "text/plain"))).build()
  };

  /**
   * User friendly titles for available queries.
   */
  private String[] mTitles;

  /**
   * Main drawer layout.
   */
  private DrawerLayout mMainDrawerLayout;

  /**
   * List view that displays the available queries.
   */
  private ListView mListViewQueries;

  /**
   * List view that displays the query results.
   */
  private ListView mListViewFiles;

  /**
   * Index of the selected query.
   */
  private int mSelectedIndex = 0;

  /**
   * Retrieved metadata results buffer or {@code null} if there are
   * no results retrieved yet.
   */
  private MetadataBuffer mMetadataBuffer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    mTitles = getResources().getStringArray(R.array.titles_array);

    mMainDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayoutMain);
    mListViewQueries = (ListView) findViewById(R.id.listViewQueries);
    mListViewQueries.setOnItemClickListener(this);

    mListViewFiles = (ListView) findViewById(R.id.listViewFiles);
    mListViewFiles.setAdapter(new ResultsAdapter());
    mListViewFiles.setEmptyView(findViewById(R.id.viewEmpty));
  }

  /**
   * Called when {@code GoogleApiClient} is connected, no querying or client related
   * actions other than disconnection should be invoked before.
   */
  @Override
  public void onConnected(Bundle connectionHint) {
    super.onConnected(connectionHint);
    mListViewQueries.setAdapter(
        new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mTitles));
    refresh();
  }

  /**
   * Invokes calls to query user's Google Drive root folder's children
   * with the currently selected query.
   */
  private void refresh() {
    DriveFolder root = Drive.DriveApi.getRootFolder();
    root.queryChildren(mGoogleApiClient, sQueries[mSelectedIndex]).addResultCallback(this);
  }

  /**
   * Called when user clicks on one of the items on the queries list.
   * Keep the query's index and invoke to filter the user's Drive with
   * the selected query.
   */
  @Override
  public void onItemClick(AdapterView<?> arg0, View arg1, int i, long arg3) {
    mMainDrawerLayout.closeDrawers();
    mSelectedIndex = i;
    refresh();
  }

  /**
   * Called when query has executed and a result has been retrieved.
   * Files list view should be re-rendered with the new results.
   */
  @Override
  public void onChildrenRetrieved(MetadataBufferResult result) {
    if (!result.getStatus().isSuccess()) {
      Toast.makeText(this, R.string.msg_errorretrieval, Toast.LENGTH_SHORT).show();
      return;
    }
    Log.d(TAG, "Retrieved file count: " + result.getMetadataBuffer().getCount());
    mMetadataBuffer = result.getMetadataBuffer();
    ((ResultsAdapter)mListViewFiles.getAdapter()).notifyDataSetChanged();
  }

  /**
   * List adapter to provide data to the files list view. If there are
   * no results yet retrieved, it shows no items.
   */
  private class ResultsAdapter extends BaseAdapter {

    /**
     * Returns the number of the items in {@code mMetaBuffer}, returns 0
     * if it is {@code null}.
     */
    @Override
    public int getCount() {
      if (mMetadataBuffer == null) {
        return 0;
      }
      return mMetadataBuffer.getCount();
    }

    /**
     * Returns the item at the ith position.
     */
    @Override
    public Object getItem(int i) {
      return mMetadataBuffer.get(i);
    }

    /**
     * Returns the id of the item at the ith position.
     */
    @Override
    public long getItemId(int i) {
      return i;
    }

    /**
     * Inflates the row view for the item at the ith position, renders it
     * with the corresponding item.
     */
    @Override
    public View getView(int i, View convertView, ViewGroup arg2) {
      if (convertView == null) {
        convertView = View.inflate(getBaseContext(), R.layout.row_file, null);
      }
      TextView titleTextView = (TextView) convertView.findViewById(R.id.textViewTitle);
      TextView descTextView = (TextView) convertView.findViewById(R.id.textViewDescription);
      Metadata metadata = mMetadataBuffer.get(i);

      titleTextView.setText(metadata.getTitle());
      descTextView.setText(metadata.getModifiedDate().toString());
      return convertView;
    }
  }
}
