package com.opengoss.nodehub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.opengoss.nodehub.utils.API;


public class Threads extends Activity {
	private static final int ACTION_INDEX = 0;
	private BroadcastReceiver mReceiver;

	protected ListView lv;
	protected List<HashMap<String, String>> lvFillMaps;
	protected SimpleAdapter lvAdapter;
	// Menu setp 2
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.threads_ctx, menu);
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		View targetView = info.targetView;
		String nodeName = ((TextView)(targetView.findViewById(R.id.thread_node))).getText().toString();
		String threadName = ((TextView)(targetView.findViewById(R.id.thread_thread))).getText().toString();
		menu.findItem(R.id.menu_show_node).setTitle("进入节点：" + nodeName);
		menu.findItem(R.id.menu_show_thread).setTitle("进入线索：" + threadName);
	}

	// Menu setp 3
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		TextView tv;
		Intent i;
		switch (item.getItemId()) {
		case R.id.menu_show_node:
			tv = (TextView) info.targetView.findViewById(R.id.thread_nid);
			String nid = tv.getText().toString();
			i = new Intent(this, NodeShow.class);
			i.putExtra("nodeId", nid);
			startActivity(i);	
			return true;
		case R.id.menu_show_thread:
			tv = (TextView) info.targetView.findViewById(R.id.thread_thread);
			String thread = tv.getText().toString();
			i = new Intent(this, ThreadShow.class);
			i.putExtra("thread", thread);
			startActivity(i);	
			return true;			
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.threads);

		init();
		System.out.println("Dashboard created!");
	}

	public void init() {
		initListView();
		JSONObject data = API.getThreads();
		if (data == null) {
			Toast.makeText(this, getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
		} else {
			updateThreads(data);
			this.registerForContextMenu(lv);
		}
	}

	protected void initListView() {
		lv = (ListView) findViewById(R.id.threadlist);
		// create the grid item mapping
		String[] from = new String[] { "nid", "node", "thread", "time",
				"title", "body" };
		int[] to = new int[] { R.id.thread_nid, R.id.thread_node,
				R.id.thread_thread, R.id.thread_time, R.id.thread_title,
				R.id.thread_body };

		// prepare the list of all records
		lvFillMaps = new ArrayList<HashMap<String, String>>();
		// fill in the grid_item layout
		lvAdapter = new SimpleAdapter(this, lvFillMaps, R.layout.thread_item,
				from, to);

		lv.setAdapter(lvAdapter);		
	}

	/****************************************************************************/
	// API stuff
	public int updateThreads(JSONObject data) {
		int itemLen = 0;
		try {
			JSONObject content = data.getJSONObject("events");
			@SuppressWarnings("unchecked")
			Iterator<String> nodeIds = content.keys();
			while (nodeIds.hasNext()) {
				JSONObject obj = content.getJSONObject(nodeIds.next());
				// JSONObject nodeObj = obj.getJSONObject("node");
				JSONArray events = obj.getJSONArray("events");
				itemLen += events.length();
				for (int i = 0; i < events.length(); i++) {
					JSONObject e = (JSONObject) events.get(i);
					String nid = e.getString("node_id");
					String node = e.getString("node_alias");
					String thread = e.getString("thread");
					String time = e.getString("raised_at");
					String title = e.getString("title");
					String body = e.getString("body");
					if (body == "null") {
						body = "";
					}

					HashMap<String, String> item = new HashMap<String, String>();
					item.put("nid", nid);
					item.put("node", node);
					item.put("thread", thread);
					item.put("time", time);
					item.put("title", title);
					item.put("body", body);
					lvFillMaps.add(item);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (itemLen > 0) {
			lvAdapter.notifyDataSetChanged();
		}
		return itemLen;
	}

	// Listener
	/****************************************************************************/
	public void onNodeClick(View v) {
		TextView tv = (TextView) ((View) v.getParent().getParent())
				.findViewById(R.id.thread_nid);
		String nid = tv.getText().toString();
		Intent i = new Intent(Threads.this, NodeShow.class);
		i.putExtra("nodeId", nid);
		startActivity(i);
	}

	public void onThreadClick(View v) {
		TextView tv = (TextView) v.findViewById(R.id.thread_thread);
		String thread = tv.getText().toString();
		Intent i = new Intent(Threads.this, ThreadShow.class);
		i.putExtra("thread", thread);
		startActivity(i);
	}

	public void onRefreshClick(View v) {
		JSONObject data = API.getThreads();
		if (data == null) {
			Toast.makeText(this, getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
		} else {
			updateThreads(data);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver();
	}

	// Receive message from Main activity
	private void registerReceiver() {
		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent.getAction().equals(Main.ACTION_TYPES[ACTION_INDEX])) {	
						JSONObject data = API.getThreads();
						if (data == null) {
							Toast.makeText(Threads.this, getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
						} else {
							lvFillMaps.clear();
							lvAdapter.notifyDataSetChanged();
							updateThreads(data);
							Toast.makeText(Threads.this, "已更新!", Toast.LENGTH_SHORT).show();
						}
					}
				}
			};
		}
		getApplicationContext().registerReceiver(mReceiver,
				new IntentFilter(Main.ACTION_TYPES[ACTION_INDEX]));
	}

	private void unregisterReceiver() {
		if (mReceiver != null) {
			getApplicationContext().unregisterReceiver(mReceiver);
		}
	}
}