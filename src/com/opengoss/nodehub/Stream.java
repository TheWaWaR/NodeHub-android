package com.opengoss.nodehub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.opengoss.nodehub.utils.API;


public abstract class Stream extends Activity {
	
	protected boolean hasMore = true;
	protected boolean notNotified = true;
	protected String firstEvent = null;
	protected String lastEvent = null;
	protected int type;
	protected String thread = null;
	protected String nodeId = "-1"; // -1 means home
	protected String severity = "";

	// UI things
	protected ListView lv;
	protected List<HashMap<String, String>> lvFillMaps;
	protected SimpleAdapter lvAdapter;
	protected ProgressDialog pd;

	protected abstract int initStream();
	public abstract void onItemClick(View v);

	// Menu setp 2
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		if (this instanceof NodeShow) {
			return;
		}
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.stream_ctx, menu);
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		View targetView = info.targetView;
		String nodeName = ((TextView)(targetView.findViewById(R.id.item_node))).getText().toString();
		menu.findItem(R.id.menu_show_item).setTitle("进入节点：" + nodeName);
	}

	// Menu setp 3
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (this instanceof NodeShow) {
			return false;
		}
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.menu_show_item:
			TextView tv = (TextView) info.targetView.findViewById(R.id.item_nid);
			String nid = tv.getText().toString();
			Intent i = new Intent(this, NodeShow.class);
			i.putExtra("nodeId", nid);
			startActivity(i);	
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	protected void init() {
		initListView();
		int eventsLen = initStream();
		if (eventsLen > 0) {
			registerListViewListener();
			// Menu setp 1
			this.registerForContextMenu(lv);
		}
	}

	protected void initListView() {
		lv = (ListView) findViewById(R.id.itemlist);

		// create the grid item mapping
		String[] from = new String[] { "nid", "node", "time", "title", "body" };
		int[] to = new int[] { R.id.item_nid, R.id.item_node, R.id.item_time,
				R.id.item_title, R.id.item_body };

		// prepare the list of all records
		lvFillMaps = new ArrayList<HashMap<String, String>>();
		// fill in the grid_item layout
		lvAdapter = new SimpleAdapter(this, lvFillMaps, R.layout.stream_item,
				from, to);
		lv.setAdapter(lvAdapter);
	}

	private void registerListViewListener() {
		// 1. Scroll
		lv.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// Check if the last view is visible
				if (++firstVisibleItem + visibleItemCount > totalItemCount) {
					// load more content
					if (hasMore) {
						loadItems(API.LOAD_OLD);
					} else if (notNotified) {
						Toast.makeText(Stream.this,getString(R.string.no_more), Toast.LENGTH_SHORT).show();
						notNotified = false;
					}
				}
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
			}
		});
	}

	protected int loadItems(int loadType) {
		String ahchor = "";
		if (loadType == API.LOAD_OLD) {
			ahchor = lastEvent;
		} else {
			ahchor = firstEvent;
		}

		JSONObject data = null;
		if (type == API.TYPE_NODE) {
			data = API.loadEvents(nodeId, severity, ahchor, loadType);
		} else if (type == API.TYPE_THREAD) {
			data = API.loadThreadEvents(thread, severity, ahchor, loadType);
		}
		
		if (data == null) {
			Toast.makeText(this, getString(R.string.error_connect),Toast.LENGTH_SHORT).show();
			return 0;
		}else {			
			return updateEvents(data, loadType);
		}
	}

	protected int updateEvents(JSONObject data, int loadType) {
		int eventsLen = 0;
		try {
			JSONArray events = data.getJSONArray("events");
			eventsLen = events.length();
			for (int i = 0; i <= eventsLen - 1; i++) {
				JSONObject e = (JSONObject) events.get(i);

				String nid = e.getString("node_id"); // node id
				String title = e.getString("title");
				String body = e.getString("body");
				String time = e.getString("raised_at");
				if (body == "null") {
					body = "";
				}

				HashMap<String, String> item = new HashMap<String, String>();
				item.put("nid", nid);
				item.put("title", title);
				item.put("body", body);
				item.put("time", time);
				// Show page and items Timeline page items are different
				if (nodeId.equals("-1")) {
					String node = e.getString("node_alias");
					item.put("node", node);
				}

				if (loadType == API.LOAD_NEW) {
					lvFillMaps.add(i, item);
				} else {
					lvFillMaps.add(item);
				}

				// update ahchor
				if (i == 0) {
					if (loadType == API.LOAD_INIT || loadType == API.LOAD_NEW) {
						firstEvent = e.getString("id");
					}
				}
				if (i == eventsLen - 1) {
					if (loadType == API.LOAD_INIT || loadType == API.LOAD_OLD) {
						lastEvent = e.getString("id");
						System.out.println("lastEvent:" + lastEvent);
					}
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (eventsLen > 0) {
			lvAdapter.notifyDataSetChanged();
		} else if (loadType == API.LOAD_OLD) {
			hasMore = false;
		}
		return eventsLen;
	}

	protected void doRefresh() {
		int eventsLen = this.loadItems(API.LOAD_NEW);
		String msg = null;
		if (eventsLen > 0) {
			msg = "已更新！";
		} else {
			msg = "没有新消息！";
		}
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	public void onRefreshClick(View v) {
		doRefresh();
	}

	public void finishStream(View v) {
		this.finish();
	}
}
