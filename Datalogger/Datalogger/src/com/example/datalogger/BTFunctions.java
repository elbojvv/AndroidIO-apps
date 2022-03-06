// Insert here your own package name
// Include in AndroidManifest.xml:
//    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
//    <uses-permission android:name="android.permission.BLUETOOTH" />

package com.example.datalogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

public class BTFunctions extends Activity {
	private static final String TAG = "BTfunctions";
	
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private ConnectedThread mConnectedThread;
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID service
	//private static String BTaddress = "00:10:05:25:00:01"; 	  // MAC-address of Bluetooth module (you must edit this line)
	String BTAddress;
	String BTReturnString="";
	int MessageChannel;
	boolean BTReturnStringR=false;
	Handler MessageHandlerToMain;
	int	BTSucces=0;
	int BTFail=0;

	final int RECIEVE_MESSAGE_BTSTATUS = 1;		// Constant for Handler: Bluetooth status
	final int RECIEVE_MESSAGE_BTDATA = 2;		// Constant for Handler: Received Bluetooth data

	String sText;

	public BTFunctions () { // constructor
	}

	public boolean connected() {
		return (BTSucces>3) ? true : false ;
	}
	
	private void btStatus(int level, boolean succeeded, String TextMessage) {
		// btStatus contains the status ofthe Bluetooth connection and generates the feedback to the GUI
		// Status 0 .. 6:
		//   0: At program start
		//   1: Device has Bluetooth
		//   2: Bluetooth is on
		//   3..5: different phases in set up of connection
		//   6: Succesful sending of data
		// variables wiFiSuccess with highest success and wiFiFail, with lowest value that failed 
		if (succeeded) {
			if (level>BTSucces) {
				BTSucces=level;
				statusText(TextMessage);
			}
			if (BTFail>level) {
				BTFail=level-1;
			}
		} else {
			if ((BTSucces==6) && (level<6)) mConnectedThread.interrupt();
			if (level<6) statusText(TextMessage);
			if (level<BTSucces) {
				BTSucces=level;
				statusText(TextMessage);
			}
			if (BTFail>level) {
				BTFail=level-1;
			}
		}
	}

	public void Initiate (int channel, Handler MessageHandler, String BTDeviceAddress) {
		MessageChannel=channel;
		MessageHandlerToMain = MessageHandler;  // This sets the messagehandler in this object
		BTAddress = BTDeviceAddress;
		btAdapter = BluetoothAdapter.getDefaultAdapter();		// get Bluetooth adapter - the default local adapter, or null if Bluetooth is not supported on this hardware platform 
	}

	private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
		if(Build.VERSION.SDK_INT >= 10){
			try {

				final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
				return (BluetoothSocket) m.invoke(device, MY_UUID);
			} catch (Exception e) {
			}
		}
		return  device.createRfcommSocketToServiceRecord(MY_UUID);
	}


	public void connect () {
		if (BTSucces>5) return;
		BTSucces=0;
		BTFail=0;
		BluetoothDevice device = btAdapter.getRemoteDevice(BTAddress); // Set up a pointer to the remote node using it's address.

		// check whether the device has Bluetooth
		if(btAdapter==null) { 
			btStatus(1, false, "Device has no BT");
			return;
		} else {
			btStatus(1, true, "device has BT");
			// Check Bluetooth is on
			if (btAdapter.isEnabled()) {
				btStatus(2, true, "BT is on");
			} else {
				btStatus(2, false, "BT is off");
				return;
			}
		}

		if (BTSucces<2) return;
		// Two things are needed to make a connection: 
		//   A MAC address, which we got above.
		//   A Service ID or UUID.  In this case we are using the UUID for SPP.
		Log.d(TAG, "3");
		try {
			btSocket = createBluetoothSocket(device);
			// btSocket = device.create	RfcommSocketToServiceRecord(MY_UUID); // on Android 4

		} catch (IOException e) {
			btStatus(3, false, "BT socket error");
			return;
		}
		btStatus(3, true, "BT socket made");
		// Discovery is resource intensive.  Make sure it isn't going on when you attempt to connect and pass your message.
		if (btAdapter!=null) { 
			btAdapter.cancelDiscovery(); 
		} else {
			btStatus(1, false, "Device has no BT");
			return;
		}
		// Establish the connection.  This will block until it connects.

		try {
			if (btSocket!=null) {
				btSocket.connect(); 
				btStatus(4, true, "BT connected");
			} else {
				btStatus(3, false, "BT socket error");
				return;
			}
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				btStatus(4, false, "BT not connected");
			}
			btStatus(4, false, "BT not connected");
			return;
		}
		if (btSocket!=null) {
			mConnectedThread = new ConnectedThread(btSocket);
			mConnectedThread.start();	
		} else {
			btStatus(3, false, "BT socket error");
			return;
		}
	}

	public void disconnect () {
		if (BTSucces<4) return;
		try     {
			if (btSocket!=null) {
				btSocket.close(); 
			} else {
				btStatus(3, false, "BT socket error");
			}
		} catch (IOException e2) {
			btStatus(4, false, "BT not connected");
		}

	}

	private void statusText(String message){
		MessageHandlerToMain.obtainMessage(RECIEVE_MESSAGE_BTSTATUS, MessageChannel, ((BTSucces>3) ? 1 : 0 ), message).sendToTarget();
	}

	public void send_command(String command) {
		if (BTSucces<5) return;
		if (btSocket==null) {
			btStatus(3, false, "BT socket error");
		}
		if (BTSucces>4) {
			if (mConnectedThread!=null) {
				mConnectedThread.write(command);	// Send command through Bluetooth
			} else {
				btStatus(3, false, "BT socket error");
			}
		}
	}

	private class ConnectedThread extends Thread {
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			// Get the input and output streams, using temp objects because member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
				btStatus(5, true, "BT stream OK");
			} catch (IOException e) {
				btStatus(5, false, "BT stream error");
			}
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		} // ConnectedThread constructor

		public void run() {
			int readbyte; // bytes returned from read()
			int buflength = 0; // length of the buffer
			String sBuffer=""; // buffer for received characters 

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					readbyte = mmInStream.read();        // Get number of bytes and message in "buffer"
					sBuffer+=(char)readbyte;
					buflength++;
					if ((buflength>200) || (readbyte<32) || (readbyte==35)) {
						// data received: send through handler to MainActivity
						MessageHandlerToMain.obtainMessage(RECIEVE_MESSAGE_BTDATA, MessageChannel, -1, sBuffer).sendToTarget();     
						sBuffer="";
						buflength=0;
					}
				} catch (IOException e) {
					btStatus(4, false, "BT not connected");
					break;
				}
			}
		} // run

		/* Call this from the main activity to send data to the remote device */
		public void write(String message) {
			byte[] msgBuffer = message.getBytes();
			try {
				mmOutStream.write(msgBuffer);
				mmOutStream.flush();
				btStatus(6, true, "BT OK");
			} catch (IOException e) {
				btStatus(4, false, "BT not connected");
			}
		} // write
	} // ConnectedThread class

}
