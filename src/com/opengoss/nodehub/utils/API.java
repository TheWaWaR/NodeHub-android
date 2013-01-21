package com.opengoss.nodehub.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;


public class API {	
	// Constant variable
	public static final String KEY_TOKEN = "token";
	public static final int TYPE_NODE = 0;
	public static final int TYPE_THREAD = 1;
	
	public static final int LOAD_INIT = 0;
	public static final int LOAD_NEW = 1;
	public static final int LOAD_OLD = 2;
	public static final String[] loadTypes = {"init", "new", "old"};
	
	// Cookie
	private static SharedPreferences sp = null;
	private static String token = null;
	
	// API URI
	public static final String scheme = "http";
	public static final String host = "ahorn.me";
	public static final int port = 8001;
	public static final int emqtt_port = 1883;
	
	private static final String path_prefix = "/api"; // *** The PREFIX ***	
	private static final String path_login = "/login";
	private static final String path_logout = "/logout";
	private static final String path_nodes = "/nodes";
	private static final String path_node_show = "/nodes/show"; // require id
	private static final String path_timeline = "/home/timeline";
	private static final String path_threads = "/home/threads";
	private static final String path_thread_show = "/threads";
	private static final String path_load_events = "/load/events"; // optional require id
	private static final String path_load_thread_events = "/load/thread-events";

	public static void setSharedPreferences(SharedPreferences tsp) {
		sp = tsp;
	}
	
	/******************************************************************/	
	// The views
	public static String doLogin(String username, String password, String tToken) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("username", username));
		params.add(new BasicNameValuePair("password", password));
		params.add(new BasicNameValuePair("token", tToken));
		JSONObject data = httpGet(path_login, params);
		
		if (data == null) {
			return "";
		} else {			
			try {
				String category = data.getString("category");
				if (category.equals("success") ) {
					token = data.getString("token");
					sp.edit().putString(KEY_TOKEN, token).commit();
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return token;
		}
	}
	
	public static JSONObject doLogout() {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		JSONObject data = httpGet(path_logout, params);
		sp.edit().remove(KEY_TOKEN).commit();
		token = null;
		return data;
	}
	
	public static JSONObject doUnsubscribe() {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		JSONObject data = httpGet(path_logout, params);
		return data;
	}
		
	// For initialization
	public static JSONObject getNodes() {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		JSONObject data = httpGet(path_nodes, params);
		return data;
	}
	
	public static JSONObject getThreads() {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		JSONObject data = httpGet(path_threads, params);
		return data;
	}
	
	public static JSONObject getThreadData(String thread, String severity) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("thread", thread));
		params.add(new BasicNameValuePair("severity", severity));
		return httpGet(path_thread_show , params);
	}
		
	public static JSONObject getTimeline(String severity) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("severity", severity));
		return httpGet(path_timeline, params);
	}
	
	public static JSONObject getNodeData(String id, String severity, String isFirst) {		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("severity", severity));
		params.add(new BasicNameValuePair("isFirst", isFirst));
		return httpGet(path_node_show + "/" + id, params);
	}
	
	
	// For dynamic load
	public static JSONObject loadEvents(String nodeId, String severity, String ahchor, int loadType) {	
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		//id, severity, loadType, ahchor
		params.add(new BasicNameValuePair("id", nodeId)); // Current node's id
		params.add(new BasicNameValuePair("severity", severity));
		params.add(new BasicNameValuePair("loadType", loadTypes[loadType]));
		params.add(new BasicNameValuePair("ahchor", ahchor));
		
		return httpGet(path_load_events, params);
	}
	
	public static JSONObject loadThreadEvents(String thread, String severity, String ahchor, int loadType) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("thread", thread));
		params.add(new BasicNameValuePair("severity", severity));
		params.add(new BasicNameValuePair("loadType", loadTypes[loadType]));
		params.add(new BasicNameValuePair("ahchor", ahchor));
		
		return httpGet(path_load_thread_events, params);		
	}
	
	
	/*********************************************************************/
	// 使用HttpCient连接GoogleWeatherAPI
	private static String httpClientConn(String path, List<NameValuePair> params) {		
		// prepare URL
		String query = null;
		URI uri = null;
		if (params != null) {
			query = URLEncodedUtils.format(params, "utf-8");
		}
		try {
			uri = URIUtils.createURI(scheme, host, port, path_prefix + path, query, null);
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
				
		// Do GET
		HttpGet httpget = new HttpGet(uri);		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		ResponseHandler<String> responseHandler = new BasicResponseHandler();		
		String content = null;
		try {
			content = httpclient.execute(httpget, responseHandler);
		} catch (Exception e) {
			e.printStackTrace();
		}
		httpclient.getConnectionManager().shutdown();
		return content;
	}	

	private static JSONObject httpGet(String path, List<NameValuePair> params) {
		if (token != null) {
			params.add(new BasicNameValuePair("token", token));
		}
		
		System.out.println("path:"+ path);
		System.out.println("params:" + params.toString());
		String content = httpClientConn(path, params);		
		System.out.println("content:"+ content);
		
		JSONObject data = null;
		if (content != null && content.length() > 0) {			
			try {
				data = new JSONObject(content);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return data;
	}
}
