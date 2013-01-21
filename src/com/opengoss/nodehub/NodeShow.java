package com.opengoss.nodehub;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.opengoss.nodehub.utils.API;


public class NodeShow extends Stream {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.show);
		
		super.type = API.TYPE_NODE; 
		super.nodeId = getIntent().getStringExtra("nodeId");
		super.init();
	}

	@Override
	protected int initStream() {
		// TODO Auto-generated method stub
		JSONObject data = API.getNodeData(nodeId, severity, "yes");
		if (data == null) {
			Toast.makeText(this, getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
			return 0;
		}
		// Update title
		String title = null;
		try{
			JSONObject node = data.getJSONObject("node");
			title = node.getString("alias");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		int eventsLen = super.updateEvents(data, API.LOAD_INIT);
		if (eventsLen == 0) {
			title = title + " (无消息)";
			Toast.makeText(NodeShow.this, "无消息！", Toast.LENGTH_SHORT)
			.show();
		}
		TextView tv = (TextView)findViewById(R.id.header_title);
		tv.setText(title);
		
		return eventsLen;
	}
	
	@Override
	public void onItemClick(View v) {
		// TODO Auto-generated method stub		
	}
	
	public void finishShow(View v) {
		NodeShow.this.finish();
	}
}
