// Include in AndroidManifest.xml:
//    <uses-feature android:name="android.hardware.usb.accessory"/>

package com.example.EtchControl;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.util.Log;

public class USBFunctions { //extends { //

	/*declare a FT311 UART interface variable*/
	//public FT311UARTInterface uartInterface;
	private static final String ACTION_USB_PERMISSION =  "com.UARTTest.USB_PERMISSION";	
	public UsbManager usbmanager;
	public UsbAccessory usbaccessory;
	public PendingIntent mPermissionIntent;
	public ParcelFileDescriptor filedescriptor = null;
	public FileInputStream inputstream = null;
	public FileOutputStream outputstream = null;
	public boolean mPermissionRequestPending = false;

	private byte [] usbdata; 
	private byte []	writeusbdata;
	private byte [] readBuffer; /*circular buffer*/
	private int readcount;
	private int totalBytes;
	private int writeIndex;
	private int readIndex;
	private byte status;
	final int  maxnumbytes = 65536;

	public boolean datareceived = false;
	public boolean READ_ENABLE = false;
	public boolean accessory_attached = false;

	public Context global_context;

	public static String ManufacturerString = "mManufacturer=FTDI";
	public static String ModelString1 = "mModel=FTDIUARTDemo";
	public static String ModelString2 = "mModel=Android Accessory FT312D";
	public static String VersionString = "mVersion=1.0";

	public SharedPreferences intsharePrefSettings;


	private static final String TAG = "USBFunctions";

	Handler MessageHandlerToMain;
	int MessageChannel;

	int	uSBSuccess=-1;	// 0: not initiate, 1: initiated

	final int RECIEVE_MESSAGE_USBSTATUS = 1;		// Constant for Handler: USB status
	final int RECIEVE_MESSAGE_USBDATA = 2;		// Constant for Handler: Received USB data

	int configsend=-1;
	
	String sText;

	public USBFunctions () { // constructor
		super();
	}

	public boolean connected() {
		return (uSBSuccess>0) ? true : false ;
	}

	public void Initiate (int channel, Handler MessageHandler, Context context) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); //dirty way to use socket in main thread
		StrictMode.setThreadPolicy(policy);                                                         // since Android 3

		/*shall we start a thread here or what*/
		usbdata = new byte[1024]; 
		writeusbdata = new byte[256];
		/*128(make it 256, but looks like bytes should be enough)*/
		readBuffer = new byte [maxnumbytes];


		readIndex = 0;
		writeIndex = 0;
		/***********************USB handling******************************************/

		usbmanager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		// Log.d("LED", "usbmanager" +usbmanager);
		mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		context.registerReceiver(mUsbReceiver, filter);
		inputstream = null;
		outputstream = null;

		MessageChannel=channel;
		MessageHandlerToMain = MessageHandler;  // Dit zet de Message Handler uit MainActivity gelijk aan de handler in de obkect BTfunctions

		new Thread(new Runnable() {  // Maak een thread
			StringBuilder sb = new StringBuilder(128);
			String resultstring;
			int chr,lngth,status,onemore;
			int readbyte; // bytes returned from read()
			int buflength = 0; // lengte van de buffer
			String sBuffer=""; // de buffer voor de ontvangen characters 
			public void run(){		 // en definieer de interface

				// Keep listening to the InputStream until an exception occurs
				while (true) {
					if (inputstream!=null) {
						do {
							onemore=0;
							try {
								readbyte = inputstream.read();        // Get number of bytes and message in "buffer"
								onemore=1;
								sBuffer+=(char)readbyte;
								buflength++;
								if ((buflength>200) || (readbyte<32) || (readbyte==35)) {
									// data ontvangen: stuur via de handler naar MainActivity
									MessageHandlerToMain.obtainMessage(RECIEVE_MESSAGE_USBDATA, MessageChannel, -1, sBuffer).sendToTarget();     
									sBuffer="";
									buflength=0;
								}
							} catch (IOException e) {
							}
						} while (onemore==1);
					}
					try {Thread.sleep(10);} catch (InterruptedException e) {}
				}

			}
		}).start(); // Thread
	}

	public void connect () {
		ResumeAccessory();
		//SetConfig(9600, (byte)8, (byte)2, (byte)0, (byte)0); // 9600 baud, 1 stop bit, no parity, no flow control
		if (accessory_attached) {
			uSBSuccess=1;
			statusText(1, "USB connected");
		} else {
			statusText(0, "USB disconnected");
		}

	}

	public void disconnect () {
		configsend=-1;
		DestroyAccessory(true);
		statusText(0, "USB disconnected");
	}

	public void send_command(String command) {
		byte [] usbdata = new byte[256];
		int lngth = 0;
		int st=-2;

		if (uSBSuccess>0) {
			lngth=command.length();
			if (lngth>255) {
				usbdata=command.substring(0, 256).getBytes();
			} else {
				usbdata=command.getBytes();
			}
			if (lngth > 0) {
				st=SendData(lngth, usbdata);
			}
		}
		//MessageHandlerToMain.obtainMessage(9, MessageChannel, -1, String.format("S %s %d %d %d",command,uSBSuccess,configsend,st)).sendToTarget();
	}

	private void statusText(int stat, String sMessage){
		Log.e(TAG, "statusText: "+sMessage);
		MessageHandlerToMain.obtainMessage(RECIEVE_MESSAGE_USBSTATUS, MessageChannel, stat, sMessage).sendToTarget();
	}

	public void SetConfig(int baud, byte dataBits, byte stopBits,
			byte parity, byte flowControl)
	{

		/*prepare the baud rate buffer*/
		writeusbdata[0] = (byte)baud;
		writeusbdata[1] = (byte)(baud >> 8);
		writeusbdata[2] = (byte)(baud >> 16);
		writeusbdata[3] = (byte)(baud >> 24);

		/*data bits*/
		writeusbdata[4] = dataBits;
		/*stop bits*/
		writeusbdata[5] = stopBits;
		/*parity*/
		writeusbdata[6] = parity;
		/*flow control*/
		writeusbdata[7] = flowControl;

		/*send the UART configuration packet*/
		Log.e(TAG,"SendPacket for config");
		if (SendPacket((int)8) == 1) configsend=1;
	}


	/*write data*/ 
	public int SendData(int numBytes, byte[] buffer) 					     
	{
		int status = 0;
		int st;

		if (configsend!=1) {
			SetConfig((int)9600,(byte)8,(byte)1,(byte)0,(byte)0);  // send default setting data for config
			//MessageHandlerToMain.obtainMessage(9, MessageChannel, -1, String.format("Config %d %d",configsend,configsend)).sendToTarget();
		}

		if (configsend==1) {
			/*
			 * if num bytes are more than maximum limit
			 */
			if(numBytes < 1){
				/*return the status with the error in the command*/
				return status;
			}

			/*check for maximum limit*/
			if(numBytes > 256){
				numBytes = 256;
			}

			/*prepare the packet to be sent*/
			for(int count = 0;count<numBytes;count++)
			{	
				writeusbdata[count] = buffer[count];
			}

			if(numBytes != 64)
			{
				st=SendPacket(numBytes);
				if ((status>-1) && (st==-1)) status=-1; else status=st;
			}
			else
			{
				byte temp = writeusbdata[63];
				st=SendPacket(63);
				if ((status>-1) && (st==-1)) status=-1;
				writeusbdata[0] = temp;
				st=SendPacket(1);
				if ((status>-1) && (st==-1)) status=-1;
			}
		}
		return status;
	}

	/*read data*/
	public byte ReadData(int numBytes,byte[] buffer, int [] actualNumBytes)
	{
		status = 0x00; /*success by default*/

		/*should be at least one byte to read*/
		if((numBytes < 1) || (totalBytes == 0)){
			actualNumBytes[0] = 0;
			status = 0x01;
			return status;
		}

		/*check for max limit*/
		if(numBytes > totalBytes)
			numBytes = totalBytes;

		/*update the number of bytes available*/
		totalBytes -= numBytes;

		actualNumBytes[0] = numBytes;	

		/*copy to the user buffer*/	
		for(int count = 0; count<numBytes;count++)
		{
			buffer[count] = readBuffer[readIndex];
			readIndex++;
			/*shouldnt read more than what is there in the buffer,
			 * 	so no need to check the overflow
			 */
			readIndex %= maxnumbytes;
		}
		return status;
	}

	/*method to send on USB*/
	private int SendPacket(int numBytes)
	{	
		int status=-1;
		Log.e(TAG,"SendPacket "+outputstream+" "+numBytes);
		try {
			if(outputstream != null){
				Log.e(TAG,"SendPacket write "+numBytes);
				outputstream.write(writeusbdata, 0,numBytes);
				status=1;
			}
		} catch (IOException e) {
			Log.e(TAG,"SendPacket error");
			e.printStackTrace();
		}
		return status;
	}

	/*resume accessory*/
	public int ResumeAccessory()
	{
		//Intent intent = getIntent();
		Log.e(TAG,"ResumeAccessory");
		if (inputstream != null && outputstream != null) {
			return 1;
		}

		UsbAccessory[] accessories = usbmanager.getAccessoryList();
		if(accessories != null) {
			Log.e(TAG,"ResumeAccessory Accessory Attached");
			//Toast.makeText(global_context, "Accessory Attached", Toast.LENGTH_SHORT).show();
		} else	{
			// return 2 for accessory detached case
			//Log.e(">>@@","ResumeAccessory RETURN 2 (accessories == null)");
			Log.e(TAG,"ResumeAccessory Accessory not Attached");
			accessory_attached = false;
			return 2;
		}

		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if( -1 == accessory.toString().indexOf(ManufacturerString))
			{
				//Toast.makeText(global_context, "Manufacturer is not matched!", Toast.LENGTH_SHORT).show();
				Log.e(TAG,"ResumeAccessory: "+accessory.toString().indexOf(ManufacturerString));
				return 1;
			}

			if( -1 == accessory.toString().indexOf(ModelString1) && -1 == accessory.toString().indexOf(ModelString2))
			{
				//Toast.makeText(global_context, "Model is not matched!", Toast.LENGTH_SHORT).show();
				return 1;
			}

			if( -1 == accessory.toString().indexOf(VersionString))
			{
				//Toast.makeText(global_context, "Version is not matched!", Toast.LENGTH_SHORT).show();
				return 1;
			}

			//Toast.makeText(global_context, "Manufacturer, Model & Version are matched!", Toast.LENGTH_SHORT).show();
			accessory_attached = true;

			if (usbmanager.hasPermission(accessory)) {
				OpenAccessory(accessory);
			} 
			else
			{
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						//Toast.makeText(global_context, "Request USB Permission", Toast.LENGTH_SHORT).show();
						usbmanager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {}

		return 0;
	}

	/*destroy accessory*/
	public void DestroyAccessory(boolean bConfiged){

		if(true == bConfiged){
			READ_ENABLE = false;  // set false condition for handler_thread to exit waiting data loop
			writeusbdata[0] = 0;  // send dummy data for instream.read going
			SendPacket(1);
		}
		else
		{
			//SetConfig(9600,(byte)1,(byte)8,(byte)0,(byte)0);  // send default setting data for config
			try{Thread.sleep(10);}
			catch(Exception e){}

			READ_ENABLE = false;  // set false condition for handler_thread to exit waiting data loop
			writeusbdata[0] = 0;  // send dummy data for instream.read going
			SendPacket(1);
		}

		try{Thread.sleep(10);}
		catch(Exception e){}			
		CloseAccessory();
	}

	/*********************helper routines*************************************************/		

	public void OpenAccessory(UsbAccessory accessory)
	{	
		// ToDo
		CloseAccessory();
		Log.e(TAG, "OpenAccessory ****************************");
		filedescriptor = usbmanager.openAccessory(accessory);
		Log.e(TAG, "OpenAccessory "+filedescriptor);
		//http://code.google.com/p/android/issues/detail?id=20545
		if(filedescriptor != null){
			usbaccessory = accessory;

			FileDescriptor fd = filedescriptor.getFileDescriptor();

			inputstream = new FileInputStream(fd);
			Log.e(TAG,"OpenAccessory inputstream "+inputstream);
			outputstream = new FileOutputStream(fd);
			Log.e(TAG,"OpenAccessory outputstream "+outputstream);
			/*check if any of them are null*/
			if(inputstream == null || outputstream==null){
				return;
			}
// ToDo
			if(READ_ENABLE == false){
				READ_ENABLE = true;
			}
		}
	}

	private void CloseAccessory()
	{
		try{
			if(filedescriptor != null)
				filedescriptor.close();

		}catch (IOException e){}

		try {
			if(inputstream != null)
				inputstream.close();
		} catch(IOException e){}

		try {
			if(outputstream != null)
				outputstream.close();

		}catch(IOException e){}
		/*FIXME, add the notfication also to close the application*/

		filedescriptor = null;
		inputstream = null;
		outputstream = null;

		// ToDo
		//System.exit(0);
	}



	/***********USB broadcast receiver*******************************************/
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() 
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) 
			{
				synchronized (this)
				{
					UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
					{
						OpenAccessory(accessory);
					} 
					else 
					{

					}
					mPermissionRequestPending = false;
				}
			} 
			else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) 
			{
				DestroyAccessory(true);
			}else
			{
				Log.d("LED", "....");
			}
		}	
	};

	/*usb input data handler*/


}
