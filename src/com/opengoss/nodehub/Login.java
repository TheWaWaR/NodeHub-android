package com.opengoss.nodehub;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.opengoss.nodehub.utils.API;


public class Login extends Activity {
	private static SharedPreferences sp = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		sp = this.getSharedPreferences("API", MODE_PRIVATE);
		API.setSharedPreferences(sp);
		String token = sp.getString(API.KEY_TOKEN, null);
		if (token != null) {
			String retToken = API.doLogin(null, null, token);
			System.out.println("retToken:" + retToken);
			if (retToken != null ){				
				if (retToken.length() == 0) {
					Toast.makeText(this, getString(R.string.error_connect), Toast.LENGTH_SHORT).show();	
				} else {
					startHome(token);
				}
			}
		}
		setContentView(R.layout.login);
	}

	private void startHome(String token) {			
		Intent i = new Intent(Login.this, Main.class);
		i.putExtra("token", token);
		startActivity(i);
		finish();
	}

	public void doLogin(View v) {
		EditText loginText = (EditText) Login.this.findViewById(R.id.username);
		EditText passText = (EditText) Login.this.findViewById(R.id.password);
		String username = loginText.getText().toString();
		String password = passText.getText().toString();
		
		String token = API.doLogin(username, password, null);
		System.out.println("Login.doLogin.token:" + token);
		if(token == null) {
			Toast.makeText(Login.this, "用户名或密码错误！", Toast.LENGTH_SHORT).show();
		} else if(token.length() == 0) {
			Toast.makeText(this, getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, getString(R.string.login_notice), Toast.LENGTH_SHORT).show();
			startHome(token);
		}
	}
	
	public void doRegister(View v) {
		String link = API.scheme + "://" + API.host + ":" + API.port + "/signup";
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
		startActivity(browserIntent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.login, menu);
		return true;
	}
	
	// Handle all menu action
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_quit:
			finish();
			break;
		default:
			break;
		}
		return true;
	}
}
