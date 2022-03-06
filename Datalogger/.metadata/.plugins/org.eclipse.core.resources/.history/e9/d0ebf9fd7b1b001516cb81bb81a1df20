// Insert here your own package name
// Include in AndroidManifest.xml:
//    <uses-feature android:name="android.hardware.usb.host" />
// The library d2xx.jar is needed in libs/ (download from http://www.ftdichip.com/Drivers/D2XX/Android/d2xx.jar)
// Based on J2xxHyperTerm from FTDI

package com.example.EtchControl;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

public class USBHostFunctions extends Activity {

	// j2xx
	public static D2xxManager ftD2xx = null;
	FT_Device ftDev;
	int DevCount = -1;
	int currentPortIndex = -1;
	int portIndex = -1;
	int boardindex=0;

	enum DeviceStatus{
		DEV_NOT_CONNECT,
		DEV_NOT_CONFIG,
		DEV_CONFIG
	}
	
	boolean INTERNAL_DEBUG_TRACE = false; // Toast message for debug
	
    final String[] contentFormatItems = {"Character","Hexadecimal"};
    final String[] fontSizeItems = {"5","6","7","8","10","12","14","16","18","20"};
    final String[] echoSettingItems = {"On","Off"};
    
	// log tag
	final String TT = "Trace";
	final String TXS = "XM-Send";
	final String TXR = "XM-Rec";
	final String TYS = "YM-Send";
	final String TYR = "YM-Rec";
	final String TZS = "ZM-Send";
	final String TZR = "ZM-Rec";
	
	// handler event
	final int UPDATE_TEXT_VIEW_CONTENT = 0;
	final int UPDATE_SEND_FILE_STATUS = 1;
	final int UPDATE_SEND_FILE_DONE = 2;
	final int ACT_SELECT_SAVED_FILE_NAME = 3;
	final int ACT_SELECT_SAVED_FILE_FOLDER = 4;
	final int ACT_SAVED_FILE_NAME_CREATED = 5;	
	final int ACT_SELECT_SEND_FILE_NAME = 6;
	final int MSG_SELECT_FOLDER_NOT_FILE = 7;
	final int MSG_XMODEM_SEND_FILE_TIMEOUT = 8;
	final int UPDATE_MODEM_RECEIVE_DATA = 9;
	final int UPDATE_MODEM_RECEIVE_DATA_BYTES = 10;
	final int UPDATE_MODEM_RECEIVE_DONE = 11;
	final int MSG_MODEM_RECEIVE_PACKET_TIMEOUT = 12;
	final int ACT_MODEM_SELECT_SAVED_FILE_FOLDER = 13;
	final int MSG_MODEM_OPEN_SAVE_FILE_FAIL = 14;
	final int MSG_YMODEM_PARSE_FIRST_PACKET_FAIL = 15;
	final int MSG_FORCE_STOP_SEND_FILE = 16;
	final int UPDATE_ASCII_RECEIVE_DATA_BYTES = 17;
	final int UPDATE_ASCII_RECEIVE_DATA_DONE = 18;
	final int MSG_FORCE_STOP_SAVE_TO_FILE = 19;
	final int UPDATE_ZMODEM_STATE_INFO = 20;
	final int ACT_ZMODEM_AUTO_START_RECEIVE = 21;
	
	final int MSG_SPECIAL_INFO = 98;
	final int MSG_UNHANDLED_CASE = 99;

    final byte XON = 0x11;    /* Resume transmission */
    final byte XOFF = 0x13;    /* Pause transmission */
    
	// strings of file transfer protocols
    final String[] protocolItems = {"ASCII","XModem-CheckSum","XModem-CRC","XModem-1KCRC","YModem","ZModem"};
    String currentProtocol;
    
	final int MODE_GENERAL_UART = 0;
	final int MODE_X_MODEM_CHECKSUM_RECEIVE = 1;
	final int MODE_X_MODEM_CHECKSUM_SEND = 2;
	final int MODE_X_MODEM_CRC_RECEIVE = 3;
	final int MODE_X_MODEM_CRC_SEND = 4;
	final int MODE_X_MODEM_1K_CRC_RECEIVE = 5;
	final int MODE_X_MODEM_1K_CRC_SEND = 6;
	final int MODE_Y_MODEM_1K_CRC_RECEIVE = 7;
	final int MODE_Y_MODEM_1K_CRC_SEND = 8;
	final int MODE_Z_MODEM_RECEIVE = 9;
	final int MODE_Z_MODEM_SEND = 10;
	final int MODE_SAVE_CONTENT_DATA = 11;
		
	int transferMode = MODE_GENERAL_UART;
	int tempTransferMode = MODE_GENERAL_UART;

    // general data count
	int totalReceiveDataBytes = 0;
	int totalUpdateDataBytes = 0;
	
	boolean uart_configured = false;

	String uartSettings  = "";

	//public static final int maxReadLength = 256;
	byte[] usbdata;
	char[] readDataToText;
	public int iavailable = 0;

	public Context global_context;
	// END J2xx
	
	private static final String TAG = "USBHostFunctions";

	Handler MessageHandlerToMain;
	int MessageChannel;

	int	uSBSuccess=-1;	// 0: not initiate, 1: initiated

	final int RECIEVE_MESSAGE_USBSTATUS = 1;		// Constant for Handler: USB status
	final int RECIEVE_MESSAGE_USBDATA = 2;		// Constant for Handler: Received USB data

	String sText;

	public USBHostFunctions () { // constructor
	}

	public boolean connected() {
		if(ftDev == null || false == ftDev.isOpen()) {
			return false;			
		} else if (false == uart_configured) {return false;}
		return true;
	}

	public void Initiate (int channel, Handler MessageHandler, int boardidx, Context context) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); //dirty way to use socket in main thread
		StrictMode.setThreadPolicy(policy);                                                         // since Android 3

		Log.e(TAG, "Initiate started");
		global_context=context;
		MessageChannel=channel;
		boardindex=boardidx;
		MessageHandlerToMain = MessageHandler;  // This sets the messagehandler in this object

		try {
			ftD2xx = D2xxManager.getInstance(global_context);
		} catch (D2xxManager.D2xxException e) {}
		
		new Thread(new Runnable() {  // Make a thread
			StringBuilder sb = new StringBuilder(128);
			String resultstring;
			int chr,lngth,bytesinbuffer;
			byte[] readBuffer = new byte [16];
			boolean bBufferNotEmpty;
			public void run(){		 // and define the interface
				lngth=0;
				chr=-1;
				while (true) {
					if (ftDev!=null) {
						bBufferNotEmpty=true;
						while (bBufferNotEmpty) {
							bBufferNotEmpty=false;
							bytesinbuffer=ftDev.getQueueStatus();
							if (bytesinbuffer > 0) {
								bBufferNotEmpty=true;
								ftDev.read(readBuffer, 1);
								chr=readBuffer[0];
								sb.append((char)chr);
								lngth+=1;
								if ((lngth>100) || ((chr>=0) && (chr<32))) {
									resultstring = sb.toString();
									MessageHandlerToMain.obtainMessage(RECIEVE_MESSAGE_USBDATA, MessageChannel, -1, resultstring).sendToTarget();
									sb.setLength(0);
									lngth=0;
								}
							}
						}
					}
					try {Thread.sleep(1);} catch (InterruptedException e) {}// Sleep 1 ms
				} // while
			}
		}).start(); // Thread
	}

	public void connect () {
		// always check whether there is a device or not
		createDeviceList();
		//if(DevCount > 0) {
			//connectFunction();
			ftDev = ftD2xx.openByIndex(global_context, boardindex);
			if (ftDev==null) Log.e(TAG,"ftDEV null"); else Log.e(TAG,"ftDEV exists"); 
			statusText(0, "USBHost not connected");
			ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
			ftDev.setBaudRate(9600);
			ftDev.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8, D2xxManager.FT_STOP_BITS_1, D2xxManager.FT_PARITY_NONE);
			ftDev.setFlowControl(D2xxManager.FT_FLOW_NONE, XON, XOFF);
		statusText(1, "USBHost Connected");;
		uart_configured = true;

	}

	public void disconnect () {
		/*
		DevCount = -1;
		currentPortIndex = -1;
		//bReadTheadEnable = false;
		//try {
		//	Thread.sleep(50);
		//}
		//catch (InterruptedException e) {e.printStackTrace();}
		if(ftDev != null) {
			if( true == ftDev.isOpen()) {
				ftDev.close();
			}
		}
		statusText(0, "USB disconnected");
		*/
	}

	public void send_command(String command) {
		byte [] usbdata = new byte[256];
		int lngth = 0;

//		if (uSBSuccess>1) {
		if (ftDev!=null) {
			lngth=command.length();
			if (lngth>255) {
				usbdata=command.substring(0, 256).getBytes();
			} else {
				usbdata=command.getBytes();
			}
			if (lngth > 0) {
				ftDev.write(usbdata, lngth);
			}		
		}
	}

	private void statusText(int stat, String sMessage){
		Log.e(TAG, "statusText: "+sMessage);
		MessageHandlerToMain.obtainMessage(RECIEVE_MESSAGE_USBSTATUS, MessageChannel, stat, sMessage).sendToTarget();
	}

	public void createDeviceList()
	{
		int tempDevCount = ftD2xx.createDeviceInfoList(global_context);
		Log.e(TAG, "tempDevCount "+tempDevCount);
		
		if (tempDevCount > 0) {
			if( DevCount != tempDevCount ) {
				DevCount = tempDevCount;
			}
		}
		else {
			DevCount = -1;
			currentPortIndex = -1;
		}
	}

	
	private void connectFunction()
	{
		if(null == ftDev) {
			ftDev = ftD2xx.openByIndex(global_context, 0);
		} else {
			ftDev = ftD2xx.openByIndex(global_context, 0);
		}
		uart_configured = false;

		if(ftDev == null) {
			return;
		}
			
		if (true == ftDev.isOpen()) {
			currentPortIndex = portIndex;
			//if(false == bReadTheadEnable) {	
			//	readThread = new ReadThread(handler);
			//	readThread.start();
			//}
		}
	}
	
	private DeviceStatus checkDevice()
	{
		if(ftDev == null || false == ftDev.isOpen())
		{
			return DeviceStatus.DEV_NOT_CONNECT;			
		}
		else if(false == uart_configured)
		{
			//midToast("CHECK: uart_configured == false", Toast.LENGTH_SHORT);
			return DeviceStatus.DEV_NOT_CONFIG;			
		}
		
		return DeviceStatus.DEV_CONFIG;
		
	}
	
	void setConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl)
	{
		// configure port
		// reset to UART mode for 232 devices
		ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

		ftDev.setBaudRate(baud);

		switch (dataBits)
		{
		case 7:
			dataBits = D2xxManager.FT_DATA_BITS_7;
			break;
		case 8:
			dataBits = D2xxManager.FT_DATA_BITS_8;
			break;
		default:
			dataBits = D2xxManager.FT_DATA_BITS_8;
			break;
		}

		switch (stopBits)
		{
		case 1:
			stopBits = D2xxManager.FT_STOP_BITS_1;
			break;
		case 2:
			stopBits = D2xxManager.FT_STOP_BITS_2;
			break;
		default:
			stopBits = D2xxManager.FT_STOP_BITS_1;
			break;
		}

		switch (parity)
		{
		case 0:
			parity = D2xxManager.FT_PARITY_NONE;
			break;
		case 1:
			parity = D2xxManager.FT_PARITY_ODD;
			break;
		case 2:
			parity = D2xxManager.FT_PARITY_EVEN;
			break;
		case 3:
			parity = D2xxManager.FT_PARITY_MARK;
			break;
		case 4:
			parity = D2xxManager.FT_PARITY_SPACE;
			break;
		default:
			parity = D2xxManager.FT_PARITY_NONE;
			break;
		}

		ftDev.setDataCharacteristics(dataBits, stopBits, parity);

		short flowCtrlSetting;
		switch (flowControl)
		{
		case 0:
			flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
			break;
		case 1:
			flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
			break;
		case 2:
			flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
			break;
		case 3:
			flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
			break;
		default:
			flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
			break;
		}

		ftDev.setFlowControl(flowCtrlSetting, XON, XOFF);

		uart_configured = true;
	}
	
}
