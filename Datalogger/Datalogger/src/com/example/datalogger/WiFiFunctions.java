// Insert here your own package name
// Include in AndroidManifest.xml:
//    <uses-permission android:name="android.permission.INTERNET" />
//    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

package com.example.datalogger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.Activity;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

public class WiFiFunctions extends Activity {
	private static final String TAG = "WiFifunctions";

	Socket socket = null;
	DataOutputStream dataOutputStream;

	String IPAddress;
	int IPPort;
	Handler MessageHandlerToMain;
	int MessageChannel;

	int	wiFiSuccess=-1;

	final int RECIEVE_MESSAGE_WIFISTATUS = 1;		// Constant for Handler: WiFi status
	final int RECIEVE_MESSAGE_WIFIDATA = 2;		// Constant for Handler: Received WiFi data

	String sText;

	public WiFiFunctions () { // constructor
	}

	public boolean connected() {
		return (wiFiSuccess>1) ? true : false ;
	}

	private void wiFiStatus(int level, boolean succeeded, String TextMessage) {
		Log.e(TAG, "WiFiText: "+TextMessage);
		if (succeeded) {
			if (level>wiFiSuccess) {
				wiFiSuccess=level;
				statusText(TextMessage);
			}
		} else {
			if (level<wiFiSuccess) {
				wiFiSuccess=level;
				statusText(TextMessage);
			}
		}
	}

	private void statusText(String sMessage){
		Log.e(TAG, "statusText: "+sMessage);
		MessageHandlerToMain.obtainMessage(RECIEVE_MESSAGE_WIFISTATUS, MessageChannel, ((wiFiSuccess>1) ? 1 : 0 ), sMessage).sendToTarget();
	}

	public void Initiate (int channel, Handler MessageHandler, String IPAddressInit, int IPPortInit) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); //dirty way to use socket in main thread
		StrictMode.setThreadPolicy(policy);                                                         // since Android 3

		MessageChannel=channel;
		MessageHandlerToMain = MessageHandler;  // This sets the messagehandler in this object

		IPAddress = IPAddressInit; 
		IPPort = IPPortInit;

		new Thread(new Runnable() {  //Make a thread
			StringBuilder sb = new StringBuilder(128);
			String resultstring;
			int chr,lngth;
			public void run(){		 // and define the interface
				InputStreamReader dataInputStream = null;
				BufferedReader instream = null;
				lngth=0;
				chr=-1;
				while (true) {
					if (wiFiSuccess>1) {
						Log.e(TAG,"WiFiThread: 1");
						try {
							Log.e(TAG,"WiFiThread: 2");
							dataInputStream = new InputStreamReader(socket.getInputStream());
							Log.e(TAG,"WiFiThread: 3");
							instream = new BufferedReader(dataInputStream);
							Log.e(TAG,"WiFiThread: 4");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						chr=0;
						while (chr!=-1) {

							try {
								chr = instream.read();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								chr=-1;
							}
							//Log.e(TAG,"WiFiThread: 7");

							if (chr != -1) {
								sb.append((char) chr);
								//Log.e(TAG,"WiFiThread: 8b "+chr);
								lngth+=1;
							}
							if ((lngth>100) || ((chr>=0) && (chr<32))) {
								//Log.e(TAG,"WiFiThread: 9 "+chr);

								resultstring = sb.toString();
								MessageHandlerToMain.obtainMessage(RECIEVE_MESSAGE_WIFIDATA, MessageChannel, -1, resultstring).sendToTarget();
								sb.setLength(0);
								lngth=0;
							}
						} // while
					} // if wiFiSuccess>1
					try {Thread.sleep(1);} catch (InterruptedException e) {}// Sleep 1 ms
				} // while
			}
		}).start(); // Thread
	}

	public void connect () {
		Log.e(TAG, "Connect" );
		Log.e(TAG, "DisConnect @ connect" );
		disconnect();
		socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(IPAddress, IPPort),500);
			wiFiStatus(1,true,"Socket created");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			wiFiStatus(0,false,"Socket error");
		} 
		if (wiFiSuccess>0) {
			try {
				dataOutputStream = new DataOutputStream(socket.getOutputStream());
				wiFiStatus(2,true,"WiFi OK");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				wiFiStatus(1,false,"Stream error");
			}
		}

	}

	public void disconnect () {
		Log.e(TAG, "DisConnect" );
		//if (wiFiSuccess<2) return;
		try {
			if (dataOutputStream!=null) dataOutputStream.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			if (socket!=null) socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		wiFiStatus(0, false, "Socket disconnected");

	}

	public void send_command(String command) {
		if (wiFiSuccess>1) {
			try {
				dataOutputStream.writeBytes(command);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				wiFiStatus(0,false,"WiFi error");
			}
		}
	}

}
