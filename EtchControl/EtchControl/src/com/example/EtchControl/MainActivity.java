package com.example.EtchControl;


// A6 - output Exposure Unit
// A7 - output Heater Developer bath
// B0 - output Heater Etch bath
// B1 - Buzzer
// B4 - NTC Developer Bath
// B3 - NTC Etch Bath

// use ctrl-shift-o to automatically get imports
// Note: android.R means an error in one of the xml files
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity  {

	// ************************************************************************************************
	// *************************  Variables and object Declaration ************************************
	// ************************************************************************************************

	// Objects for graphical elements
	TextView tvBTStatus; 			// Textview for Bluetooth status

	TextView tvDevelopTemp; 		// TextView for Development bath Temperatue
	TextView tvEtchTemp; 			// TextView for Etch bath Temperature
	TextView tvDevelopTimerStatus; 	// TextView for Development Timer Status
	TextView tvEtchTimerStatus; 	// TextView for Etch Timer Status
	TextView tvExpTimerStatus; 		// TextView for Exposure Timer Status
	TextView tvDevelopHeaterStatus; // TextView for Development Temperature Status
	TextView tvEtchHeaterStatus;	// TextView for Etch Temperature Status
	TextView tvDebug; 				// TextView for Debug text

	Button btBTConnect; 			// Button for connecting to Bluetooth
	Button btDevelopTempOn; 		// Button for Developer temperature control On
	Button btDevelopTempOff; 		// Button for Developer temperature control Off
	Button btEtchTempOn; 			// Button for Etch temperature control On
	Button btEtchTempOff; 			// Button for Etch temperature control Off
	Button btDevelopTimerStart; 	// Button for start Developer Timer
	Button btDevelopTimerStop; 		// Button for stop Developer Timer
	Button btDevelopTimerReset; 	// Button for Reset Developer Timer
	Button btEtchTimerStart; 		// Button for start Etch Timer
	Button btEtchTimerStop; 		// Button for stop Etch Timer
	Button btEtchTimerReset; 		// Button for Reset Etch Timer
	Button btExpTimerStart; 		// Button for start Exposure Timer
	Button btExpTimerStop; 			// Button for stop Exposure Timer
	Button btExpTimerReset; 		// Button for Reset Exposure Timer

	// Handling the AndroidIO board
	IOBoardFunctions IOBoard = new IOBoardFunctions();	// Object for the communications with the AndroidIO board
	int iIOBoardConnectionMode = 1;
	private String BTAddress = "20:14:05:20:12:11";   	// MAC-address of Bluetooth module (is set in settings, but you can edit this line)
	private String IPAddress = "192.168.178.25"; 	  	// IP address (is set in settings, but you can edit this line)
	private int IPPort = 2000;							// IP port  (is set in settings, but you can edit this line)
	private int USBHostPort = 0;					  	// index of USB board (is set in settings, but you can edit this line)
	boolean onpausedelay=false;	// delays disconnect at onPause

	// Variables for the functionality
	boolean bRunThread=true;						// boolean indicating whether thread should run (onResume, onPause)

	boolean bBTStatusOkIdc = false;			// Indicate BTStatus in green if connected?
	boolean bDevelopHeaterOn = false;		// Status Heater of the Developer bath is on
	boolean bEtchHeaterOn = false;			// Status Heater of the Etching bath is on
	boolean bExpUnitOn = false;				// Status Exposure Unit is on
	boolean bDevelopHeaterCtrlOn = false;	// Status Heater of control of the Developer bath is on
	boolean bEtchHeaterCtrlOn = false;		// Status Heater of control of the Etching bath is on
	boolean bDevelopTimerOn = false;		// Status Timer of Developer
	boolean bEtchTimerOn = false;			// Status Timer of Etch
	boolean bExpTimerOn = false;			// Status Timer of Exposure
	int iDevelopCountdown = 0;				// Countdown timer for Developer
	int iEtchCountdown = 0;					// Countdown timer for Etch
	int iExpCountdown = 0;					// Countdown timer for Exposure
	int iDevelopCountdownMax = 0;			// Initial countdown value for timer for Developer
	int iEtchCountdownMax = 0;				// Initial countdown value for timer for Etch
	int iExpCountdownMax = 0;				// Initial countdown value for timer for Exposure
	boolean bDevelopNTC = true;				// Is Developer temperature sensor present
	boolean bEtchNTC = true;				// Is Etch temperature sensor present
	boolean bDevelopTempIdc = true;			// Color for Developer temperature if within range? 	
	boolean bEtchTempIdc = true;			// Color for Etch temperature if within range? 	
	boolean bDevelopTempRng = false;		// Flag for Developer temperature if within range 	
	boolean bEtchTempRng = false;			// Flag for Etch temperature if within range 	
	double dDevelopTemp = -400d;			// Temperature of Develop bath
	double dEtchTemp = -400d;				// Temperature of Etch bath
	double dDevelopTempMax = 22d;			// Max temperature for switching of Developer heater
	double dDevelopTempMin = 20d;			// Max temperature for switching of Developer heater
	double dEtchTempMax = 47d;				// Max temperature for switching of Etch heater
	double dEtchTempMin = 43d;				// Max temperature for switching of Etch heater
	double dDevelopTempMaxIdc = 23d;		// Max temperature for indication of Developer heater
	double dDevelopTempMinIdc = 19d;		// Max temperature for indication of Developer heater
	double dEtchTempMaxIdc = 48d;			// Max temperature for indication of Etch heater
	double dEtchTempMinIdc = 42d;			// Max temperature for indication of Etch heater
	boolean bDevelopTempRead =  false;		// flag whether Developer temperature is read
	boolean bEtchTempRead =  false;			// flag whether Etch temperature is read
	double dDevelopBeta = 3880d;			// Beta value of the NTC in the Developer bath
	double dEtchBeta = 3880d;				// Beta value of the NTC in the Etch bath
	int iTempReadWDT = 0;					// watch dog timer whether 
	final int iTempReadWDTMax = 20;			// Max countdown value for watch dog in 100ms (20 = 2 seconds)
	int iBuzzCountdown = 0;					// Countdown for buzzer on
	int iBuzzCountdownMax = 50;				// Maximum for buzzer Countdown in 100 ms (100 = 10 second)
	boolean bBuzzOn = false;				// Flag whether buzzer is on
	boolean bDebugWrite = true;				// Flag whether response of AndroidIO board is shown
	boolean bExpBuzz = true;				// Flag whether Buzzer buzzs after Exposure timer
	boolean bExpBeep = true;				// Flag whether Android Device beeps after Exposure timer
	boolean bDevBuzz = true;				// Flag whether Buzzer buzzs after Developer timer
	boolean bDevBeep = true;				// Flag whether Android Device beeps after Developer timer
	boolean bEtchBuzz = true;				// Flag whether Buzzer buzzs after Etch timer
	boolean bEtchBeep = true;				// Flag whether Android Device beeps after Etch timer

	// Database for user settings
	SharedPreferences prefs;				// object prefs is the database for the settings
	OnSharedPreferenceChangeListener spListener;  // listener for changed settings

	// Handler for messages between objects
	Handler hndMessage;							// The handler for sending messages
	final int RECIEVE_MESSAGE_CONSTATUS = 1;	// Constant for Handler: Bluetooth/WiFi status
	final int RECIEVE_MESSAGE_CONDATA = 2;		// Constant for Handler: Received Bluetooth/WiFi data
	final int RECIEVE_MESSAGE_UPDATE = 3;		// Constant for Handler: Update UI
	final int RECIEVE_MESSAGE_CONCOMMAND = 4;	// Constant for Handler: Send command to AndroidIO Board

	final int RMB_DEV_ON = 1;					// for RECIEVE_MESSAGE_CONCOMMAND: Switch Develop Heater on
	final int RMB_DEV_OFF = 2;					// for RECIEVE_MESSAGE_CONCOMMAND: Switch Develop Heater off
	final int RMB_ETCH_ON = 3;					// for RECIEVE_MESSAGE_CONCOMMAND: Switch Etch Heater on
	final int RMB_ETCH_OFF = 4;					// for RECIEVE_MESSAGE_CONCOMMAND: Switch Etch Heater off
	final int RMB_EXP_ON = 3;					// for RECIEVE_MESSAGE_CONCOMMAND: Switch Exposure unit on
	final int RMB_EXP_OFF = 4;					// for RECIEVE_MESSAGE_CONCOMMAND: Switch Exposure off
	final int RMB_DEV_BUZ_ON = 5;				// for RECIEVE_MESSAGE_CONCOMMAND: Turn buzzer on (developer)
	final int RMB_DEV_BUZ_OFF = 6;				// for RECIEVE_MESSAGE_CONCOMMAND: Turn buzzer off (developer)
	final int RMB_ETCH_BUZ_ON = 7;				// for RECIEVE_MESSAGE_CONCOMMAND: Turn buzzer on (etch)
	final int RMB_ETCH_BUZ_OFF = 8;				// for RECIEVE_MESSAGE_CONCOMMAND: Turn buzzer off (etch)
	final int RMB_EXP_BUZ_ON = 9;				// for RECIEVE_MESSAGE_CONCOMMAND: Turn buzzer on (exposure)
	final int RMB_EXP_BUZ_OFF = 10;				// for RECIEVE_MESSAGE_CONCOMMAND: Turn buzzer off (exposure)
	final int RMB_BUZ_OFF = 11;					// for RECIEVE_MESSAGE_CONCOMMAND: Turn buzzer off (all cases)
	final int RMB_READ_TEMP = 12;				// for RECIEVE_MESSAGE_CONCOMMAND: Read temperatures

	// Audio
	private final double duration = 5.0; // seconds 
	private final int sampleRate = 44100; 
	private final int numSamples = (int) duration * sampleRate; 
	private final byte soundWave[] = new byte[1 * numSamples];
	final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 
			sampleRate, AudioFormat.CHANNEL_OUT_MONO, 
			AudioFormat.ENCODING_PCM_8BIT, numSamples, 
			AudioTrack.MODE_STATIC); 


	// ************************************************************************************************
	// *************************************  Methodes  ***********************************************
	// ************************************************************************************************

	@SuppressLint("HandlerLeak") 	// Prevent warning for the message handler

	@Override
	public void onCreate(Bundle savedInstanceState){  // First step in the Android life cycle 
		super.onCreate(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);	// the user settings database for this app

		setContentView(R.layout.activity_main);		// Create the layout from the xml file in src/layout/activity_main.xml

		RelativeLayout rView = (RelativeLayout)findViewById(R.id.view);

		btBTConnect = (Button) findViewById(R.id.idBTConnect);
		tvBTStatus=(TextView)findViewById(R.id.idBTStatus);
		tvDebug=(TextView)findViewById(R.id.idDebugtext);

		tvDevelopTimerStatus=(TextView)rView.findViewById(R.id.idDevelopTimerStat);
		tvEtchTimerStatus=(TextView)findViewById(R.id.idEtchTimerStat);
		tvExpTimerStatus=(TextView)findViewById(R.id.idExpTimerStat);
		tvDevelopHeaterStatus=(TextView)rView.findViewById(R.id.idDevelopTempStat);
		tvEtchHeaterStatus=(TextView)findViewById(R.id.idEtchTempStat);
		tvDevelopTemp=(TextView)rView.findViewById(R.id.idDevelopTempTemp);
		tvEtchTemp=(TextView)findViewById(R.id.idEtchTempTemp);

		btDevelopTempOn = (Button) findViewById(R.id.idDevelopTempOn);
		btDevelopTempOff = (Button) findViewById(R.id.idDevelopTempOff);
		btEtchTempOn = (Button) findViewById(R.id.idEtchTempOn);
		btEtchTempOff = (Button) findViewById(R.id.idEtchTempOff);
		btDevelopTimerStart = (Button) findViewById(R.id.idDevelopTimerStart);
		btDevelopTimerStop = (Button) findViewById(R.id.idDevelopTimerStop);
		btDevelopTimerReset = (Button) findViewById(R.id.idDevelopTimerReset);
		btEtchTimerStart = (Button) findViewById(R.id.idEtchTimerStart);
		btEtchTimerStop = (Button) findViewById(R.id.idEtchTimerStop);
		btEtchTimerReset = (Button) findViewById(R.id.idEtchTimerReset);
		btExpTimerStart = (Button) findViewById(R.id.idExpTimerStart);
		btExpTimerStop = (Button) findViewById(R.id.idExpTimerStop);
		btExpTimerReset = (Button) findViewById(R.id.idExpTimerReset);

		hndMessage = new Handler() {							// Initiate the handler to send messages
			public void handleMessage(android.os.Message msgMessage) { // Android calls this if a message is sent. msgMessage is object received
				String sTekstMessage;
				int iTemp1;
				switch (msgMessage.what) {
				case RECIEVE_MESSAGE_CONSTATUS:					// message is a Bluetooth status update
					sTekstMessage=(String) msgMessage.obj;		// Convert text to string sTekstMessage
					tvBTStatus.setText(sTekstMessage);			// Put text in tvBTStatus TextView
					if (msgMessage.arg2==1) {
						init_AndroidIOBoard();					// if connected, first initiate pins
						if (bBTStatusOkIdc) {
							tvBTStatus.setBackgroundColor(Color.parseColor("#FF00C000"));
							tvBTStatus.setTextColor(Color.parseColor("#FF000000"));
						} else {
							tvBTStatus.setBackgroundColor(Color.parseColor("#00000000"));
							tvBTStatus.setTextColor(Color.parseColor("#FFC0C0C0"));
						}
					} else {
						tvBTStatus.setBackgroundColor(Color.parseColor("#FFB00000"));
						tvBTStatus.setTextColor(Color.parseColor("#FF000000"));
					}
					break;
				case RECIEVE_MESSAGE_CONDATA:					// message is data from Bluetooth module
					sTekstMessage=(String) msgMessage.obj; 		// Convert text to string sTekstMessage
					sTekstMessage=sTekstMessage.replace("\r","").replace("\n",""); // remove EOL characters
					//Log.d(TAG, "BT says: "+sTekstMessage);
					if (bDebugWrite) {
						if(sTekstMessage.length()>22) {
							tvDebug.setText("D: "+sTekstMessage.substring(0, 22));// put response from AndroidIO board to status TextView
						} else {
							tvDebug.setText("D: "+sTekstMessage);// put response from AndroidIO board to status TextView
						}
					}
					if (sTekstMessage.length()>3) {				// A valid response contains at least 3 characters 
						//Log.d(TAG, "Read: " + sTekstMessage);
						if (bDevelopNTC) {						// NTC in Develop bath?
							if ((sTekstMessage.substring(0, 4).equals("R B4")) && (sTekstMessage.length()>5)) { // datalog status
								iTemp1=Integer.parseInt(sTekstMessage.substring(5, sTekstMessage.length()), 10);
								dDevelopTemp=convNTC2Temp(iTemp1,dDevelopBeta);
								bDevelopTempRead=true;
								iTempReadWDT=iTempReadWDTMax;		// reset watch dog timer
							} // B4 
						} // bDevelopNTC
						if (bEtchNTC) {						// NTC in Develop bath?
							if ((sTekstMessage.substring(0, 4).equals("R B3")) && (sTekstMessage.length()>5)) { // datalog status
								iTemp1=Integer.parseInt(sTekstMessage.substring(5, sTekstMessage.length()), 10);
								dEtchTemp=convNTC2Temp(iTemp1,dEtchBeta);
								bEtchTempRead=true;
								iTempReadWDT=iTempReadWDTMax;		// reset watch dog timer
							} // B3 
						} // bEtchNTC
					}
					break;
				case RECIEVE_MESSAGE_UPDATE:					// message to update GUI
					updateUI();
					break;
				case RECIEVE_MESSAGE_CONCOMMAND:					// message is a command
					if (msgMessage.arg1==RMB_DEV_ON) {switchDevelopHeater(true);}
					if (msgMessage.arg1==RMB_DEV_OFF) {switchDevelopHeater(false);}
					if (msgMessage.arg1==RMB_ETCH_ON) {switchEtchHeater(true);}
					if (msgMessage.arg1==RMB_ETCH_OFF) {switchEtchHeater(false);}
					if (msgMessage.arg1==RMB_EXP_ON) {switchExpUnit(true);}
					if (msgMessage.arg1==RMB_EXP_OFF) {switchExpUnit(false);}
					if (msgMessage.arg1==RMB_DEV_BUZ_ON) {if (bExpBuzz) {iBuzzCountdown=iBuzzCountdownMax; switchBuzz(true);}; if (bExpBeep) {produceBeep();}}
					if (msgMessage.arg1==RMB_ETCH_BUZ_ON) {if (bExpBuzz) {iBuzzCountdown=iBuzzCountdownMax; switchBuzz(true);}; if (bExpBeep) {produceBeep();}}
					if (msgMessage.arg1==RMB_EXP_BUZ_ON) {if (bExpBuzz) {iBuzzCountdown=iBuzzCountdownMax; switchBuzz(true);}; if (bExpBeep) {produceBeep();}}
					if (msgMessage.arg1==RMB_BUZ_OFF) {switchBuzz(false);}
					if (msgMessage.arg1==RMB_READ_TEMP) {
						bDevelopTempRead=false;
						bEtchTempRead=false;
						MyConnection_send_command("r b3"+(char)10+"r b4"+(char)10);
						//Log.d(TAG, "Read Temperature");
					}
					//if (msgMessage.arg2==2) {tvLogStatus.setText("---");}
					break;
				} // switch
			}; // handleMessage
		}; // hndMessage

		get_options_settings(false);	// Read all settings from the database

		prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {  
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {  // Android calls this when user settings are changed
				get_options_settings(true);
			} // onSharedPreferenceChanged
		}); // prefs.registerOnSharedPreferenceChangeListener

		if (iIOBoardConnectionMode==0) {	// Bluetooth
			IOBoard.Initiate(0, hndMessage, BTAddress, 0, iIOBoardConnectionMode,this);	// Initiate the Bluetooth connection
		}
		if (iIOBoardConnectionMode==1) {	// WiFi
			IOBoard.Initiate(0, hndMessage, IPAddress, IPPort, iIOBoardConnectionMode, this);	// Initiate the WiFi connection
		}
		if (iIOBoardConnectionMode==2) {	// USB Accessory
			IOBoard.Initiate(0, hndMessage, " ", 0, iIOBoardConnectionMode, this);	// Initiate the USB Accessory connection
		}
		if (iIOBoardConnectionMode==3) {	// USB Host
			IOBoard.Initiate(0, hndMessage, " ", USBHostPort, iIOBoardConnectionMode, this);	// Initiate the USB Host connection
		}

		btBTConnect.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: connect to AndroidIOboard
			public void onClick(View v) {
				IOBoard.connect();
			}
		}); // btBTConnect.setOnClickListener

		btDevelopTempOn.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: set developer temperature control on
			public void onClick(View v) {
				bDevelopHeaterCtrlOn=true;
			}
		}); // btDevelopTempOn.setOnClickListener

		btDevelopTempOff.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: set developer temperature control off
			public void onClick(View v) {
				bDevelopHeaterCtrlOn=false;
				switchDevelopHeater(false);
			}
		}); // btDevelopTempOff.setOnClickListener

		btEtchTempOn.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: set etch temperature control on
			public void onClick(View v) {
				bEtchHeaterCtrlOn=true;
			}
		}); // btEtchTempOn.setOnClickListener

		btEtchTempOff.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: set etch temperature control off
			public void onClick(View v) {
				bEtchHeaterCtrlOn=false;
				switchEtchHeater(false);
			}
		}); // btEtchTempOff.setOnClickListener

		btDevelopTimerStart.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: start developer timer
			public void onClick(View v) {
				if (iDevelopCountdown==0) setDevelopTimerMax(); // otherwise continue
				bDevelopTimerOn=true;
			}
		}); // btDevelopTimerStart.setOnClickListener

		btDevelopTimerStop.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: stop developer timer
			public void onClick(View v) {
				bDevelopTimerOn=false;
			}
		}); // btDevelopTimerStop.setOnClickListener

		btDevelopTimerReset.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: reset developer timer
			public void onClick(View v) {
				bDevelopTimerOn=false;
				setDevelopTimerMax();
			}
		}); // btDevelopTimerReset.setOnClickListener

		btEtchTimerStart.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: start etch timer
			public void onClick(View v) {
				if (iEtchCountdown==0) setEtchTimerMax(); // otherwise continue
				bEtchTimerOn=true;
			}
		}); // btEtchTimerStart.setOnClickListener

		btEtchTimerStop.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: stop etch timer
			public void onClick(View v) {
				bEtchTimerOn=false;
			}
		}); // btEtchTimerStop.setOnClickListener

		btEtchTimerReset.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: reset etch timer
			public void onClick(View v) {
				bEtchTimerOn=false;
				setEtchTimerMax();
			}
		}); // btEtchTimerReset.setOnClickListener

		btExpTimerStart.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: start exposure timer
			public void onClick(View v) {
				if (iExpCountdown==0) setExpTimerMax(); // otherwise continue
				switchExpUnit(true);
				bExpTimerOn=true;
			}
		}); // btExpTimerStart.setOnClickListener

		btExpTimerStop.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: stop exposure timer
			public void onClick(View v) {
				bExpTimerOn=false;
				switchExpUnit(false);
			}
		}); // btExpTimerStop.setOnClickListener

		btExpTimerReset.setOnClickListener(new OnClickListener() { // Android calls the button is clicked: reset exposure timer
			public void onClick(View v) {
				bExpTimerOn=false;
				switchExpUnit(false);
				setExpTimerMax();
			}
		}); // btExpTimerReset.setOnClickListener

		new Thread(new Runnable() {  //Make a thread
			public void run(){		 // and define the interface
				int loopcnt=0;
				boolean bUpdateNeeded = false;
				while (true) {
					if (bRunThread) {
						loopcnt++;
						//Log.d(TAG, "Thread says: "+loopcnt);
						// Watchdog timer
						if (iTempReadWDT>0) {iTempReadWDT--;}		// decrease dog timer

						// Check temperatures, check within range and switch Heaters
						if (bDevelopTempRead) {						// Develop thermostat
							bDevelopTempRead=false;
							if((dDevelopTemp<dDevelopTempMinIdc) || (dDevelopTemp>dDevelopTempMaxIdc)) {bDevelopTempRng=false;} else {bDevelopTempRng=true;}
							if(bDevelopHeaterCtrlOn){
								if ((dDevelopTemp>dDevelopTempMax) && (bDevelopHeaterOn)) {	// High temperature reached while heater on?
									hndMessage.obtainMessage(RECIEVE_MESSAGE_CONCOMMAND, RMB_DEV_OFF, 0, " ").sendToTarget();
									bUpdateNeeded=true;
								}
								if ((dDevelopTemp<dDevelopTempMin) && (!bDevelopHeaterOn)) {	// Low temperature reached while heater off?
									hndMessage.obtainMessage(RECIEVE_MESSAGE_CONCOMMAND, RMB_DEV_ON, 0, " ").sendToTarget();
									bUpdateNeeded=true;
								}
							}
						}
						if (bEtchTempRead) {						// Etch thermostat
							bEtchTempRead=false;
							if((dEtchTemp<dEtchTempMinIdc) || (dEtchTemp>dEtchTempMaxIdc)) {bEtchTempRng=false;} else {bEtchTempRng=true;}
							if(bEtchHeaterCtrlOn){
								if ((dEtchTemp>dEtchTempMax) && (bEtchHeaterOn)) {	// High temperature reached while heater on?
									hndMessage.obtainMessage(RECIEVE_MESSAGE_CONCOMMAND, RMB_ETCH_OFF, 0, " ").sendToTarget();
									bUpdateNeeded=true;
								}
								if ((dEtchTemp<dEtchTempMin) && (!bEtchHeaterOn)) {	// Low temperature reached while heater off?
									hndMessage.obtainMessage(RECIEVE_MESSAGE_CONCOMMAND, RMB_ETCH_ON, 0, " ").sendToTarget();
									bUpdateNeeded=true;
								}
							}
						}

						// Timers
						if (bDevelopTimerOn) {									// Develop Timer
							iDevelopCountdown--;
							bUpdateNeeded=true;
							if (iDevelopCountdown<0) {
								bDevelopTimerOn=false;
								iDevelopCountdown=0;
								hndMessage.obtainMessage(RECIEVE_MESSAGE_CONCOMMAND, RMB_DEV_BUZ_ON, 0, " ").sendToTarget();
								setDevelopTimerMax();
							}
						}
						if (bEtchTimerOn) {									// Etch Timer
							iEtchCountdown--;
							bUpdateNeeded=true;
							if (iEtchCountdown<0) {
								bEtchTimerOn=false;
								iEtchCountdown=0;
								hndMessage.obtainMessage(RECIEVE_MESSAGE_CONCOMMAND, RMB_ETCH_BUZ_ON, 0, " ").sendToTarget();
								setEtchTimerMax();
							}
						}
						if (bExpTimerOn) {									// Exp Timer
							iExpCountdown--;
							bUpdateNeeded=true;
							if (iExpCountdown<0) {
								bExpTimerOn=false;
								iExpCountdown=0;
								hndMessage.obtainMessage(RECIEVE_MESSAGE_CONCOMMAND, RMB_EXP_OFF, 0, " ").sendToTarget();
								hndMessage.obtainMessage(RECIEVE_MESSAGE_CONCOMMAND, RMB_EXP_BUZ_ON, 0, " ").sendToTarget();
								setExpTimerMax();
							}
						}

						// Buzzer
						if (bBuzzOn) {
							iBuzzCountdown--;
							bUpdateNeeded=true;
							if (iBuzzCountdown<=0) {
								bBuzzOn=false;
								hndMessage.obtainMessage(RECIEVE_MESSAGE_CONCOMMAND, RMB_BUZ_OFF, 0, " ").sendToTarget();
							}
						}

						// Read temperatures once every second
						if ((loopcnt % 10) == 0) {
							hndMessage.obtainMessage(RECIEVE_MESSAGE_CONCOMMAND, RMB_READ_TEMP, 0, " ").sendToTarget();
						}
						// Loop completed, send update if needed and wait a little
						if ((bUpdateNeeded) || ((loopcnt % 10) == 5)) hndMessage.obtainMessage(RECIEVE_MESSAGE_UPDATE, -1, 1, " ").sendToTarget();
					} //if (bRunThread)
					try {Thread.sleep(100);} catch (InterruptedException e) {}// Sleep 100 ms
				} // while
			}
		}).start(); // Thread


	}  // onCreate

	@Override
	public void onResume() {	// Second phase in the life cycle
		super.onResume();
		onpausedelay=false;
		if (!onpausedelay) {IOBoard.connect();}
	} // onResume

	@Override
	public void onPause() {		// First step in the end of the life cycle
		super.onPause();
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				onpausedelay=true;
				try {Thread.sleep(1000);} catch (InterruptedException e) {} // delay for 1 second before disconnecting, to prevent flipping
				if (onpausedelay) {
					onpausedelay=false;
					IOBoard.disconnect();
				}
			}
		}, 0);
	} // onPause

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {		// Generate code for menu
		getMenuInflater().inflate(R.menu.main, menu);	// from xml file src/menu/menu.xml
		return true;
	} // onCreateOptionsMenu

	private static final String TAG = "Datalogger";

	public boolean onOptionsItemSelected(MenuItem item) {	// Is called by Android when menu is clicked
		switch (item.getItemId()) {
		case R.id.action_settings:	// start de user setting fragment
			Intent i = new Intent(MainActivity.this, UserSettingActivity.class); // Do not forget to modify AndroidManifest to allow
			startActivity(i);
			return true;
		case R.id.exit:
			finish();		// exit the app; normally not needed. But sometimes needed when Bluetooth is stuck.
			return true;
		default:
			return super.onOptionsItemSelected(item);
		} 
	} // onOptionsItemSelected

	private void updateUI() {
		// Temperature values 
		if (bDevelopNTC) {
			if ((dDevelopTemp<-380d) || (iTempReadWDT==0)) {
				tvDevelopTemp.setText("---");
				if (bDevelopTempIdc) {
					tvDevelopTemp.setBackgroundColor(Color.parseColor("#FFB00000"));
				} else {
					tvDevelopTemp.setBackgroundColor(Color.parseColor("#00000000"));
				}
			} else {
				tvDevelopTemp.setText(String.format("%-3.1f C", dDevelopTemp));
				if (bDevelopTempIdc) {
					if (bDevelopTempRng) {
						tvDevelopTemp.setBackgroundColor(Color.parseColor("#FF00B000"));
					} else {
						tvDevelopTemp.setBackgroundColor(Color.parseColor("#FFB00000"));
					}
				} else {
					tvDevelopTemp.setBackgroundColor(Color.parseColor("#00000000"));
				}
			}
		}
		if (bEtchNTC) {
			if ((dEtchTemp<-380d) || (iTempReadWDT==0)) {
				tvEtchTemp.setText("---");
				if (bEtchTempIdc) {
					tvEtchTemp.setBackgroundColor(Color.parseColor("#FFB00000"));
				} else {
					tvEtchTemp.setBackgroundColor(Color.parseColor("#00000000"));
				}
			} else {
				tvEtchTemp.setText(String.format("%-3.1f C", dEtchTemp));
				if (bEtchTempIdc) {
					if (bEtchTempRng) {
						tvEtchTemp.setBackgroundColor(Color.parseColor("#FF00B000"));
					} else {
						tvEtchTemp.setBackgroundColor(Color.parseColor("#FFB00000"));
					}
				} else {
					tvEtchTemp.setBackgroundColor(Color.parseColor("#00000000"));
				}
			}
		}

		// Heater control
		if (bDevelopHeaterOn) {
			tvDevelopHeaterStatus.setText("Heater ON");
			tvDevelopHeaterStatus.setBackgroundColor(Color.parseColor("#FF00B000"));
			tvDevelopHeaterStatus.setTextColor(Color.parseColor("#FF000000"));
		} else {
			tvDevelopHeaterStatus.setText("Heater OFF");
			tvDevelopHeaterStatus.setBackgroundColor(Color.parseColor("#00000000"));
			tvDevelopHeaterStatus.setTextColor(Color.parseColor("#FFC0C0C0"));
		}
		if (bDevelopHeaterCtrlOn) {
			tvDevelopHeaterStatus.setPaintFlags(tvDevelopHeaterStatus.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
			btDevelopTempOn.getBackground().setColorFilter(0xA000C000, PorterDuff.Mode.OVERLAY);
			btDevelopTempOff.getBackground().clearColorFilter();
		} else {
			tvDevelopHeaterStatus.setPaintFlags(tvDevelopHeaterStatus.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			btDevelopTempOn.getBackground().clearColorFilter();
			btDevelopTempOff.getBackground().setColorFilter(0xA0006000, PorterDuff.Mode.OVERLAY);
		}
		if (bEtchHeaterOn) {
			tvEtchHeaterStatus.setText("Heater ON");
			tvEtchHeaterStatus.setBackgroundColor(Color.parseColor("#FF00B000"));
			tvEtchHeaterStatus.setTextColor(Color.parseColor("#FF000000"));
		} else {
			tvEtchHeaterStatus.setText("Heater OFF");
			tvEtchHeaterStatus.setBackgroundColor(Color.parseColor("#00000000"));
			tvEtchHeaterStatus.setTextColor(Color.parseColor("#FFC0C0C0"));
		}
		if (bEtchHeaterCtrlOn) {
			tvEtchHeaterStatus.setPaintFlags(tvDevelopHeaterStatus.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
			btEtchTempOn.getBackground().setColorFilter(0xA000C000, PorterDuff.Mode.OVERLAY);
			btEtchTempOff.getBackground().clearColorFilter();
		} else {
			tvEtchHeaterStatus.setPaintFlags(tvDevelopHeaterStatus.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			btEtchTempOn.getBackground().clearColorFilter();
			btEtchTempOff.getBackground().setColorFilter(0xA0006000, PorterDuff.Mode.OVERLAY);
		}

		// Timer
		tvDevelopTimerStatus.setText(Integer.toString(iDevelopCountdown/10)+" s");
		if (bDevelopTimerOn) {
			btDevelopTimerStart.getBackground().setColorFilter(0xA000C000, PorterDuff.Mode.OVERLAY);
			btDevelopTimerStop.getBackground().clearColorFilter();
		} else {
			btDevelopTimerStart.getBackground().clearColorFilter();
			btDevelopTimerStop.getBackground().setColorFilter(0xA0006000, PorterDuff.Mode.OVERLAY);
		}
		tvEtchTimerStatus.setText(Integer.toString(iEtchCountdown/10)+" s");
		if (bEtchTimerOn) {
			btEtchTimerStart.getBackground().setColorFilter(0xA000C000, PorterDuff.Mode.OVERLAY);
			btEtchTimerStop.getBackground().clearColorFilter();
		} else {
			btEtchTimerStart.getBackground().clearColorFilter();
			btEtchTimerStop.getBackground().setColorFilter(0xA0006000, PorterDuff.Mode.OVERLAY);
		}
		tvExpTimerStatus.setText(Integer.toString(iExpCountdown/10)+" s");
		if (bExpTimerOn) {
			btExpTimerStart.getBackground().setColorFilter(0xA000C000, PorterDuff.Mode.OVERLAY);
			btExpTimerStop.getBackground().clearColorFilter();
		} else {
			btExpTimerStart.getBackground().clearColorFilter();
			btExpTimerStop.getBackground().setColorFilter(0xA0006000, PorterDuff.Mode.OVERLAY);
		}
	}

	// Send the commands to the AndroidIO board to set all the pins
	void init_AndroidIOBoard() {
		MyConnection_send_command("s a6 2 0"+(char)10);		// A6 as output: Light of exposure unit  
		try {Thread.sleep(10);} catch (InterruptedException e) {}// Sleep 10 ms
		MyConnection_send_command("s a7 2 0"+(char)10);		// A7 as output: Heater of developer
		try {Thread.sleep(10);} catch (InterruptedException e) {}// Sleep 10 ms
		MyConnection_send_command("s b0 2 0"+(char)10);		// B0 as output: Heater of etch
		try {Thread.sleep(10);} catch (InterruptedException e) {}// Sleep 10 ms
		MyConnection_send_command("s b1 2 0"+(char)10);		// B1 as output: Buzzer
		try {Thread.sleep(10);} catch (InterruptedException e) {}// Sleep 10 ms
		MyConnection_send_command("s b3 3"+(char)10);// B3 as ADC, 10 readings : Developer
		try {Thread.sleep(10);} catch (InterruptedException e) {}// Sleep 10 ms
		MyConnection_send_command("s b31 2"+(char)10);
		try {Thread.sleep(10);} catch (InterruptedException e) {}// Sleep 10 ms
		MyConnection_send_command("s b32 10"+(char)10);		
		try {Thread.sleep(10);} catch (InterruptedException e) {}// Sleep 10 ms
		MyConnection_send_command("s b4 3"+(char)10);// B4 as ADC, 10 readings : Developer
		try {Thread.sleep(10);} catch (InterruptedException e) {}// Sleep 10 ms
		MyConnection_send_command("s b41 2"+(char)10);
		try {Thread.sleep(10);} catch (InterruptedException e) {}// Sleep 10 ms
		MyConnection_send_command("s b42 10"+(char)10);		
		try {Thread.sleep(10);} catch (InterruptedException e) {}// Sleep 10 ms
	}

	// Switch the Developer heater (A7) on or off
	void switchDevelopHeater(boolean value) {
		if (value) {
			MyConnection_send_command("w a7 1"+(char)10);
			bDevelopHeaterOn=true;
		} else {
			MyConnection_send_command("w a7 0"+(char)10);
			bDevelopHeaterOn=false;
		}
	}

	// Switch the Etch heater (B0) on or off
	void switchEtchHeater(boolean value) {
		if (value) {
			MyConnection_send_command("w b0 1"+(char)10);
			bEtchHeaterOn=true;
		} else {
			MyConnection_send_command("w b0 0"+(char)10);
			bEtchHeaterOn=false;
		}
	}

	// Switch the Exposure (A6) on or off
	void switchExpUnit(boolean value) {
		if (value) {
			MyConnection_send_command("s a6 2 1"+(char)10);
			bExpUnitOn=true;
		} else {
			MyConnection_send_command("w a6 0"+(char)10);
			bExpUnitOn=false;
		}
	}

	// Switch the Buzzer (B1) on or off
	void switchBuzz(boolean value) {
		if (value) {
			MyConnection_send_command("w b1 1"+(char)10);
			bBuzzOn=true;
		} else {
			MyConnection_send_command("w b1 0"+(char)10);
			bBuzzOn=false;
		}
	}

	void MyConnection_send_command(String cmmd) {
		IOBoard.send_command(cmmd);
	}

	void setDevelopTimerMax() {
		iDevelopCountdown=iDevelopCountdownMax;
	}

	void setEtchTimerMax() {
		iEtchCountdown=iEtchCountdownMax;
	}

	void setExpTimerMax() {
		iExpCountdown=iExpCountdownMax;
	}

	// Converts a ADC value to the temperature, using the beta
	double convNTC2Temp(int ADCvalue, double BetaNTC) {
		double Beta=BetaNTC;
		if (Beta<1000d) Beta=1000d;
		if (Beta>10000d) Beta=10000d;
		double Roo=1e4d*Math.exp(-Beta/293d);
		double Vntc=(double)ADCvalue/10d;
		if (Vntc<0.5d) Vntc=1d;
		double Rntc=1e4/(1024d / Vntc-1);
		double temperature=Beta/Math.log(Rntc/Roo)-273;
		//Log.d(TAG, "Conv temperature: "+ADCvalue+" "+temperature);
		return temperature;
	}


	// Produce a beep by generating a sound file and play
	void produceBeep(){
		int idx;
		byte b;

		for (idx = 0; idx < numSamples; idx++) {
			if ((idx % 32000)<16000) {
				b = ((idx % 80) < 40) ? (byte)255 : (byte)0 ;
			} else {
				b = (byte)128 ;
			}
			soundWave[idx] = b;
		}
		soundWave[numSamples-1] = (byte) 128;

		if (audioTrack.getPlayState()!=AudioTrack.PLAYSTATE_STOPPED) {
			audioTrack.stop();
			audioTrack.reloadStaticData();
		}
		audioTrack.write(soundWave, 0, numSamples); 
		audioTrack.play();

	} 

	// read the settings. True: all. False: not the connection parameters (only at startup) 
	void get_options_settings(boolean onchange) {
		iExpCountdownMax = Integer.valueOf(prefs.getString("exp_time", "30")) * 10; // Exposure timer
		iDevelopCountdownMax = Integer.valueOf(prefs.getString("dev_time", "30")) * 10; // Developer timer
		iEtchCountdownMax = Integer.valueOf(prefs.getString("etch_time", "30")) * 10; // Etch timer

		dDevelopTempMax = Double.parseDouble(prefs.getString("dev_temp_mid", "30")) + Double.parseDouble(prefs.getString("dev_temp_hyst", "1")); // Max develop temperature
		dDevelopTempMin = Double.parseDouble(prefs.getString("dev_temp_mid", "30")) - Double.parseDouble(prefs.getString("dev_temp_hyst", "1")); // Min develop temperature
		dDevelopTempMaxIdc = Double.parseDouble(prefs.getString("dev_temp_mid", "30")) + Double.parseDouble(prefs.getString("dev_temp_ok", "3")); // Max develop temperature range
		dDevelopTempMinIdc = Double.parseDouble(prefs.getString("dev_temp_mid", "30")) - Double.parseDouble(prefs.getString("dev_temp_ok", "3")); // Min develop temperature range
		dEtchTempMax = Double.parseDouble(prefs.getString("etch_temp_mid", "30")) + Double.parseDouble(prefs.getString("etch_temp_hyst", "1")); // Max etchelop temperature
		dEtchTempMin = Double.parseDouble(prefs.getString("etch_temp_mid", "30")) - Double.parseDouble(prefs.getString("etch_temp_hyst", "1")); // Min etchelop temperature
		dEtchTempMaxIdc = Double.parseDouble(prefs.getString("etch_temp_mid", "30")) + Double.parseDouble(prefs.getString("etch_temp_ok", "3")); // Max etchelop temperature range
		dEtchTempMinIdc = Double.parseDouble(prefs.getString("etch_temp_mid", "30")) - Double.parseDouble(prefs.getString("etch_temp_ok", "3")); // Min etchelop temperature range

		bDevelopTempIdc = prefs.getBoolean("dev_show_temp_OK",true);	// Color for Developer temperature if within range? 
		bEtchTempIdc = prefs.getBoolean("exp_show_temp_OK",true);		// Color for Etch temperature if within range? 
		bBTStatusOkIdc = prefs.getBoolean("bt_green",true);				// Indicate BTStatus in green if connected?

		bDevelopNTC = prefs.getBoolean("dev_ntc",true);					// Etch temperature sensor present?
		bEtchNTC = prefs.getBoolean("etch_ntc",true);					// Etch temperature sensor present?
		bDebugWrite = prefs.getBoolean("debug",true);					// Show debuginfo?
		bExpBuzz = prefs.getBoolean("exp_buzz",true);					// Flag whether Buzzer buzzs after Exposure timer
		bExpBeep = prefs.getBoolean("exp_beep",true);					// Flag whether Android Device beeps after Exposure timer
		bDevBuzz = prefs.getBoolean("dev_buzz",true);					// Flag whether Buzzer buzzs after Developer timer
		bDevBeep = prefs.getBoolean("dev_beep",true);					// Flag whether Android Device beeps after Developer timer
		bEtchBuzz = prefs.getBoolean("etch_buzz",true);					// Flag whether Buzzer buzzs after Etch timer
		bEtchBeep = prefs.getBoolean("etch_beep",true);					// Flag whether Android Device beeps after Etch timer

		dDevelopBeta = Double.parseDouble(prefs.getString("dev_beta", "3880"));
		dEtchBeta = Double.parseDouble(prefs.getString("etch_beta", "3880"));

		if (prefs.getBoolean("awake",false)==true) {	// Stay awake?
			tvBTStatus.setKeepScreenOn(true); 
		} else {
			tvBTStatus.setKeepScreenOn(false); 
		}

		if (!bExpTimerOn) {setExpTimerMax();}
		if (!bDevelopTimerOn) {setDevelopTimerMax();}
		if (!bEtchTimerOn) {setEtchTimerMax();}

		if (!onchange) {												// Only change connection mode at startup
			iIOBoardConnectionMode=Integer.valueOf(prefs.getString("conn_mode", "1"));
			BTAddress=prefs.getString("bt_address", "20:14:05:20:12:11");
			IPAddress=prefs.getString("wifi_address", "192.168.178.25");
			IPPort=Integer.valueOf(prefs.getString("wifi_port", "200"));
			USBHostPort=Integer.valueOf(prefs.getString("usbhost_port", "0"));
		}

	}  // get_options_settings


} // MainActivity

