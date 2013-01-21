package com.opengoss.nodehub;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.opengoss.nodehub.utils.API;

public class Timeline extends Stream {
	private static final int ACTION_INDEX = 1;
	private BroadcastReceiver mReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timeline);
		
		super.init();
	}
	
	@Override
	protected int initStream() {
		JSONObject data = API.getTimeline(severity);
		if (data == null) {
			Toast.makeText(this, getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
			return 0;
		}
		return super.updateEvents(data, API.LOAD_INIT);
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

	private void registerReceiver() {
		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent.getAction().equals(Main.ACTION_TYPES[ACTION_INDEX])) {	
						doRefresh();
					} else {
						System.out.println("Action: " + intent.getAction());
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
		
	@Override
	public void onItemClick(View v) {
		// TODO Auto-generated method stub
		TextView tv = (TextView)(((View)v.getParent().getParent()).findViewById(R.id.item_nid));
		String nid = tv.getText().toString();
		Intent i = new Intent(Timeline.this, NodeShow.class);
		i.putExtra("nodeId", nid);
		startActivity(i);
	}
}