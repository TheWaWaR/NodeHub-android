package com.opengoss.nodehub;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttNotConnectedException;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

import com.opengoss.nodehub.utils.API;
import com.opengoss.nodehub.utils.Helper;

public class Main extends TabActivity {
	private ProgressDialog pd;
	private TabHost tabHost;
	private View tabTimeline;
	private MqttClient client;	
	private String android_id;	
	private String token = null;
	
	public static final String[] ACTION_TYPES = {"THREADS", "TIMELINE", "NODES"};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		
		token = getIntent().getStringExtra("token");		
		android_id = Secure.getString(this.getContentResolver(),
				Secure.ANDROID_ID);

		pd = ProgressDialog.show(this, "Connecting", "Please wait..", true, false);

		new Thread() {
			int flag;
			public void run() {
				if (connect())
					flag = 1;
				else
					flag = 0;
				handlerConnect.sendMessage(Message.obtain(handlerConnect, flag));				
				subscribe();
				System.out.println("Subscribe successfully!");
			}
		}.start(); // Connect in a new thread!
		
		/**
		 * Powered by:
		 * http://stackoverflow.com/questions/5799320/android-remove-space-between-tabs-in-tabwidget/5804436#5804436 
		 * */
		tabHost = getTabHost();
		Intent intent;
		TabSpec spec;		
		View tab;
		
		String[] tabLabels = getResources().getStringArray(R.array.tabLabel);		
		// Threads
		tab = LayoutInflater.from(tabHost.getContext()).
				inflate(R.layout.tab, null);
		((TextView)tab.findViewById(R.id.tab_text)).setText(tabLabels[0]);
		intent = new Intent(this, Threads.class);
		spec = tabHost.newTabSpec("threads")
				.setIndicator(tab)
				.setContent(intent);
		tabHost.addTab(spec);
		
		// Timeline
		tab = LayoutInflater.from(tabHost.getContext()).
				inflate(R.layout.tab, null);
		tabTimeline = tab;
		((TextView)tab.findViewById(R.id.tab_text)).setText(tabLabels[1]);
		intent = new Intent(this, Timeline.class);
		spec = tabHost.newTabSpec("timeline")
				.setIndicator(tab)
				.setContent(intent);		
		tabHost.addTab(spec);

		// Nodes
		tab = LayoutInflater.from(tabHost.getContext()).
				inflate(R.layout.tab, null);
		((TextView)tab.findViewById(R.id.tab_text)).setText(tabLabels[2]);
		intent = new Intent(this, Nodes.class);
		spec = tabHost.newTabSpec("nodes")
				.setIndicator(tab)
				.setContent(intent);
		tabHost.addTab(spec);
		
		tabHost.setCurrentTab(0);
	}
	
	
	/****************************************************************************/		
	final Handler handlerConnect = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			pd.dismiss();
			if (msg.what == 0) {
				Intent i = new Intent(Main.this, Login.class);
				i.putExtra("msg", "Connect Failed!");
				startActivity(i);
				finish();
			}
		}
	};

	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			// String topic = data.getString("topic");
			String message = data.getString("message");
			if (message.equals("yes")) {
				ImageView iv = (ImageView)tabTimeline.findViewById(R.id.tab_status);
				iv.setVisibility(View.VISIBLE);
				// Toast.makeText(Main.this, "New Events!", Toast.LENGTH_SHORT).show();
			} else {
				System.out.println("+" + message + "+");
			}
		}
	};
	
	
	@SuppressWarnings("unused")
	private void publish() {
		try {
			client.publish("NITTrichy", "Test Message".getBytes(), 1, false);
		} catch (MqttException e) {
			Toast.makeText(this, "Publish Failed!", Toast.LENGTH_SHORT).show();
		}
		Toast.makeText(this, "Publish Success!", Toast.LENGTH_SHORT).show();
	}

	private void subscribe() {
		try {
			String topics[] = { token};
			int qos[] = { 0 };
			int ret = client.subscribe(topics, qos);
			System.out.println("unsubscribe ret:" + ret);
		} catch (MqttException e) {
			e.printStackTrace();
			return;
		}
	}
	
	private void unsubscribe() {
		try {
			String topics[] = { token };
			int ret = client.unsubscribe(topics);
			System.out.println("unsubscribe ret:" + ret);
		} catch (MqttException e) {
			e.printStackTrace();
			return;
		}
	}

	private boolean connect() {		
		try {
			client = (MqttClient) MqttClient.createMqttClient("tcp://" + API.host
					+ ":" + API.emqtt_port, null);
			client.registerSimpleHandler(new MessageHandler());
			client.connect("HM" + android_id, true, (short) 240);
			System.out.println("connect: success!");
			return true;
		} catch (MqttException e) {
			e.printStackTrace();
			return false;
		}
	}

	private class MessageHandler implements MqttSimpleCallback {
		public void publishArrived(String _topic, byte[] payload, int qos,
				boolean retained) throws Exception {
			String _message = new String(payload);
			Bundle b = new Bundle();
			b.putString("topic", _topic);
			b.putString("message", _message);
			Message msg = handler.obtainMessage();
			msg.setData(b);
			handler.sendMessage(msg);
		}

		public void connectionLost() throws Exception {
			client = null;
			System.out.println("Lost:" + "connection dropped");
			Thread t = new Thread(new Runnable() {

				public void run() {
					do {// pause for 5 seconds and try again;
						System.out.println("Lost:" + "sleeping for 5 seconds before trying to reconnect");
						try {
							Thread.sleep(5 * 1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} while (!connect());
					System.err.println("reconnected");
				}
			});
			t.start();
		}
	}
 
	public void onRefreshClick(View v) {
		int currentTab = tabHost.getCurrentTab();
		Intent i = new Intent();
		i.setAction(Main.ACTION_TYPES[currentTab]);
		sendBroadcast(i);
		
		ImageView iv = (ImageView)tabTimeline.findViewById(R.id.tab_status);
		iv.setVisibility(View.GONE);
	}
	
	/**************************************************************************/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
	
	// Handle all menu action
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_logout:
			unsubscribe();
			try {
				client.disconnect();
				client.terminate();
			} catch (MqttPersistenceException e) {
				e.printStackTrace();
			}
			
			API.doLogout();			
			Intent i = new Intent(Main.this, Login.class);
			startActivity(i);
			finish();
			break;

		case R.id.menu_quit:
			unsubscribe();
			try {
				client.disconnect();
				client.terminate();
			} catch (MqttPersistenceException e) {
				e.printStackTrace();
			}
			API.doUnsubscribe();
			finish();
			break;

		case R.id.menu_about:
			Helper.alert((Context)Main.this, R.string.about);
			break;
		default:
			break;
		}
		return true;
	}
}
