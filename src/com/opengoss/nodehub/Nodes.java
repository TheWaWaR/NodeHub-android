package com.opengoss.nodehub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.opengoss.nodehub.utils.API;


public class Nodes extends Activity {
	private BroadcastReceiver mReceiver;
	private static final int ACTION_INDEX = 2;
	
	protected ListView lv;
	protected List<HashMap<String, Object>> lvFillMaps;	
	protected SimpleAdapter lvAdapter;
	
	// Menu setp 2
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);	
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.nodes_ctx, menu);
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		View targetView = info.targetView;
		String nodeName = ((TextView)(targetView.findViewById(R.id.node_alias))).getText().toString();
		menu.findItem(R.id.menu_show_item).setTitle("进入节点：" + nodeName);
	}

	// Menu setp 3
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.menu_show_item:
			TextView tv = (TextView) info.targetView.findViewById(R.id.node_nid);
			String nid = tv.getText().toString();
			Intent i = new Intent(this, NodeShow.class);
			i.putExtra("nodeId", nid);
			startActivity(i);	
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nodes);
		
		initListView();
		JSONObject data = API.getNodes();
		if (data == null) {
			Toast.makeText(this, getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
		} else {			
			updateNodes(data);
			this.registerForContextMenu(lv);			
		}
	}
	
	private final SimpleAdapter.ViewBinder mViewBinder =
		    new SimpleAdapter.ViewBinder() {
		        @Override
		        public boolean setViewValue(
		                final View view,
		                final Object data,
		                final String textRepresentation) {

		            if (view instanceof ImageView) {
		                ((ImageView) view).setImageDrawable((Drawable) data);
		                return true;
		            }

		            return false;
		        }
		    };
	
	protected void initListView() {
		lv= (ListView)findViewById(R.id.nodelist);		
        // create the grid item mapping
        String[] from = new String[] { "nid", "status", "alias", "owner", "name", "apikey" };
        int[] to = new int[] { R.id.node_nid, R.id.node_status, R.id.node_alias, 
        		R.id.node_owner, R.id.node_name, R.id.node_apikey, };

        // prepare the list of all records
        lvFillMaps = new ArrayList<HashMap<String, Object>>();
        // fill in the grid_item layout
        lvAdapter = new SimpleAdapter(this, lvFillMaps, R.layout.node_item, from, to);
        lvAdapter.setViewBinder(mViewBinder);
        lv.setAdapter(lvAdapter);
	}
	
	private void updateNodes(JSONObject data) {
		int eventsLen = 0;
		try {
			JSONArray events = data.getJSONArray("nodes");
			eventsLen = events.length();
			for (int i=0; i <= eventsLen-1; i++) {
				JSONObject e = (JSONObject) events.get(i);
				
				HashMap<String, Object> item = new HashMap<String, Object>();
				int presence = e.getInt("presence");
				GradientDrawable status = (GradientDrawable)getResources().getDrawable(R.drawable.node_status); 
				// <!-- Green: #5DA423, Red: #C60F13, Gray: #505050 -->
				switch (presence) {
				case -1:
					status.setColor(Color.parseColor("#C60F13"));
					break;
				case 0:
					status.setColor(Color.parseColor("#DDDDDD"));
					break;
				case 1:
					status.setColor(Color.parseColor("#5DA423"));
					break;
				}
				item.put("status", status);
				
				String nid = e.getString("id");
				String alias = e.getString("alias");
				String owner = e.getString("owner");
				String name = e.getString("name");
				String apikey = e.getString("apikey");
				
				item.put("nid", nid);
				item.put("alias", alias);
				item.put("owner", owner);
				item.put("name", name);
				item.put("apikey", apikey);
				lvFillMaps.add(item);				
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (eventsLen > 0) {
			lvAdapter.notifyDataSetChanged();	
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
						JSONObject data = API.getNodes();
						if (data == null) {
							Toast.makeText(Nodes.this, getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
						} else {
							lvFillMaps.clear();
							lvAdapter.notifyDataSetChanged();
							updateNodes(data);
							Toast.makeText(Nodes.this, "节点列表已更新！", Toast.LENGTH_SHORT).show();
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
	
	
	public void onItemClick(View v) {
		// TODO Auto-generated method stub
		TextView tv = (TextView)(((View)v.getParent().getParent()).findViewById(R.id.node_nid));
		String nid = tv.getText().toString();
		Intent i = new Intent(Nodes.this, NodeShow.class);
		i.putExtra("nodeId", nid);
		startActivity(i);
	}
	
	public void finishNodes(View v) {
		Nodes.this.finish();
	}
}
