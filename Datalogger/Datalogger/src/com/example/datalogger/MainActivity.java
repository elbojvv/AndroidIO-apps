// AndroidPlot:
//   Download jar from: http://androidplot.com/download/ and place in libs/
//   Source based on: http://androidplot.com/docs/how-to-pan-zoom-and-scale/
//               and: https://groups.google.com/forum/#!topic/androidplot/jBKW2lYa37g
package com.example.datalogger;

//use ctrl-shift-o to automatically get imports
//Note: android.R means an error in one of the xml filesimport java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.FloatMath;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

public class MainActivity extends Activity implements OnTouchListener { //onTouchListener for AndroidPlot zoom


	// ************************************************************************************************
	// ************************* Variables and object Declarations ************************************
	// ************************************************************************************************

	// Objects for graphical elements
	TextView tvBTStatus; 	// Declares TextView object; shows Bleutooth/WiFi/USB connection status
	TextView tvLogStatus;   // Declares TextView object; shows status of the datalogger
	TextView tvDebug; 		// Declares TextView object; shows debug information
	Button btBTConnect; 	// Declares Button object; try to connect
	Button btLogStart; 		// Declares Button object; starts datalogging
	Button btLogRestart; 	// Declares Button object; restarts/resumes datalogging
	Button btLogStop; 		// Declares Button object; stops datalogging
	Button btDownload; 		// Declares Button object; download all data from datalogger
	Button btUpdate; 		// Declares Button object; only download update data from datalogger
	Button btHeader; 		// Declares Button object; only download header data
	Button btRaw; 			// Declares Button object; download the raw data from the datalogger memory (do not decode)
	Button btReadPins; 		// Declares Button object; get the pin settings from the datalogger
	Button btLoadPins; 		// Declares Button object; read the pin settings from the save config on Android device
	Button btSavePins; 		// Declares Button object; save the pin settings to the config on Android device
	CheckBox cbSave;		// Declares Checkbox object; whether data is saved to file 
	RelativeLayout rlPinSettings; // Relative layout for pin settings
	CheckBox cbShowPins;	// Declares Checkbox object; checks whether pin settings are shown
	CheckBox cbSaveEEprom;	// Declares Checkbox object; checks whether the pin settings are save to eeprom on the datalogger
	CheckBox cbAutoStart;	// Declares Checkbox object; checks whether the datalogger should restart after power outage
	CheckBox cbLoop;		// Declares Checkbox object; checks whether the datalogger shuld record in a loop
	Spinner spMeasurements; // Declares Spinner object; for number of measurements
	Spinner spPeriod; 		// Declares Spinner object; for period between 'measurements' 
	Spinner spSumAvg; 		// Declares Spinner object; for sum, avarage, or 1 bit
	TextView tvMeasurements; // Declares TextView object; to clarify (in front of) spMeasurements 
	TextView tvPeriod; 		// Declares TextView object; to clarify (in front of) spPeriod 
	TextView tvSumAvg; 		// Declares TextView object; to clarify (in front of) spSumAvg
	CheckBox cbMax;			// Declares Checkbox object; for selecting max value
	CheckBox cbMin;			// Declares Checkbox object; for selecting min value
	CheckBox cbSD;			// Declares Checkbox object; for selecting standard deviation
	CheckBox cbFlashLed;	// Declares Checkbox object; for selecting flashing LED
	ScrollView svScrollView; // Scrollview for the whole GUI
	XYPlot plot; 			// AndroidPlot
	TextView[] tvArrPinName = new TextView[24];		// Array with Textview for pin names
	Spinner[] spArrFunction = new Spinner[24];		// Array with Spinner for pin functions
	TextView[] tvArrPP2Descr = new TextView[24];	// Array with Textview for description of status-2 register
	EditText[] etArrPP2Value = new EditText[24];	// Array with Edittext for status-2 register
	int[] iAllowedPinFunction = new int[24];		// Array for allowed functions per pin
	boolean[] bPP2Vis = new boolean[24];			// Array for need of status-2 register
	int[][] iPP0Value = new int[24][20];			// Array with status-0 register 
	int[][] iPP1Value = new int[24][20];			// Array with status-1 register
	String[][] sPP2Text = new String[24][20];		// Array with text for spinners

	boolean bThreadRun=true;	// flag to end the 'eternal' background loop

	// Database for user settings
	SharedPreferences prefs;
	//OnSharedPreferenceChangeListener spListener;

	// Handler to communicate between objects
	Handler hndMessage;		// The handler
	final int RECIEVE_MESSAGE_BTSTATUS = 1;			// Constant for Handler: Bluetooth status
	final int RECIEVE_MESSAGE_BTDATA = 2;			// Constant for Handler: Received Bluetooth data
	final int RECIEVE_MESSAGE_UPDATE_GRAPH = 3;		// Constant for Handler: Make graph
	final int RECIEVE_MESSAGE_UPDATE_GUI = 4;		// Constant for Handler: Update GUI
	final int RECIEVE_MESSAGE_UPDATE_RAW = 5;		// Constant for Handler: Save raw data from flash
	final int RECIEVE_MESSAGE_UPDATE_SAVE = 6;		// Constant for Handler: Save data
	final int RECIEVE_MESSAGE_SLOW_WARNING = 7;		// Constant for Handler: Give warning for slow scroll/zoom
	final int RECIEVE_MESSAGE_TEST = 8;				// Constant for Handler: Test
	final int RECIEVE_MESSAGE_DEBUG = 9;			// Constant for Handler: Send debug information

	// Handling the AndroidIO board
	IOBoardFunctions IOBoard = new IOBoardFunctions();	// Object for the communications with the AndroidIO board
	int iIOBoardConnectionMode = 1;
	private String BTAddress = "20:14:05:20:12:11";   	// MAC-address of Bluetooth module (is set in settings, but you can edit this line)
	private String IPAddress = "192.168.178.25"; 	  	// IP address (is set in settings, but you can edit this line)
	private int IPPort = 2000;							// IP port  (is set in settings, but you can edit this line)
	private int USBHostPort = 0;					  	// index of USB board (is set in settings, but you can edit this line)
	boolean onpausedelay=false;	// delays disconnect at onPause

	// Variables for analysing data from datalogger
	boolean bWriteChecked;
	final int writebuffermax = 200;
	String[] writebuffercomm = new String[writebuffermax];
	String[] writebuffercheck = new String[writebuffermax];
	String[] writebufferresponse = new String[writebuffermax];
	int[] writebufferdelay = new int[writebuffermax];
	int writebufferinpos=0;
	int writebufferoutpos=0;
	int writedelaycounter=0;
	int[] i2cBuffer = new int[65536];
	int c3_address, c3_base, c3_count;
	boolean bC3_read=true;
	boolean bC4_read=true;
	int iC3_ever_read=0; // bit 0 and 1 indicate whether the first 64 byte (two times 32) is ever read 
	int c4_block=0;
	int c4_status=0;
	int c4_posbyte=0;
	int c4_posbit=0;
	int c4_mcount=0;
	int c4_mscount=0;
	int last_cnt=0;
	boolean graph_refresh=false;
	double[] i2cData = new double[65536];
	int graph_max=0;
	int run_cnt;
	boolean bC3_download=false;
	boolean bC3_raw=false;
	int iC3_download_half;
	boolean bC3_graph=false;
	int iC4_readblock, iC4_readblockmax;
	int iC4_readblock_old=0;
	int iC4_loopcnt=0;
	int downloadcount, downloadmax;
	int[] iBufRead = new int[1026];
	int iBlocksToRead=0;
	int iBlocksToReadStart=0;
	int iBlocksToReadEnd=0;
	int iBlocksToReadMax=0;
	int iLastBlockRead=0;
	int iNumberOfMeasurements;
	boolean bWarningGiven = false;
	final int maxchannel=16;
	final int maxpoints=32768;
	final int maxlines=4;
	int[] bits_chan_sum = new int[maxchannel];	// Number of bits per channel
	int[] bits_chan_max = new int[maxchannel];
	int[] bits_chan_min = new int[maxchannel];
	int[] bits_chan_sd = new int[maxchannel];
	int[] div_chan_sum = new int[maxchannel];	// Divider (=max value of the channel)
	int[] div_chan_max = new int[maxchannel];
	int[] div_chan_min = new int[maxchannel];
	int[] div_chan_sd = new int[maxchannel];

	int chan_chn_max=0;
	int chan_idx_max=0;
	int[][] chan_sum = new int[maxpoints][maxchannel];
	int[][] chan_max = new int[maxpoints][maxchannel];
	int[][] chan_min = new int[maxpoints][maxchannel];
	int[][] chan_sd = new int[maxpoints][maxchannel];
	boolean[][] bchan_exist = new boolean[maxlines][maxchannel];

	int bits_per_measurement=0;

	int ipinread[] = new int[96];

	PrintWriter dataFile = null;

	// AndroidPlot (https://groups.google.com/forum/#!topic/androidplot/jBKW2lYa37g)
	private XYPlot xyAndroidPlot;
	final int SERIES_IDXMAX = 40;
	private SimpleXYSeries[] series = new SimpleXYSeries[SERIES_IDXMAX];

	private PointF minXY;
	private PointF maxXY;
	double scalex=1;
	double scaley=1;
	// Definition of the touch states
	static final int NONE = 0;
	static final int ONE_FINGER_DRAG = 1;
	static final int TWO_FINGERS_DRAG = 2;
	int mode = NONE;

	PointF firstFinger;
	float distBetweenFingersx;
	float distBetweenFingersy;
	boolean bPlotCreated=false;
	float lastScrolling;
	float distBetweenFingers;
	float lastZooming;

	boolean stopThread = false;


	// ************************************************************************************************
	// *************************************  Methods  ************************************************
	// ************************************************************************************************

	@SuppressLint("HandlerLeak") 	// Prevent warning at the message handler

	@Override
	public void onCreate(Bundle savedInstanceState){  // First step in the Android life cycle 
		LayoutParams lpTmp;

		super.onCreate(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);	// the usersettings database for this app

		setContentView(R.layout.activity_main);		// Create the layout based on the xml file src/layout/activity_main.xml

		// Fill the allowed pins functions arrays
		final int iINPUT=1;
		final int iINPUTWPU=2;
		final int iADC=8;
		final int iCPS=16;
		final int iCOUNTER=64;
		iAllowedPinFunction[0]=iINPUT+iADC+iCPS; 	// A0
		iAllowedPinFunction[1]=iINPUT+iADC+iCPS; 	// A1
		iAllowedPinFunction[2]=iINPUT+iADC+iCPS; 	// A2
		iAllowedPinFunction[3]=iINPUT+iADC+iCPS; 	// A3
		iAllowedPinFunction[4]=iINPUT+iADC+iCPS; 	// A4
		iAllowedPinFunction[5]=iINPUT+iADC+iCPS; 	// A5
		iAllowedPinFunction[6]=iINPUT+iADC; 		// A6
		iAllowedPinFunction[7]=iINPUT+iADC;		 	// A7
		iAllowedPinFunction[8]=iINPUT+iINPUTWPU+iADC+iCPS; 		// B0
		iAllowedPinFunction[9]=iINPUT+iINPUTWPU+iADC+iCPS; 		// B1
		iAllowedPinFunction[10]=iINPUT+iINPUTWPU+iADC+iCPS; 	// B2
		iAllowedPinFunction[11]=iINPUT+iINPUTWPU+iADC+iCPS; 	// B3
		iAllowedPinFunction[12]=iINPUT+iINPUTWPU+iADC+iCPS; 	// B4
		iAllowedPinFunction[13]=iINPUT+iINPUTWPU+iADC+iCPS; 	// B5
		iAllowedPinFunction[14]=iINPUT+iINPUTWPU;	 			// B6
		iAllowedPinFunction[15]=iINPUT+iINPUTWPU; 				// B7
		iAllowedPinFunction[16]=iINPUT+iCOUNTER; 		// C0
		iAllowedPinFunction[17]=iINPUT; 				// C1
		iAllowedPinFunction[18]=iINPUT; 				// C2
		iAllowedPinFunction[19]=iINPUT; 				// C3
		iAllowedPinFunction[20]=iINPUT; 				// C4
		iAllowedPinFunction[21]=iINPUT; 				// C5
		iAllowedPinFunction[22]=0; 						// C6
		iAllowedPinFunction[23]=0;		 				// C7

		// Create and assign the GUI objects 
		btBTConnect = (Button) findViewById(R.id.idBTConnect); 
		tvBTStatus=(TextView)findViewById(R.id.idBTStatus); 
		tvLogStatus=(TextView)findViewById(R.id.idLogStatus); 
		tvDebug=(TextView)findViewById(R.id.idDebugtext); 
		btLogStart = (Button) findViewById(R.id.idLogStart); 
		btLogRestart = (Button) findViewById(R.id.idLogRestart); 
		btLogStop = (Button) findViewById(R.id.idLogStop); 
		btDownload = (Button) findViewById(R.id.idDownload); 
		btUpdate = (Button) findViewById(R.id.idUpdate); 
		btHeader = (Button) findViewById(R.id.idHeader); 
		btRaw = (Button) findViewById(R.id.idRaw); 
		btReadPins = (Button) findViewById(R.id.idReadPins); 
		btLoadPins = (Button) findViewById(R.id.idLoadPins); 
		btSavePins = (Button) findViewById(R.id.idSavePins); 

		cbSave = (CheckBox) findViewById(R.id.idSave);
		rlPinSettings = (RelativeLayout) findViewById(R.id.idPinSettings);
		cbShowPins = (CheckBox) findViewById(R.id.idShowPins);
		cbSaveEEprom = (CheckBox) findViewById(R.id.idSaveEEprom);
		cbAutoStart = (CheckBox) findViewById(R.id.idAutoStart);
		cbLoop = (CheckBox) findViewById(R.id.idLoop);
		spMeasurements = (Spinner) findViewById(R.id.idMeasurements);
		spMeasurements.setSelection(0);
		spPeriod = (Spinner) findViewById(R.id.idPeriod);
		spPeriod.setSelection(0);
		spSumAvg = (Spinner) findViewById(R.id.idSumAvg);
		spSumAvg.setSelection(2);
		tvMeasurements = (TextView)findViewById(R.id.idtvMeasurements );
		tvPeriod = (TextView)findViewById(R.id.idtvPeriod);
		tvSumAvg = (TextView)findViewById(R.id.idtvSumAvg);
		cbMax = (CheckBox) findViewById(R.id.idMax);
		cbMin = (CheckBox) findViewById(R.id.idMin);
		cbSD = (CheckBox) findViewById(R.id.idSD);
		cbFlashLed = (CheckBox) findViewById(R.id.idFlashLed);
		svScrollView = (ScrollView) findViewById(R.id.scrollview);
		xyAndroidPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
		xyAndroidPlot.setOnTouchListener(this);

		// Make the GUI for the pin settings
		for (int i=0; i<22; i++) {
			tvArrPinName[i] = new TextView(this);
			lpTmp=new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			if (i>0) {
				lpTmp.addRule(RelativeLayout.BELOW, i+1000-1);
				lpTmp.addRule(RelativeLayout.ALIGN_LEFT, i+1000-1);
			} else {
				lpTmp.addRule(RelativeLayout.BELOW, R.id.idShowPins);
				lpTmp.addRule(RelativeLayout.ALIGN_LEFT, R.id.idShowPins);
			}
			lpTmp.setMargins(0, 20, 0, 0); // Margin to top: 10dp (left, top, right, bottom)
			tvArrPinName[i].setLayoutParams(lpTmp);
			tvArrPinName[i].setText(pin_to_name(i));
			//tvArr[i].setTextSize(8);
			tvArrPinName[i].setId(i+1000);
			rlPinSettings.addView(tvArrPinName[i]);

			spArrFunction[i] = new Spinner(this);

			lpTmp=new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lpTmp.addRule(RelativeLayout.RIGHT_OF, i+1000);
			lpTmp.addRule(RelativeLayout.ALIGN_BOTTOM, i+1000);
			spArrFunction[i].setLayoutParams(lpTmp);
			spArrFunction[i].setId(i+1100);
			spArrFunction[i].setPrompt("Mode");
			rlPinSettings.addView(spArrFunction[i]);

			tvArrPP2Descr[i]=new TextView(this);
			lpTmp=new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lpTmp.addRule(RelativeLayout.RIGHT_OF, i+1100);
			lpTmp.addRule(RelativeLayout.ALIGN_BOTTOM, i+1000);
			tvArrPP2Descr[i].setLayoutParams(lpTmp);
			tvArrPP2Descr[i].setText("# meas.");
			tvArrPP2Descr[i].setId(i+1200);
			rlPinSettings.addView(tvArrPP2Descr[i]);

			etArrPP2Value[i]=new EditText(this);
			lpTmp=new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lpTmp.addRule(RelativeLayout.RIGHT_OF, i+1200);
			lpTmp.addRule(RelativeLayout.ALIGN_BOTTOM, i+1000);
			etArrPP2Value[i].setLayoutParams(lpTmp);
			etArrPP2Value[i].setText("0");
			etArrPP2Value[i].setId(i+1300);
			rlPinSettings.addView(etArrPP2Value[i]);

			// Fill the spinners with the allowed pin functions, and set the status register 0, 1 and 2 constants
			List<String> list = new ArrayList<String>();
			int poscnt=0;
			list.add("Not assigned"); sPP2Text[i][poscnt]=" "; iPP1Value[i][poscnt]=0; bPP2Vis[i]=false; poscnt++;
			if ((iAllowedPinFunction[i] & iINPUT) !=0) {list.add("Input, 1 bit"); sPP2Text[i][poscnt]=" "; iPP0Value[i][poscnt]=0; iPP1Value[i][poscnt]=0+128; bPP2Vis[i]=false; poscnt++;}
			if ((iAllowedPinFunction[i] & iINPUT) !=0) {list.add("Input, latch"); sPP2Text[i][poscnt]=" "; iPP0Value[i][poscnt]=0; iPP1Value[i][poscnt]=16+128; bPP2Vis[i]=false; poscnt++;}
			if ((iAllowedPinFunction[i] & iINPUTWPU) !=0) {list.add("Input WPU, 1 bit"); sPP2Text[i][poscnt]=" "; iPP0Value[i][poscnt]=1; iPP1Value[i][poscnt]=0+128; bPP2Vis[i]=false; poscnt++; }
			if ((iAllowedPinFunction[i] & iINPUTWPU) !=0) {list.add("Input WPU, latch"); sPP2Text[i][poscnt]=" "; iPP0Value[i][poscnt]=1; iPP1Value[i][poscnt]=16+128; bPP2Vis[i]=false; poscnt++; }
			if ((iAllowedPinFunction[i] & iADC) !=0) {list.add("ADC, Vdd, avg"); sPP2Text[i][poscnt]="#meas."; iPP0Value[i][poscnt]=3; iPP1Value[i][poscnt]=0+128; bPP2Vis[i]=true; poscnt++; }
			if ((iAllowedPinFunction[i] & iADC) !=0) {list.add("ADC, Vdd, sum"); sPP2Text[i][poscnt]="#meas."; iPP0Value[i][poscnt]=3; iPP1Value[i][poscnt]=2+128; bPP2Vis[i]=true; poscnt++; }
			if ((iAllowedPinFunction[i] & iADC) !=0) {list.add("ADC, 2V, avg"); sPP2Text[i][poscnt]="#meas."; iPP0Value[i][poscnt]=3; iPP1Value[i][poscnt]=1+128; bPP2Vis[i]=true; poscnt++; }
			if ((iAllowedPinFunction[i] & iADC) !=0) {list.add("ADC, 2V, sum"); sPP2Text[i][poscnt]="#meas."; iPP0Value[i][poscnt]=3; iPP1Value[i][poscnt]=3+128; bPP2Vis[i]=true; poscnt++; }
			if ((iAllowedPinFunction[i] & iCPS) !=0) {list.add("CPS"); sPP2Text[i][poscnt]="threshold"; iPP0Value[i][poscnt]=4; iPP1Value[i][poscnt]=128; bPP2Vis[i]=true; poscnt++; }
			if ((iAllowedPinFunction[i] & iCOUNTER) !=0) {list.add("Counter"); sPP2Text[i][poscnt]=" "; iPP0Value[i][poscnt]=6; iPP1Value[i][poscnt]=0+128; bPP2Vis[i]=false; poscnt++; }
			if ((iAllowedPinFunction[i] & iCOUNTER) !=0) {list.add("Frequency"); sPP2Text[i][poscnt]=" "; iPP0Value[i][poscnt]=6; iPP1Value[i][poscnt]=3+128; bPP2Vis[i]=false; poscnt++; }
			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>
			(this, android.R.layout.simple_spinner_item,list);

			dataAdapter.setDropDownViewResource
			(android.R.layout.simple_spinner_dropdown_item);

			spArrFunction[i].setAdapter(dataAdapter);
			spArrFunction[i].setSelection(0);
			setSpinnerListeners(i);

			tvArrPP2Descr[i].setVisibility(View.GONE);
			//etArrPP2Value[i].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
			etArrPP2Value[i].setVisibility(View.GONE);

		}
		// see EditText focus problem: http://stackoverflow.com/questions/8100831/stop-scrollview-from-setting-focus-on-edittext
		svScrollView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
		svScrollView.setFocusable(true);
		svScrollView.setFocusableInTouchMode(true);
		svScrollView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				v.requestFocusFromTouch();
				return false;
			}
		});
		showPinFunctions(false);	// start with pin settings not visible

		// set for Android Plot zoom
		minXY = new PointF(0,0);
		maxXY = new PointF(1,1000);

		for (int i=0;i<1026;i++) {iBufRead[i]=-1;}	// initiate that nothing from buffer read

		hndMessage = new Handler() {							// Initialise the handler to send messages between objects
			public void handleMessage(android.os.Message msgMessage) { // Android calls this with the message. msgMessage is received object
				String sTekstMessage, txt;
				int timetonext;
				sTekstMessage=(String) msgMessage.obj;		// Convert the text to string sTekstMessage
				switch (msgMessage.what) {
				case RECIEVE_MESSAGE_BTSTATUS:					// message is Bluetooth status update
					tvBTStatus.setText(sTekstMessage);			// Put text in tvBTStatus TextView
					break;
				case RECIEVE_MESSAGE_UPDATE_GRAPH:					// Update the graph
					Timer t = new Timer();
					t.schedule(new TimerTask() {
						@Override
						public void run() {
							hndMessage.obtainMessage(RECIEVE_MESSAGE_DEBUG, 0, 0, "Converting data").sendToTarget();
							convert_buffer_to_values();
							hndMessage.obtainMessage(RECIEVE_MESSAGE_DEBUG, 0, 0, "Plotting data").sendToTarget();
							make_AndroidPlot();
							hndMessage.obtainMessage(RECIEVE_MESSAGE_UPDATE_SAVE, 0, 0, " ").sendToTarget();
							hndMessage.obtainMessage(RECIEVE_MESSAGE_DEBUG, 0, 0, " ").sendToTarget();
						}
					}, 0);
					break;
				case RECIEVE_MESSAGE_UPDATE_GUI:					// Update GUI
					refresh_visibility();
					break;
				case RECIEVE_MESSAGE_UPDATE_RAW:					// Raw data is received
					save_raw();
					hndMessage.obtainMessage(RECIEVE_MESSAGE_DEBUG, 0, 0, " ").sendToTarget();
					break;
				case RECIEVE_MESSAGE_UPDATE_SAVE:					// Ready to save data
					if (cbSave.isChecked()) {save_values();}
					break;
				case RECIEVE_MESSAGE_SLOW_WARNING:					// Give warning that zooming Android Plot can be slow
					Toast.makeText(getBaseContext(), "With this many point, scolling and zooming the plot can be slow. Be patience.", Toast.LENGTH_LONG).show();
					break;
				case RECIEVE_MESSAGE_TEST:
					// put test case here
					break;
				case RECIEVE_MESSAGE_DEBUG:							// message is debug info
					tvDebug.setText(sTekstMessage);
					break;
				case RECIEVE_MESSAGE_BTDATA:						// message is data from Bluetooth/WiFi/USB module
					sTekstMessage=sTekstMessage.replace("\r","").replace("\n",""); // verwijder de einde lijn tekens
					Log.d(TAG," Recv: " +sTekstMessage);
					if (sTekstMessage.length()>3) {				// A valid response contains at least 3 characters 
						//Log.d(TAG, "Read: " + sTekstMessage);
						if (sTekstMessage.substring(0, 3).equals("G A")) {load_pins_from_string(sTekstMessage);}
						if (sTekstMessage.substring(0, 3).equals("G B")) {load_pins_from_string(sTekstMessage);}
						if (sTekstMessage.substring(0, 3).equals("G C")) {load_pins_from_string(sTekstMessage);}
						if (sTekstMessage.substring(0, 3).equals("G S")) {load_pins_from_string(sTekstMessage);}
						if (sTekstMessage.substring(0, 4).equals("R C3")) { // data from datalogger flash memory
							if (sTekstMessage.length()>=73) { // datalogger memory readout should have at least 73 charactres
								c3_address=Integer.parseInt(sTekstMessage.substring(5, 9), 16);
								if (c3_address<32768) {
									iBufRead[c3_address/32]=1;
								} else {
									iBufRead[((c3_address/32)%2)+1024]=1;
								}
								c3_address=(c3_address & 32767);
								for(int c3_count = 0; c3_count < 32; c3_count++) {
									i2cBuffer[c3_address+c3_count]=Integer.parseInt(sTekstMessage.substring(10+c3_count*2, 12+c3_count*2), 16);
								}
								bC3_read=true;
								if ((c3_address & 32767) == 0) iC3_ever_read=iC3_ever_read | 1; 
								if ((c3_address & 32767) == 32) iC3_ever_read=iC3_ever_read | 2; 
							} else { // C3 not in logging mode, responses is probably "R C3 0"
								// Set the AndroidIO into reading the flash memory
								write_wait("S C3 6"+(char)10, 10);
							}
						} // C3 
						if (sTekstMessage.substring(0, 4).equals("R C4")) { // datalogger status
							if (sTekstMessage.length()>=30) { // datalogger status should have at least 30 characters
								try {c4_status=Integer.parseInt(sTekstMessage.substring(5, 7), 16);} catch (NumberFormatException e) {c4_status=0;}
								try {c4_block=Integer.parseInt(sTekstMessage.substring(8, 12), 16);} catch (NumberFormatException e) {c4_block	=0;}
								try {c4_posbyte=Integer.parseInt(sTekstMessage.substring(13, 15), 16);} catch (NumberFormatException e) {c4_posbyte=0;}
								try {c4_posbit=Integer.parseInt(sTekstMessage.substring(16, 17), 16);} catch (NumberFormatException e) {c4_posbit=0;}
								try {c4_mcount=Integer.parseInt(sTekstMessage.substring(18, 22), 16);} catch (NumberFormatException e) {c4_mcount=0;}
								try {c4_mscount=Integer.parseInt(sTekstMessage.substring(23, 31), 16);} catch (NumberFormatException e) {c4_mscount=0;}
								bC4_read=true;
								//Log.d(TAG,"c4_status "+c4_status+" c4_block "+c4_block);
								if ((c4_status & 1) == 1) {txt="Logging on";} else {txt="Logging off";}
								if (iC3_ever_read==3) {	// First 64 byte of buffer read?
									int bpm=i2cBuffer[5]*256+i2cBuffer[4];
									if (bpm<1) bpm=1;
									int mscountmax=((i2cBuffer[13]*256+i2cBuffer[12])*256+i2cBuffer[11])*256+i2cBuffer[10];
									if ((c4_status & 16)==0) { // no loop?
										iNumberOfMeasurements=0;
										if (c4_block>1) iNumberOfMeasurements=(c4_block-1)*(int)(504/bpm);
										iNumberOfMeasurements+=(c4_posbyte*8+c4_posbit-8)/bpm;
									} else {
										iNumberOfMeasurements=(int)(504/bpm)*510+(c4_posbyte*8+c4_posbit-8)/bpm;
									}
									try {
										timetonext=(int)(((long)c4_mcount*(long)mscountmax+(long)c4_mscount)/1000);
									} catch (ArithmeticException e1) {
										timetonext=0;
									}
									txt+="\n# of measurements: "+Integer.toString(iNumberOfMeasurements);
									if ((c4_status & 1) == 1) {txt+="\nSeconds to next write: "+Integer.toString(timetonext); }
									if ((c4_status & 16)==0) { // no loop?
										float memused=100f*(float)(c4_block*512+c4_posbyte*8+c4_posbit)/262144f;
										txt+="\nI2C memory used: "+String.format("%.2f",memused)+"%";
									} else {
										txt+="\nAll I2C memory used, now looping";
									}
								} else {
									if ((c4_status & 16)==0) {
										float memused=100f*(float)(c4_block*512+c4_posbyte*8+c4_posbit)/262144f;
										txt+="\nI2C memory used: "+String.format("%.2f",memused)+"%";
									} else {
										txt+="\nAll I2C memory used, now looping";
									}
									if ((c4_status & 1) == 1) {
										txt+="\nMeas. to next save: "+Integer.toString(c4_mcount);
										txt+="\nms to next meas.: "+Integer.toString(c4_mscount);
									}
									txt+="\nDownload header to get more info";
								}
							} else { // C4 not in logging mode, respones is probably "R C4 1"
								c4_status=0;
								c4_block=-1;
								c4_posbyte=-1;
								c4_posbit=-1;
								c4_mcount=-1;
								c4_mscount=-1;
								bC4_read=true;
								txt="Logging off";
							}
							tvLogStatus.setText(txt);
						} // C4
						if (sTekstMessage.substring(0, 4).equals("G S0")) { // datalog status
							if (Integer.parseInt(sTekstMessage.substring(5, 6),10)==1) {
							} else {
							}
						} // S0
					}
					if (prefs.getBoolean("debug",false)==true) {	// if usersettings in debugmode
						if(sTekstMessage.length()>22) {
							tvDebug.setText("D: "+sTekstMessage.substring(0, 22));// display response (only first 22 chracters) of the AndroidIO board to the status TextView
						} else {
							tvDebug.setText("D: "+sTekstMessage);// display response of the AndroidIO board to the status TextView
						}
					}
					break;
				} // switch
			}; // handleMessage
		}; // hndMessage

		iIOBoardConnectionMode=Integer.valueOf(prefs.getString("conn_mode", "0"));
		if (iIOBoardConnectionMode==0) {	// Bluetooth
			BTAddress=prefs.getString("bt_address", "20:14:05:20:12:11");
			IOBoard.Initiate(0, hndMessage, BTAddress, 0, iIOBoardConnectionMode,this);	// Initiate the Bluetooth connection
		}
		if (iIOBoardConnectionMode==1) {	// WiFi
			IPAddress=prefs.getString("wifi_address", "192.168.178.25");
			IPPort=Integer.valueOf(prefs.getString("wifi_port", "200"));
			//Log.d(TAG,"IP "+IPAddress+" "+IPPort);
			IOBoard.Initiate(0, hndMessage, IPAddress, IPPort, iIOBoardConnectionMode, this);	// Initiate the WiFi connection
		}
		if (iIOBoardConnectionMode==2) {	// USB Accessory
			IOBoard.Initiate(0, hndMessage, " ", 0, iIOBoardConnectionMode, this);	// Initiate the USB Accessory connection
		}
		if (iIOBoardConnectionMode==3) {	// USB Host
			USBHostPort=Integer.valueOf(prefs.getString("usbhost_port", "0"));
			IOBoard.Initiate(0, hndMessage, " ", USBHostPort, iIOBoardConnectionMode, this);	// Initiate the USB Host connection
		}

		btBTConnect.setOnClickListener(new OnClickListener() { // Android calls this when button idConnect is pressed
			public void onClick(View v) {
				IOBoard.connect();
			}
		}); // btBTConnect.setOnClickListener

		btLogStart.setOnClickListener(new OnClickListener() { // Android calls this when button start logging is pressed
			public void onClick(View v) {
				writePinFunctions();
				write_wait("s s3 1"+(char)10, 10);
			}
		}); //btLogStart.setOnClickListener

		btLogRestart.setOnClickListener(new OnClickListener() { // Android calls this when button restart logging is pressed
			public void onClick(View v) {
				write_wait("s s3 2"+(char)10, 10);
			}
		}); //btLogRestart.setOnClickListener

		btLogStop.setOnClickListener(new OnClickListener() { // Android calls this when button stop logging is pressed
			public void onClick(View v) {
				write_wait("s s3 0"+(char)10, 10);
			}
		}); //btLogStop.setOnClickListener

		btDownload.setOnClickListener(new OnClickListener() { // Android calls this when button download is pressed
			public void onClick(View v) {
				if ((c4_status & 16) == 0) {	// no loop in data
					iBlocksToReadMax=c4_block*2+2;
					iLastBlockRead=c4_block*2;
					iBufRead[1024]=0;
					iBufRead[1025]=0;
				} else {						// data looped
					iBlocksToReadMax=1026;
					iLastBlockRead=c4_block*2;
				}
				for (int i=0; i<iBlocksToReadMax; i++) {iBufRead[i]=0;}
				iBlocksToRead=0;
				last_cnt=-1;
				bC3_read=false;
				bC3_download=true;
				bC3_graph=true;
			}
		}); // btDownload.setOnClickListener

		btUpdate.setOnClickListener(new OnClickListener() { // // Android calls this when button download update is pressed
			public void onClick(View v) {
				if (((c4_status & 16) == 0) || (c4_block*2>=iLastBlockRead)){	// no loop in data
					iBlocksToReadStart=iLastBlockRead;
					iBlocksToReadEnd=c4_block*2;
					iLastBlockRead=c4_block*2;
					iBlocksToReadMax=iBlocksToReadEnd-iBlocksToReadStart+2;
					for (int i=iBlocksToReadStart; i<iBlocksToReadEnd; i++) {iBufRead[i]=0;}
					iBufRead[1024]=0;
					iBufRead[1025]=0;
				} else {						// data looped
					iBlocksToReadMax=0;
					for (int i=iLastBlockRead; i<1026; i++) {
						iBufRead[i]=0;
						iBlocksToReadMax++;
					}
					for (int i=0; i<(c4_block*2+2); i++) {
						iBufRead[i]=0;
						iBlocksToReadMax++;
					}
					iLastBlockRead=c4_block*2;
				}
				iBlocksToRead=0;
				last_cnt=-1;
				bC3_read=false;
				bC3_download=true;
				bC3_graph=true;
			}
		}); //btUpdate.setOnClickListener

		btHeader.setOnClickListener(new OnClickListener() { // Android calls this when button download header is pressed
			public void onClick(View v) {
				iBufRead[0]=0;
				iBufRead[1]=0;
				iBlocksToRead=0;
				iBlocksToReadMax=2;
				bC3_read=false;
				bC3_download=true;
			}
		}); // btHeader.setOnClickListener

		btRaw.setOnClickListener(new OnClickListener() { // Android calls this when button raw download is pressed
			public void onClick(View v) {
				iBlocksToReadMax=1026;
				for (int i=0; i<iBlocksToReadMax; i++) {iBufRead[i]=0;}
				iBlocksToRead=0;
				bC3_read=false;
				bC3_download=true;
				bC3_raw=true;
			}
		}); //btRaw.setOnClickListener

		btReadPins.setOnClickListener(new OnClickListener() { // Android calls this when button read pin settings is pressed
			public void onClick(View v) {
				read_pins();
			}
		}); //btReadPins.setOnClickListener

		btLoadPins.setOnClickListener(new OnClickListener() { // Android calls this when button load pin settings is pressed
			public void onClick(View v) {
				load_pins_from_prefs();
			}
		}); //btLoadPins.setOnClickListener

		btSavePins.setOnClickListener(new OnClickListener() { // Android calls this when button to save the settings is pressed
			public void onClick(View v) {
				save_pins_to_prefs();
			}
		}); //btSavePins.setOnClickListener


		cbShowPins.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {  // Android calls this when checkbox to show pin settings is pressed
			@Override
			public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
				showPinFunctions(isChecked);
			}
		}); // cbShowPins.setOnCheckedChangeListener

		if (prefs.getBoolean("awake",false)==true) {	// read (once) the settings whether screen should always stay on, and set the GUI
			tvBTStatus.setKeepScreenOn(true); 
		} else {
			tvBTStatus.setKeepScreenOn(false); 
		}

		prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {  // Android calls this when user settings are changed
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {  // Android calls when user settings are changed
				if (key.equals("awake")) {
					if (prefs.getBoolean(key,false)==true) {
						tvBTStatus.setKeepScreenOn(true); 
					} else {
						tvBTStatus.setKeepScreenOn(false); 
					}
				}
				// warn that when connection is changed, the app should be restarted
				if (key.equals("conn_mode") || key.equals("bt_address") || key.equals("wifi_address") || key.equals("wifi_port") || key.equals("usbhost_port")) {
					Toast.makeText(getBaseContext(), "Restart app to take change in connection in effect.", Toast.LENGTH_LONG).show(); 
				}
			} // onSharedPreferenceChanged
		}); 

		// Make graph with AndroidPlot
		xyAndroidPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
		xyAndroidPlot.getGraphWidget().setTicksPerRangeLabel(2);
		xyAndroidPlot.getGraphWidget().setTicksPerDomainLabel(2);
		xyAndroidPlot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
		xyAndroidPlot.getGraphWidget().setRangeValueFormat(new DecimalFormat("#####"));
		xyAndroidPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("#####.#"));
		xyAndroidPlot.getGraphWidget().setRangeLabelWidth(25);
		xyAndroidPlot.setRangeLabel("");
		xyAndroidPlot.setDomainLabel("");

		xyAndroidPlot.setBorderStyle(Plot.BorderStyle.NONE, null, null);

		new Thread(new Runnable() {  // Make a thread
			public void run(){		 // and define the interface
				int loopcnt=0;
				int bufpos;
				while (bThreadRun) {

					// The command buffer; Are there any commands to write? Decrease the wait time. Write.
					bufpos=peek_pos_writebuffer();
					if (bufpos>-1) {
						if (writebufferdelay[bufpos]>10) {
							writebufferdelay[bufpos]=writebufferdelay[bufpos]-10;
						} else {
							bufpos=pop_pos_writebuffer();
							My_send_command(writebuffercomm[bufpos]);
						}
					}

					// every second: read C4
					if (loopcnt%100==0) {
						bC4_read=false;
						My_send_command("r c4"+(char)10);
					}

					// Check whether to read C3
					if (bC3_download) {
						bC3_read=false;
						if (loopcnt%12==0) {	// Do read C3 at 120ms
							last_cnt=((last_cnt+1)%1026);
							while ((last_cnt<2052) && (iBufRead[(last_cnt % 1026)]!=0)) {
								last_cnt++;
							}
							if (last_cnt<2052) { 	// still a block to read
								My_send_command("r c3 "+String.valueOf((last_cnt % 1026)*32)+(char)10);
								iBlocksToRead++;
								if (iBlocksToRead>iBlocksToReadMax) {iBlocksToReadMax=iBlocksToRead;} // Can occur if blocks have to be reread caused by slow or bad communication
								hndMessage.obtainMessage(RECIEVE_MESSAGE_DEBUG, 0, 0, String.format("Download %d of %d", iBlocksToRead, iBlocksToReadMax)).sendToTarget();
							} else {				// finished reading
								hndMessage.obtainMessage(RECIEVE_MESSAGE_DEBUG, 0, 0, " ").sendToTarget();
								bC3_download=false;
								bC3_read=true;
							}
						}
					}

					// Check whether data readout is ready
					if ((!bC3_download) && (bC3_read) && (bC3_graph)) {
						hndMessage.obtainMessage(RECIEVE_MESSAGE_UPDATE_GRAPH, 0, 0, " ").sendToTarget(); // Data ready, make graph and save (if needed)
						bC3_graph=false;
					}
					if ((!bC3_download) && (bC3_read) && (bC3_raw)) {
						hndMessage.obtainMessage(RECIEVE_MESSAGE_UPDATE_RAW, 0, 0, " ").sendToTarget(); // Data ready, make graph and save (if needed)
						bC3_graph=false;
					}
					try {Thread.sleep(10);} catch (InterruptedException e) {}// Sleep 10 ms
					loopcnt++;
				} // while
			}
		}).start(); // Thread

	}  // END OF onCreate - Everything up to here, only runs at start of the app

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
	public boolean onCreateOptionsMenu(Menu menu) {		// Generate the code for menu
		getMenuInflater().inflate(R.menu.main, menu);	// from the xml file src/menu/menu.xml
		return true;
	} // onCreateOptionsMenu

	private static final String TAG = "Datalogger";

	public boolean onOptionsItemSelected(MenuItem item) {	// Android calls this when clicked on menu option
		switch (item.getItemId()) {
		case R.id.action_settings:	// start de user setting fragment
			Intent i = new Intent(MainActivity.this, UserSettingActivity.class); // Don't forget to modify the AndroidManifest to allow this!
			startActivity(i);
			return true;
		case R.id.exit:
			bThreadRun=false;	// Stops running thread(s)
			finish();		// exit the app; Normally not needed, but sometimes solves connection problems
			return true;
		default:
			return super.onOptionsItemSelected(item);
		} 
	} // onOptionsItemSelected

	// get a specific bit from buffer i2cBuffer
	private int byte2bit(int idx, int bitnr) {
		return ((i2cBuffer[idx] & (1<<(7-bitnr)))==0) ? 0 : 1;
	}

	// send a command to the AndroidIO board
	void My_send_command(String cmm) {
		//Log.d(TAG," Send: " +cmm);
		IOBoard.send_command(cmm);
	}

	// extracts a value by specifying the bit range within i2cBuffer
	private int byte2data(int block, int bit_start, int bit_length) {
		int result=0;
		for(int bit_cnt=bit_start; bit_cnt<bit_start+bit_length; bit_cnt++) {
			result=result*2+byte2bit(block*64+(int)(bit_cnt/8),bit_cnt % 8);
		}
		return result;
	}

	// Convert the whole read buffer to an array of values
	public void convert_buffer_to_values() {
		int chan_counter, pos_block, pos_blockc, pos_bit, pos_block_start, pos_block_end;

		// First analyse the header for channels and extra data
		bits_per_measurement=(int)i2cBuffer[5]*256+(int)i2cBuffer[4];

		chan_chn_max=0;
		for (chan_counter=0; chan_counter<15; chan_counter++) {
			bits_chan_sum[chan_counter]=0;
			bits_chan_max[chan_counter]=0;
			bits_chan_min[chan_counter]=0;
			bits_chan_sd[chan_counter]=0;
			div_chan_sum[chan_counter]=1;
			div_chan_max[chan_counter]=1;
			div_chan_min[chan_counter]=1;
			div_chan_sd[chan_counter]=1;
			if (i2cBuffer[chan_counter*2+16]!=255) {
				//Log.d(TAG,"chan_counter: "+chan_counter);
				if ((i2cBuffer[2]&3)==1) { // 01: sum
					bits_chan_sum[chan_chn_max]=(int)i2cBuffer[chan_counter*2+17]+(int)i2cBuffer[3];
					div_chan_sum[chan_counter]=((1<<i2cBuffer[chan_counter*2+17])-1)*(1<<i2cBuffer[3]);
				}
				if ((i2cBuffer[2]&3)==2) { // 10: average
					bits_chan_sum[chan_chn_max]=i2cBuffer[chan_counter*2+17];
					div_chan_sum[chan_counter]=(1<<i2cBuffer[chan_counter*2+17])-1;
				}
				if ((i2cBuffer[2]&3)==3) { // 11: sum (1 bit) or average (>1bit)
					if (i2cBuffer[chan_counter*2+17]<2) {
						bits_chan_sum[chan_chn_max]=(int)i2cBuffer[chan_counter*2+17]+(int)i2cBuffer[3];
						div_chan_sum[chan_counter]=((1<<i2cBuffer[chan_counter*2+17])-1)*(1<<i2cBuffer[3]);
					} else {
						bits_chan_sum[chan_chn_max]=i2cBuffer[chan_counter*2+17];
						div_chan_sum[chan_counter]=(1<<i2cBuffer[chan_counter*2+17])-1;
					}
				}
				if ((i2cBuffer[2]&4)==4) { // max
					bits_chan_max[chan_chn_max]=i2cBuffer[chan_counter*2+17];
					div_chan_max[chan_counter]=(1<<i2cBuffer[chan_counter*2+17])-1;
				}
				if ((i2cBuffer[2]&8)==8) { // min
					bits_chan_min[chan_chn_max]=i2cBuffer[chan_counter*2+17];
					div_chan_min[chan_counter]=(1<<i2cBuffer[chan_counter*2+17])-1;
				}
				if ((i2cBuffer[2]&16)==16) { // sd
					bits_chan_sd[chan_chn_max]=i2cBuffer[chan_counter*2+17];
					div_chan_sd[chan_counter]=(1<<i2cBuffer[chan_counter*2+17])-1;
				}
				chan_chn_max++;
			} 
		} // for i1

		// find start and end block, also looking at looped data
		pos_block_start=1;
		pos_block_end=c4_block+1;

		if ((i2cBuffer[0]&32)!=0) {  // Loop indicator set
			for (pos_block=c4_block+1; pos_block<512; pos_block++) {
				if ( ((i2cBuffer[pos_block*64] & 128)/128) != ((i2cBuffer[pos_block*64-64] & 64)/64) ) {
					pos_block_start=pos_block;
				}
			}
			for (pos_block=1; pos_block<c4_block; pos_block++) {
				if ( ((i2cBuffer[pos_block*64] & 128)/128) != ((i2cBuffer[pos_block*64-64] & 64)/64) ) {
					pos_block_start=pos_block;
				}
			}
			pos_block_end=pos_block_start+512;
		}

		//Log.d(TAG,"pos_block_start: "+pos_block_start);
		//Log.d(TAG,"pos_block_end: "+pos_block_end);

		// Now get data from the blocks
		chan_idx_max=0;

		pos_blockc=pos_block_start; 
		while (pos_blockc!=pos_block_end) {
			pos_block=pos_blockc % 512;
			pos_bit=8; // start
			while ( ((pos_block!=c4_block) && (pos_bit+bits_per_measurement<512)) || ((pos_block==c4_block) && (pos_bit+bits_per_measurement<c4_posbyte*8+c4_posbit)) ) {
				for (chan_counter=0; chan_counter<chan_chn_max; chan_counter++) {
					chan_sum[chan_idx_max][chan_counter]=byte2data(pos_block,pos_bit,bits_chan_sum[chan_counter]);
					pos_bit+=bits_chan_sum[chan_counter];
					chan_max[chan_idx_max][chan_counter]=byte2data(pos_block,pos_bit,bits_chan_max[chan_counter]);
					pos_bit+=bits_chan_max[chan_counter];
					chan_min[chan_idx_max][chan_counter]=byte2data(pos_block,pos_bit,bits_chan_min[chan_counter]);
					pos_bit+=bits_chan_min[chan_counter];
					chan_sd[chan_idx_max][chan_counter]=byte2data(pos_block,pos_bit,bits_chan_sd[chan_counter]);
					pos_bit+=bits_chan_sd[chan_counter];
				}
				chan_idx_max++;
			}
			pos_blockc++;
			if (pos_blockc==512) {pos_blockc=513;}
		}
		// Create for graph doubles in range 0..1
		for (chan_counter=0; chan_counter<chan_chn_max; chan_counter++) {
			bchan_exist[0][chan_counter]=false;
			bchan_exist[1][chan_counter]=false;
			bchan_exist[2][chan_counter]=false;
			bchan_exist[3][chan_counter]=false;
			if (bits_chan_sum[chan_counter]>0) {
				bchan_exist[0][chan_counter]=true;
			}
			if (bits_chan_max[chan_counter]>0) {
				bchan_exist[1][chan_counter]=true;
			}
			if (bits_chan_min[chan_counter]>0) {
				bchan_exist[2][chan_counter]=true;
			}
			if (bits_chan_sd[chan_counter]>0) {
				bchan_exist[3][chan_counter]=true;
			}
		}
	}

	// save values to a file, filename made out of date. Also save info file.
	void save_values() {
		int point_cnt, line_cnt, chan_cnt;
		int file_cnt=0;
		String txt;
		String csv=";";

		csv=Character.toString((char)(Integer.valueOf(prefs.getString("csv", "59")) & 255));

		SimpleDateFormat df = new SimpleDateFormat("yyyyddMMHHmmss");
		Date today = Calendar.getInstance().getTime();        
		String reportDate = df.format(today);

		// First data file, later info file
		String fname1=Environment.getExternalStorageDirectory().getPath()+"/datalogger/data"+reportDate+".csv";
		try {
			dataFile = new PrintWriter(fname1, "UTF-8");
		} catch (FileNotFoundException e) {
			//TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			//TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (dataFile!=null) {
			//Log.d(TAG, "file created");
		} else {
			//Log.d(TAG, "file not created");
		}

		// First Header line
		txt="Index";
		for(line_cnt=0; line_cnt<chan_chn_max; line_cnt++) {
			if (bchan_exist[0][line_cnt]) {
				txt+=csv+pin_to_name(i2cBuffer[line_cnt*2+16])+" value";
			}
			if (bchan_exist[1][line_cnt]) {
				txt+=csv+pin_to_name(i2cBuffer[line_cnt*2+16])+" Max";
			}
			if (bchan_exist[2][line_cnt]) {
				txt+=csv+pin_to_name(i2cBuffer[line_cnt*2+16])+" Min";
			}
			if (bchan_exist[3][line_cnt]) {
				txt+=csv+pin_to_name(i2cBuffer[line_cnt*2+16])+" SD";
			}
		}
		if (dataFile!=null) dataFile.println(txt);

		// Actual data
		for (point_cnt=0;  point_cnt<chan_idx_max-1; point_cnt++) {	
			txt=Integer.toString(point_cnt);
			for(line_cnt=0; line_cnt<chan_chn_max; line_cnt++) {
				if (bchan_exist[0][line_cnt]) {
					txt+=csv+Integer.toString(chan_sum[point_cnt][line_cnt]);
				}
				if (bchan_exist[1][line_cnt]) {
					txt+=csv+Integer.toString(chan_max[point_cnt][line_cnt]);
				}
				if (bchan_exist[2][line_cnt]) {
					txt+=csv+Integer.toString(chan_min[point_cnt][line_cnt]);
				}
				if (bchan_exist[3][line_cnt]) {
					txt+=csv+Integer.toString(chan_sd[point_cnt][line_cnt]);
				}
			}
			if (dataFile!=null) dataFile.println(txt);
		}
		if (dataFile!=null) {dataFile.close(); file_cnt++;}

		// Now info

		String fname2=Environment.getExternalStorageDirectory().getPath()+"/datalogger/info"+reportDate+".txt";
		try {
			dataFile = new PrintWriter(fname2, "UTF-8");
		} catch (FileNotFoundException e) {
			//TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			//TODO Auto-generated catch block
			e.printStackTrace();
		}

		int mpw=(((int)i2cBuffer[5]*256+(int)i2cBuffer[6])+(int)i2cBuffer[7])+(int)i2cBuffer[8];
		int mpm=(((int)i2cBuffer[9]*256+(int)i2cBuffer[10])+(int)i2cBuffer[11])+(int)i2cBuffer[12];
		txt="S0: "+Integer.toString(i2cBuffer[1]); if (dataFile!=null) dataFile.println(txt);
		txt="S1: "+Integer.toString(i2cBuffer[2]); if (dataFile!=null) dataFile.println(txt);
		txt="Extra bits: "+Integer.toString(i2cBuffer[3]); if (dataFile!=null) dataFile.println(txt);
		txt="Bits per measurement: "+Integer.toString(i2cBuffer[4]); if (dataFile!=null) dataFile.println(txt);
		txt="Measurements per write: "+Integer.toString(mpw); if (dataFile!=null) dataFile.println(txt);
		txt="ms per measurement: "+Integer.toString(mpm); if (dataFile!=null) dataFile.println(txt);

		txt="Measured points: "+Integer.toString(chan_idx_max); if (dataFile!=null) dataFile.println(txt);

		// Pins
		for (chan_cnt=0;  chan_cnt<15; chan_cnt++) {
			if (i2cBuffer[chan_cnt*2+16]!=255) { // Channel exists
				txt="Channel: "+Integer.toString(chan_cnt)+", pin: "+pin_to_name(i2cBuffer[chan_cnt*2+16])+", bits: "+Integer.toString(i2cBuffer[chan_cnt*2+17]);
				if (dataFile!=null) dataFile.println(txt);
			}
		}
		if (dataFile!=null) {dataFile.close(); file_cnt++;}

		if (file_cnt==2) {
			Toast.makeText(getBaseContext(), "Files "+fname1+" "+fname2+" written.", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getBaseContext(), "File write error!", Toast.LENGTH_LONG).show();
		}
	}

	// save raw data to a file, filename made out of date. 
	void save_raw() {
		int address_cnt;
		String txt;

		SimpleDateFormat df = new SimpleDateFormat("yyyyddMMHHmmss");
		Date today = Calendar.getInstance().getTime();        
		String reportDate = df.format(today);

		String fname1=Environment.getExternalStorageDirectory().getPath()+"/RawData"+reportDate+".hex";
		try {
			dataFile = new PrintWriter(fname1, "UTF-8");
		} catch (FileNotFoundException e) {
			//TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			//TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (address_cnt=0; address_cnt<32768; address_cnt++) {
			txt=String.format("%02x", (0xFF & i2cBuffer[address_cnt])); 
			if (dataFile!=null) dataFile.println(txt);
		}
		if (dataFile!=null) {
			dataFile.close();}
		Toast.makeText(getBaseContext(), "File "+fname1+" written.", Toast.LENGTH_LONG).show();
	}

	// Make the grapgh with data, using AndroidPlot
	public void make_AndroidPlot() {
		int line_cnt_mod, pin_cnt;
		float xTmp1, yTmp1, xTmp2, yTmp2;
		int[][] iaColor = new int[4][16];
		boolean[] bLine = new boolean[SERIES_IDXMAX];
		boolean bDrawLine;
		String[] linename = {"Avg.", "Max.", "Min", "+SD", "-SD"};

		int point_cnt, line_cnt;

		//Log.d(TAG,"chan_chn_max: "+chan_chn_max);

		//If the graph is made the first time:
		if (!bPlotCreated) {
			for(line_cnt=0; line_cnt<5*chan_chn_max; line_cnt++) {
				line_cnt_mod=line_cnt%5;
				pin_cnt=line_cnt/5;
				if (line_cnt_mod<4) {
					if (bchan_exist[line_cnt_mod][line_cnt/5]) {
						series[line_cnt] = new SimpleXYSeries(pin_to_name(i2cBuffer[pin_cnt*2+16])+" "+linename[line_cnt_mod]);
					}
				}
				if (line_cnt_mod==4) {
					if (bchan_exist[0][line_cnt/5]) {
						if (bchan_exist[0][line_cnt/5]) { // if average exists: then SD + average
							series[line_cnt] = new SimpleXYSeries(pin_to_name(i2cBuffer[pin_cnt*2+16])+" "+linename[line_cnt_mod]);
						}
					}
				}
			}
			bPlotCreated=true;
		}

		// Give only one time warning if more then 1000 points in the graph, that zoom/scroll is slow
		if ((chan_idx_max*chan_chn_max>1000) & (!bWarningGiven)) { 
			hndMessage.obtainMessage(RECIEVE_MESSAGE_SLOW_WARNING, 0, 0, " ").sendToTarget();
			bWarningGiven=true;
		}

		// Clear the graph
		clearSeries();
		for(line_cnt=0; line_cnt<5*chan_chn_max; line_cnt++) {
			bLine[line_cnt]=false;
		}

		// draw in graph
		for (int i1=0; i1<16; i1++) {
			for (int i2=0; i2<4; i2++) {
				iaColor[i2][i1]=Color.parseColor("#202020"); // Gray
			}
		}
		iaColor[0][0]=Color.parseColor("#0000FF"); // Blue
		iaColor[1][0]=Color.parseColor("#000080"); // Med Blue
		iaColor[2][0]=Color.parseColor("#000080"); // Med Blue
		iaColor[3][0]=Color.parseColor("#A0A000"); // Yellow
		iaColor[0][1]=Color.parseColor("#FF0000"); // Red
		iaColor[1][1]=Color.parseColor("#800000"); // Dark red
		iaColor[2][1]=Color.parseColor("#800000"); // Dark red
		iaColor[3][1]=Color.parseColor("#808000"); // Med Yelow

		for (point_cnt=0;  point_cnt<chan_idx_max; point_cnt++) {	// teken de punten
			for(line_cnt=0; line_cnt<5*chan_chn_max; line_cnt++) {
				bDrawLine=false;
				line_cnt_mod=line_cnt%5;
				xTmp1=(float)point_cnt; 
				xTmp2=(float)(point_cnt+1); 
				yTmp1=0;
				yTmp2=0;
				if (line_cnt_mod<3) {
					if (bchan_exist[line_cnt_mod][line_cnt/5]) {
						yTmp1=(float)dchan_val(point_cnt,line_cnt%5,line_cnt/5);
						bDrawLine=true;
					}
				}
				if (line_cnt_mod==3) {
					if (bchan_exist[3][line_cnt/5]) {
						if (bchan_exist[0][line_cnt/5]) { // if avarage exists: then SD + avarage
							yTmp1=(float)(dchan_val(point_cnt,0,line_cnt/5)+dchan_val(point_cnt,3,line_cnt/5));
						} else { // otherwise sd at height
							yTmp1=(float)(dchan_val(point_cnt,3,line_cnt/5));
						}
						bDrawLine=true;
					}
				}
				if (line_cnt_mod==4) {
					if (bchan_exist[3][line_cnt/5]) {
						if (bchan_exist[0][line_cnt/5]) { // if avarage exists: then SD - avarage
							yTmp1=(float)(dchan_val(point_cnt,0,line_cnt/5)-dchan_val(point_cnt,3,line_cnt/5));
						} else { // otherwise sd at height
							yTmp1=(float)(dchan_val(point_cnt,3,line_cnt/5));
						}
						bDrawLine=true;
					}
					line_cnt_mod=3;
				}
				if (bDrawLine) {
					if (yTmp1<0) yTmp1=0;
					if (yTmp1>1) yTmp1=1;

					series[line_cnt].addLast(point_cnt,(int)(yTmp1*1000));
					//Log.d(TAG,"Point "+line_cnt+" "+point_cnt+" "+(int)(yTmp1*1000));
					bLine[line_cnt]=true;
				}
			}
		}

		for(line_cnt=0; line_cnt<5*chan_chn_max; line_cnt++) {
			//Log.d(TAG,"series: "+line_cnt+" "+bLine[line_cnt]);
			if (bLine[line_cnt]) {
				line_cnt_mod=line_cnt%5;
				if (line_cnt_mod>3) line_cnt_mod=3;

				xyAndroidPlot.addSeries(series[line_cnt],new LineAndPointFormatter(iaColor[line_cnt_mod][line_cnt/5], null,null, null));
				//Log.d(TAG,"Serie "+line_cnt+" "+line_cnt_mod);
			}
		}
		minXY = new PointF(0,0);
		maxXY = new PointF(chan_idx_max,1000);
		xyAndroidPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
		xyAndroidPlot.redraw();
	}

	// Routine to soom and scroll the graph
	@Override
	public boolean onTouch(View arg0, MotionEvent event) {
		//Log.d(TAG,"onTouch "+event.getAction());
		int iEvent = (event.getAction() & MotionEvent.ACTION_MASK);
		if (iEvent==MotionEvent.ACTION_DOWN) { // Start gesture
			firstFinger = new PointF(event.getX(), event.getY());
			mode = ONE_FINGER_DRAG;
			return true;
		}
		if (iEvent==MotionEvent.ACTION_UP) {
			arg0.performClick();
			return true;
		}
		if ((iEvent==MotionEvent.ACTION_POINTER_UP) || (iEvent==MotionEvent.ACTION_CANCEL)) {
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					while((Math.abs(lastScrolling)>0.001f) || (Math.abs(lastZooming-1)>0.001f)){ 
						lastScrolling*=.8;
						lastZooming+=(1-lastZooming)*.2;
						AndroidPlot_scrollzoom(lastScrolling, lastZooming);
						xyAndroidPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
						xyAndroidPlot.redraw();
						// the thread lives until the scrolling and zooming are imperceptible
						try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();
						}
					}
				}
			}, 0);
			return true;
		}
		if (iEvent==MotionEvent.ACTION_POINTER_DOWN) { // second finger
			if (event.getPointerCount()>1) {
				distBetweenFingers = spacing(event);
				if (distBetweenFingers > 5f) {
					mode = TWO_FINGERS_DRAG;
				}
			}
			return true;
		}
		if (iEvent==MotionEvent.ACTION_MOVE) {
			if (mode == ONE_FINGER_DRAG) {
				PointF oldFirstFinger=firstFinger;
				firstFinger=new PointF(event.getX(), event.getY());
				lastScrolling=oldFirstFinger.x-firstFinger.x;
				lastZooming=(firstFinger.x-oldFirstFinger.x)/xyAndroidPlot.getWidth();
				if (lastZooming<0) {
					lastZooming=1/(1-lastZooming);
				} else {
					lastZooming+=1;
				}
				AndroidPlot_scrollzoom(lastScrolling, 1);
				xyAndroidPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
				xyAndroidPlot.redraw();

			} else if (mode == TWO_FINGERS_DRAG) {
				float oldDist =distBetweenFingers; 
				if (event.getPointerCount()>1) {
					distBetweenFingers=spacing(event);
					lastZooming=oldDist/distBetweenFingers;
					xyAndroidPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
					xyAndroidPlot.redraw();
				}
			}
			return true;
		}
		return true;
	}

	private void AndroidPlot_scrollzoom(float pan, float scale) {
		float minXYtempx, maxXYtempx; 
		
		if (prefs.getBoolean("stretch",true)==true) { // Stretch beyond edge
			scroll(pan);
			if (minXY.x<0) minXY.x=0;
			if (maxXY.x>chan_idx_max) maxXY.x=chan_idx_max;
			zoom(scale);
			if (minXY.x<0) minXY.x=0;
			if (maxXY.x>chan_idx_max) maxXY.x=chan_idx_max;
		} else {
			minXYtempx=minXY.x;
			maxXYtempx=maxXY.x;
			scroll(pan);
			if ((minXY.x<0) || (maxXY.x>chan_idx_max)) {
				minXY.x=minXYtempx;		// role backif too far
				maxXY.x=maxXYtempx;
			} else {
				zoom(scale);
				if ((minXY.x<0) || (maxXY.x>chan_idx_max)) {
					minXY.x=minXYtempx;		// role back if too far
					maxXY.x=maxXYtempx;
				}				
			}
		}

	}

	private void scroll(float pan) {
		float domainSpan = maxXY.x    - minXY.x;
		float step = domainSpan / xyAndroidPlot.getWidth();
		float offset = pan * step;
		minXY.x+= offset;
		maxXY.x+= offset;
	}

	private void zoom(float scale) {
		float domainSpan = maxXY.x    - minXY.x;
		float domainMidPoint = maxXY.x        - domainSpan / 2.0f;
		float offset = domainSpan * scale / 2.0f;
		minXY.x=domainMidPoint-offset;
		maxXY.x=domainMidPoint+offset;
	}


	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	private void clearSeries() {
		for(int i = 0; i < SERIES_IDXMAX; i++) {
			if (series[i]!=null) {
				while (series[i].size()>0) {
					series[i].removeLast();
				}
			}

		}
	}

	// read the setting of all the pins
	void read_pins() {
		hndMessage.obtainMessage(RECIEVE_MESSAGE_DEBUG, 0, 0, "Download settings").sendToTarget();
		for (int i=0; i<96; i++) {ipinread[i]=-1;}

		write_wait("g s"+(char)10, 20);
		write_wait("g s1"+(char)10, 20);
		for (int i=0; i<22; i++) {
			write_wait("g "+pin_to_name(i)+(char)10, 20);
			write_wait("g "+pin_to_name(i)+"1"+(char)10, 20);
			write_wait("g "+pin_to_name(i)+"2"+(char)10, 20);
		}
	}

	// save the pin settings to the preferences
	void save_pins_to_prefs() {
		SharedPreferences.Editor prefsEditor = prefs.edit();
		String key;
		int itempos;

		prefsEditor.putBoolean("cbSaveEEprom", cbSaveEEprom.isChecked());
		prefsEditor.putBoolean("cbAutoStart", cbAutoStart.isChecked());
		prefsEditor.putBoolean("cbLoop", cbLoop.isChecked());
		prefsEditor.putBoolean("cbMax", cbMax.isChecked());
		prefsEditor.putBoolean("cbMin", cbMin.isChecked());
		prefsEditor.putBoolean("cbSD", cbSD.isChecked());
		prefsEditor.putBoolean("cbFlashLed", cbFlashLed.isChecked());

		itempos=spMeasurements.getSelectedItemPosition(); if (itempos == Spinner.INVALID_POSITION) {itempos=0;}
		prefsEditor.putInt("spMeasurements", itempos);
		itempos=spPeriod.getSelectedItemPosition(); if (itempos == Spinner.INVALID_POSITION) {itempos=0;}
		prefsEditor.putInt("spPeriod", itempos);
		itempos=spSumAvg.getSelectedItemPosition(); if (itempos == Spinner.INVALID_POSITION) {itempos=0;}
		prefsEditor.putInt("spSumAvg", itempos);

		for (int i=0; i<22; i++) {
			itempos=spArrFunction[i].getSelectedItemPosition(); if (itempos == Spinner.INVALID_POSITION) {itempos=0;}
			key=String.format(Locale.US, "pin%da%d", i,0);
			prefsEditor.putInt(key, itempos);
			key=String.format(Locale.US, "pin%da%d", i,2);
			prefsEditor.putString(key, etArrPP2Value[i].getText().toString());
		}
		prefsEditor.apply();
	}

	// get the pin settings from the preferences
	void load_pins_from_prefs() {
		String key,val2;
		int itempos;

		cbSaveEEprom.setChecked(prefs.getBoolean("cbSaveEEprom", false));
		cbAutoStart.setChecked(prefs.getBoolean("cbAutoStart", false));
		cbLoop.setChecked(prefs.getBoolean("cbLoop", false));
		cbMax.setChecked(prefs.getBoolean("cbMax", false));
		cbMin.setChecked(prefs.getBoolean("cbMin", false));
		cbSD.setChecked(prefs.getBoolean("cbSD", false));
		cbFlashLed.setChecked(prefs.getBoolean("cbFlashLed", false));

		spMeasurements.setSelection(prefs.getInt("spMeasurements", 0));
		spPeriod.setSelection(prefs.getInt("spPeriod", 0));
		spSumAvg.setSelection(prefs.getInt("spSumAvg", 0));

		for (int i=0; i<22; i++) {
			key=String.format(Locale.US, "pin%da%d", i,0);
			itempos=prefs.getInt(key, 0);
			spArrFunction[i].setSelection(itempos);
			key=String.format(Locale.US, "pin%da%d", i,2);
			val2=prefs.getString(key, "0");
			etArrPP2Value[i].setText(val2);
		}
	}

	// Analyse the response from the AndroidIO board about the settings, and change the GUI
	void load_pins_from_string(String sReply) {
		int pin, pinb, pin4, val;
		boolean bDone;
		char chr;

		bDone=false;
		hndMessage.obtainMessage(RECIEVE_MESSAGE_DEBUG, 0, 0, "Download setting "+sReply.substring(2,4)).sendToTarget();
		if (sReply.substring(0, 4).equals("G C3")) {
			bDone=true;
		}
		if (sReply.substring(0, 4).equals("G C4")) {
			bDone=true;
		}

		if (sReply.substring(0, 4).equals("G S0")) {
			bDone=true;
			if (sReply.length()>3) {
				val=Integer.parseInt(sReply.substring(5).trim());
				if ((val & 2)==0) {cbLoop.setChecked(false);} else {cbLoop.setChecked(true);}
				if ((val & 4)==0) {cbAutoStart.setChecked(false);} else {cbAutoStart.setChecked(true);}
				spMeasurements.setSelection( (val/8)%4 );
				spPeriod.setSelection( (val/32)%8 );
			}
		}

		if (sReply.substring(0, 4).equals("G S1")) {
			bDone=true;
			if (sReply.length()>3) {
				val=Integer.parseInt(sReply.substring(5).trim());
				spSumAvg.setSelection( (val)%4 );
				if ((val & 4)==0) {cbMax.setChecked(false);} else {cbMax.setChecked(true);}
				if ((val & 8)==0) {cbMin.setChecked(false);} else {cbMin.setChecked(true);}
				if ((val & 16)==0) {cbSD.setChecked(false);} else {cbSD.setChecked(true);}
				if ((val & 128)==0) {cbFlashLed.setChecked(false);} else {cbFlashLed.setChecked(true);}
			}

		}

		if (!bDone) {
			if (sReply.length()>3) {
				chr=sReply.charAt(2);
				pin=((int)(chr)-65)*32;
				chr=sReply.charAt(3);
				pin+=((int)(chr)-48)*4;
				chr=sReply.charAt(4);
				pin+=((int)(chr)-48);
				if (pin<96) {
					val=Integer.parseInt(sReply.substring(5).trim());
					ipinread[pin]=val;
					if ((pin%4)==1) {	// Now 0 and 1 read.
						pinb=pin & 124;
						pin4=pin/4;
						if ((ipinread[pinb]>0) && (ipinread[pinb+1]>0)) {
							for (int i=0; i<spArrFunction[pin4].getCount(); i++) {
								if ((iPP0Value[pin4][i]==ipinread[pinb]) && (iPP1Value[pin4][i]==ipinread[pinb+1])) {
									spArrFunction[pin4].setSelection(i);
								}
							}
						}
					}
					if ((pin%4)==2) {	// Now 2 read.
						pinb=pin & 124;
						pin4=pin/4;
						etArrPP2Value[pin4].setText(Integer.toString(ipinread[pinb+2]));
						if (pin4==21) {hndMessage.obtainMessage(RECIEVE_MESSAGE_DEBUG, 0, 0, " ").sendToTarget();}
					}
				}
			}
		}
	}

	// get the value of the data
	double dchan_val(int point, int line, int channel) {
		double result=0;
		if ((line==0) && (bits_chan_sum[channel]>0)) {
			result=(double)chan_sum[point][channel]/(double)div_chan_sum[channel];
		}
		if ((line==1) && (bits_chan_max[channel]>0)) {
			result=(double)chan_max[point][channel]/(double)div_chan_max[channel];
		}
		if ((line==2) && (bits_chan_min[channel]>0)) {
			result=(double)chan_min[point][channel]/(double)div_chan_min[channel];
		}
		if ((line==3) && (bits_chan_sd[channel]>0)) {
			result=(double)chan_sd[point][channel]/(double)div_chan_sd[channel];
		}
		return result;
	}

	// Give address for command write buffer for a peek
	int peek_pos_writebuffer() {
		// returns value for reading the writebuffer, or -1 if empty
		int result;
		if (writebufferinpos==writebufferoutpos) {
			result = -1;
		} else {
			result = writebufferoutpos;
		}
		return result;
	}

	// Give address for command write buffer for a push
	int pop_pos_writebuffer() {
		// returns value for reading the writebuffer, or -1 if empty
		int result;
		if (writebufferinpos==writebufferoutpos) {
			result = -1;
		} else {
			result = writebufferoutpos;
			writebufferoutpos++;
			if (writebufferoutpos>=writebuffermax) {writebufferoutpos=0;}
		}
		return result;
	}

	// Give address for command write buffer for a push
	int push_pos_writebuffer() {
		// returns value for writing the writebuffer, or -1 if full
		int result,tmp;
		tmp=writebufferinpos+1;
		if (tmp>=writebuffermax) {tmp=0;}
		if (tmp==writebufferoutpos) {
			result = -1;
		} else {
			result = writebufferinpos;
			writebufferinpos=tmp;
		}
		return result;
	}

	// push a command plus delay to the write buffer
	void write_wait(String command, int delayms) {
		int pos=push_pos_writebuffer();
		if (pos>-1) {
			writebuffercomm[pos] = command;
			writebufferdelay[pos] = delayms;
		}
	}

	// Generate the pin name, from the pin number (0 -> A0, etc)
	String pin_to_name(int pinnumber) {
		return Character.toString((char)(65+pinnumber/8))+Character.toString((char)(48+pinnumber%8));
	}

	// Write all the pin settings to the AndroidIO board (through write buffer)
	void writePinFunctions() {
		bWriteChecked=true;
		for (int i=0; i<22; i++) {
			int itempos=spArrFunction[i].getSelectedItemPosition();
			if (itempos != Spinner.INVALID_POSITION) {
				write_wait("S "+pin_to_name(i)+" "+Integer.toString(iPP0Value[i][itempos])+(char)10, 10);
				write_wait("S "+pin_to_name(i)+"1 "+Integer.toString(iPP1Value[i][itempos])+(char)10, 10);
				if (bPP2Vis[i]) {
					write_wait("S "+pin_to_name(i)+"2 "+etArrPP2Value[i].getText().toString()+(char)10, 10);
				}
			}
		} // for i=0..21
		int s0=0;
		if (cbLoop.isChecked()) s0+=2;						// bit 1
		if (cbAutoStart.isChecked()) s0+=4; 				// bit 2
		s0+=spMeasurements.getSelectedItemPosition()*8; 	// bits 3:4
		s0+=spPeriod.getSelectedItemPosition()*32; 			// bits 5:7
		write_wait("s s "+Integer.toString(s0)+(char)10,10);
		int s1=0;
		s1+=spSumAvg.getSelectedItemPosition(); 			// bits 0:1
		if (cbMax.isChecked()) s1+=4;						// bit 2
		if (cbMin.isChecked()) s1+=8;						// bit 3
		if (cbSD.isChecked()) s1+=16;						// bit 4
		if (cbFlashLed.isChecked()) s1+=128;				// bit 7
		write_wait("s s1 "+Integer.toString(s1)+(char)10,10);
		if (cbSaveEEprom.isChecked()) {write_wait("s z3 2"+(char)10,10);}
	}

	// Show or hide the pin settings in the GUI
	void showPinFunctions(boolean show) {
		if (show) {
			for (int i=0; i<22; i++) {
				tvArrPinName[i].setVisibility(View.VISIBLE);
				spArrFunction[i].setVisibility(View.VISIBLE);
				if (bPP2Vis[i]) {
					tvArrPP2Descr[i].setVisibility(View.VISIBLE);
					etArrPP2Value[i].setVisibility(View.VISIBLE);
				}
			}
			cbSaveEEprom.setVisibility(View.VISIBLE);
			cbAutoStart.setVisibility(View.VISIBLE);
			cbLoop.setVisibility(View.VISIBLE);
			spMeasurements.setVisibility(View.VISIBLE);
			spPeriod.setVisibility(View.VISIBLE);
			spSumAvg.setVisibility(View.VISIBLE);
			cbMax.setVisibility(View.VISIBLE);
			cbMin.setVisibility(View.VISIBLE);
			cbSD.setVisibility(View.VISIBLE);
			cbFlashLed.setVisibility(View.VISIBLE);
			tvMeasurements.setVisibility(View.VISIBLE);
			tvPeriod.setVisibility(View.VISIBLE);
			tvSumAvg.setVisibility(View.VISIBLE);

		} else {
			for (int i=0; i<22; i++) {
				tvArrPinName[i].setVisibility(View.GONE);
				spArrFunction[i].setVisibility(View.GONE);
				tvArrPP2Descr[i].setVisibility(View.GONE);
				etArrPP2Value[i].setVisibility(View.GONE);
			}
			cbSaveEEprom.setVisibility(View.GONE);
			cbAutoStart.setVisibility(View.GONE);
			cbLoop.setVisibility(View.GONE);
			spMeasurements.setVisibility(View.GONE);
			spPeriod.setVisibility(View.GONE);
			spSumAvg.setVisibility(View.GONE);
			cbMax.setVisibility(View.GONE);
			cbMin.setVisibility(View.GONE);
			cbSD.setVisibility(View.GONE);
			cbFlashLed.setVisibility(View.GONE);
			tvMeasurements.setVisibility(View.GONE);
			tvPeriod.setVisibility(View.GONE);
			tvSumAvg.setVisibility(View.GONE);
		}
	}

	// Update the GUI if showing/hiding the pin settings is changed
	void refresh_visibility() {
		for (int i=0; i<22; i++) {
			if (bPP2Vis[i]) {
				tvArrPP2Descr[i].setVisibility(View.VISIBLE);
				//etArrPP2Value[i].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
				etArrPP2Value[i].setVisibility(View.VISIBLE);
			} else {
				tvArrPP2Descr[i].setVisibility(View.GONE);
				//etArrPP2Value[i].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
				etArrPP2Value[i].setVisibility(View.GONE);
			}
		}

	}

	
	// Set all the spinnerlisteners in the GUI for the pin settings
	void setSpinnerListeners(int i) {
		// stupid, but only way...
		// generated with script:
		/*
 		#!/bin/bash
		for i in {0..21}
		do
		echo "		if (i==$i) {"
		echo "			spArrFunction[$i].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {"
		echo "				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {"
		echo "					// Object item = parent.getItemAtPosition(pos);"
		echo "					// Make EditText for PP2 visible or not, depending on Item selected"
		echo "					if (bPP2Vis[pos]) { // make PP2 visible"
		echo "						tvArrPP2Descr[$i].setVisibility(View.VISIBLE);"
		echo "						tvArrPP2Descr[$i].setText(sPP2Text[$i][pos]);"
		echo "						etArrPP2Value[$i].setVisibility(View.VISIBLE);"
		echo "					} else {"
		echo "						tvArrPP2Descr[$i].setVisibility(View.GONE);"
		echo "						etArrPP2Value[$i].setVisibility(View.GONE);"
		echo "					}"
		echo "				}"
		echo "				public void onNothingSelected(AdapterView<?> parent) {"
		echo "				}"
		echo "			});"
		echo "		}"
		done
		 */
		if (i==0) {
			spArrFunction[0].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[0].setVisibility(View.VISIBLE);
						tvArrPP2Descr[0].setText(sPP2Text[0][pos]);
						etArrPP2Value[0].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[0].setVisibility(View.GONE);
						etArrPP2Value[0].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==1) {
			spArrFunction[1].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[1].setVisibility(View.VISIBLE);
						tvArrPP2Descr[1].setText(sPP2Text[1][pos]);
						etArrPP2Value[1].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[1].setVisibility(View.GONE);
						etArrPP2Value[1].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==2) {
			spArrFunction[2].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[2].setVisibility(View.VISIBLE);
						tvArrPP2Descr[2].setText(sPP2Text[2][pos]);
						etArrPP2Value[2].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[2].setVisibility(View.GONE);
						etArrPP2Value[2].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==3) {
			spArrFunction[3].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[3].setVisibility(View.VISIBLE);
						tvArrPP2Descr[3].setText(sPP2Text[3][pos]);
						etArrPP2Value[3].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[3].setVisibility(View.GONE);
						etArrPP2Value[3].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==4) {
			spArrFunction[4].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[4].setVisibility(View.VISIBLE);
						tvArrPP2Descr[4].setText(sPP2Text[4][pos]);
						etArrPP2Value[4].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[4].setVisibility(View.GONE);
						etArrPP2Value[4].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==5) {
			spArrFunction[5].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[5].setVisibility(View.VISIBLE);
						tvArrPP2Descr[5].setText(sPP2Text[5][pos]);
						etArrPP2Value[5].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[5].setVisibility(View.GONE);
						etArrPP2Value[5].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==6) {
			spArrFunction[6].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[6].setVisibility(View.VISIBLE);
						tvArrPP2Descr[6].setText(sPP2Text[6][pos]);
						etArrPP2Value[6].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[6].setVisibility(View.GONE);
						etArrPP2Value[6].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==7) {
			spArrFunction[7].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[7].setVisibility(View.VISIBLE);
						tvArrPP2Descr[7].setText(sPP2Text[7][pos]);
						etArrPP2Value[7].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[7].setVisibility(View.GONE);
						etArrPP2Value[7].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==8) {
			spArrFunction[8].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[8].setVisibility(View.VISIBLE);
						tvArrPP2Descr[8].setText(sPP2Text[8][pos]);
						etArrPP2Value[8].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[8].setVisibility(View.GONE);
						etArrPP2Value[8].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==9) {
			spArrFunction[9].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[9].setVisibility(View.VISIBLE);
						tvArrPP2Descr[9].setText(sPP2Text[9][pos]);
						etArrPP2Value[9].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[9].setVisibility(View.GONE);
						etArrPP2Value[9].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==10) {
			spArrFunction[10].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[10].setVisibility(View.VISIBLE);
						tvArrPP2Descr[10].setText(sPP2Text[10][pos]);
						etArrPP2Value[10].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[10].setVisibility(View.GONE);
						etArrPP2Value[10].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==11) {
			spArrFunction[11].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[11].setVisibility(View.VISIBLE);
						tvArrPP2Descr[11].setText(sPP2Text[11][pos]);
						etArrPP2Value[11].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[11].setVisibility(View.GONE);
						etArrPP2Value[11].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==12) {
			spArrFunction[12].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[12].setVisibility(View.VISIBLE);
						tvArrPP2Descr[12].setText(sPP2Text[12][pos]);
						etArrPP2Value[12].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[12].setVisibility(View.GONE);
						etArrPP2Value[12].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==13) {
			spArrFunction[13].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[13].setVisibility(View.VISIBLE);
						tvArrPP2Descr[13].setText(sPP2Text[13][pos]);
						etArrPP2Value[13].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[13].setVisibility(View.GONE);
						etArrPP2Value[13].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==14) {
			spArrFunction[14].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[14].setVisibility(View.VISIBLE);
						tvArrPP2Descr[14].setText(sPP2Text[14][pos]);
						etArrPP2Value[14].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[14].setVisibility(View.GONE);
						etArrPP2Value[14].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==15) {
			spArrFunction[15].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[15].setVisibility(View.VISIBLE);
						tvArrPP2Descr[15].setText(sPP2Text[15][pos]);
						etArrPP2Value[15].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[15].setVisibility(View.GONE);
						etArrPP2Value[15].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==16) {
			spArrFunction[16].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[16].setVisibility(View.VISIBLE);
						tvArrPP2Descr[16].setText(sPP2Text[16][pos]);
						etArrPP2Value[16].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[16].setVisibility(View.GONE);
						etArrPP2Value[16].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==17) {
			spArrFunction[17].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[17].setVisibility(View.VISIBLE);
						tvArrPP2Descr[17].setText(sPP2Text[17][pos]);
						etArrPP2Value[17].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[17].setVisibility(View.GONE);
						etArrPP2Value[17].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==18) {
			spArrFunction[18].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[18].setVisibility(View.VISIBLE);
						tvArrPP2Descr[18].setText(sPP2Text[18][pos]);
						etArrPP2Value[18].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[18].setVisibility(View.GONE);
						etArrPP2Value[18].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==19) {
			spArrFunction[19].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[19].setVisibility(View.VISIBLE);
						tvArrPP2Descr[19].setText(sPP2Text[19][pos]);
						etArrPP2Value[19].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[19].setVisibility(View.GONE);
						etArrPP2Value[19].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==20) {
			spArrFunction[20].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[20].setVisibility(View.VISIBLE);
						tvArrPP2Descr[20].setText(sPP2Text[20][pos]);
						etArrPP2Value[20].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[20].setVisibility(View.GONE);
						etArrPP2Value[20].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
		if (i==21) {
			spArrFunction[21].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					// Object item = parent.getItemAtPosition(pos);
					// Make EditText for PP2 visible or not, depending on Item selected
					if (bPP2Vis[pos]) { // make PP2 visible
						tvArrPP2Descr[21].setVisibility(View.VISIBLE);
						tvArrPP2Descr[21].setText(sPP2Text[21][pos]);
						etArrPP2Value[21].setVisibility(View.VISIBLE);
					} else {
						tvArrPP2Descr[21].setVisibility(View.GONE);
						etArrPP2Value[21].setVisibility(View.GONE);
					}
				}
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}
	}


} // MainActivity

