// Insert here your own package name
// Include in AndroidManifest.xml:
//    <uses-permission android:name="android.permission.INTERNET" />
//    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
//    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
//    <uses-permission android:name="android.permission.BLUETOOTH" />
//    <uses-feature android:name="android.hardware.usb.accessory"/>
//    <uses-feature android:name="android.hardware.usb.host" />

package com.example.datalogger;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

public class IOBoardFunctions extends Activity {

	static final int MODE_BLUETOOTH = 0;
	static final int MODE_WIFI = 1;
	static final int MODE_USB = 2;
	static final int MODE_USBHOST = 3;
	
	int iConnectionMode=0;
	BTFunctions BT = new BTFunctions();
	WiFiFunctions WiFi = new WiFiFunctions();
	USBFunctions USB = new USBFunctions();
	USBHostFunctions USBHost = new USBHostFunctions();
	
	public IOBoardFunctions() { // constructor
	}
	
	public boolean connected() {
		boolean bRes=false;
		if (iConnectionMode==MODE_BLUETOOTH) bRes=BT.connected();
		if (iConnectionMode==MODE_WIFI) bRes=WiFi.connected();
		if (iConnectionMode==MODE_USB) bRes=USB.connected();
		if (iConnectionMode==MODE_USBHOST) bRes=USBHost.connected();
		return bRes;
	}

	public void Initiate (int channel, Handler MessageHandler, String IPAddressInit, int IPPortInit, int ConnectionMode, Context context) {
		iConnectionMode=ConnectionMode;
		if (iConnectionMode==MODE_BLUETOOTH) BT.Initiate(channel, MessageHandler, IPAddressInit);
		if (iConnectionMode==MODE_WIFI) WiFi.Initiate(channel, MessageHandler, IPAddressInit, IPPortInit);
		if (iConnectionMode==MODE_USB) USB.Initiate(channel, MessageHandler, context);
		if (iConnectionMode==MODE_USBHOST) USBHost.Initiate(channel, MessageHandler, IPPortInit, context);
	}

	public void Initiate (int channel, Handler MessageHandler, String IPAddressInit, int ConnectionMode, Context context) {
		Initiate(channel, MessageHandler, IPAddressInit, 2000, ConnectionMode, context);
	}
	
	public void Initiate (int channel, Handler MessageHandler, int ConnectionMode, Context context) {
		Initiate(channel, MessageHandler, "192.168.1.2", 2000, ConnectionMode, context);
	}

	public void connect () {
		if (iConnectionMode==MODE_BLUETOOTH) BT.connect();
		if (iConnectionMode==MODE_WIFI) WiFi.connect();
		if (iConnectionMode==MODE_USB) USB.connect();
		if (iConnectionMode==MODE_USBHOST) USBHost.connect();
	}

	public void disconnect () {
		if (iConnectionMode==MODE_BLUETOOTH) BT.disconnect();
		if (iConnectionMode==MODE_WIFI) WiFi.disconnect();
		if (iConnectionMode==MODE_USB) USB.disconnect();
		if (iConnectionMode==MODE_USBHOST) USBHost.disconnect();
	}

	public void send_command(String command) {
		if (iConnectionMode==MODE_BLUETOOTH) BT.send_command(command);
		if (iConnectionMode==MODE_WIFI) WiFi.send_command(command);
		if (iConnectionMode==MODE_USB) USB.send_command(command);
		if (iConnectionMode==MODE_USBHOST) USBHost.send_command(command);
	}
}
