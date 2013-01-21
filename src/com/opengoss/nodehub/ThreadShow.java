package com.opengoss.nodehub;

import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.opengoss.nodehub.utils.API;

public class ThreadShow extends Stream {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.show);
		
		super.type = API.TYPE_THREAD;
		super.thread = getIntent().getStringExtra("thread");
		super.init();
	}

	@Override
	protected int initStream() {
		// TODO Auto-generated method stub
		JSONObject data = API.getThreadData(thread, severity);
		if (data == null) {
			Toast.makeText(this, getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
			return 0;
		}
		// Update title
		int eventsLen = super.updateEvents(data, API.LOAD_INIT);
		String title = thread;
		if (eventsLen == 0) {
			Toast.makeText(ThreadShow.this, "无消息！", Toast.LENGTH_SHORT)
			.show();
			title = thread + " (无消息)";
		}
		TextView tv = (TextView)findViewById(R.id.header_title);
		tv.setText(title);
		
		return eventsLen; 
	}
	
	@Override
	public void onItemClick(View v) {
		// TODO Auto-generated method stub
		TextView tv = (TextView)(((View)v.getParent().getParent()).findViewById(R.id.item_nid));
		String nid = tv.getText().toString();
		Intent i = new Intent(ThreadShow.this, NodeShow.class);
		i.putExtra("nodeId", nid);
		startActivity(i);
	}
	
	public void finishShow(View v) {
		ThreadShow.this.finish();
	}
}
