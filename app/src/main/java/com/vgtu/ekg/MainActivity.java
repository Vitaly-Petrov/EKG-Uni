package com.vgtu.ekg;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.neurosky.thinkgear.HeartRateAcceleration;
import com.neurosky.thinkgear.NeuroSkyHeartMeters;
import com.neurosky.thinkgear.TGDevice;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.view.KeyEvent;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ImageView;

import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Set;

import static android.content.ContentValues.TAG;


public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    TGDevice tgDevice;

    private ActionBar bar;
    private View mview;

    HeartRateAcceleration  heartRateAcceleration;
    int smoothedHeartRate;



    private ImageButton btnMinimize;
    private ImageButton btnBluetooth;
    private ImageButton btnImageUsers;
    private ImageButton btnSettings;
    private ImageButton btnRecord;
    private ImageButton btnStopRecording;
    private TextView heart_Rate;
    public ImageView mImageView;
    public ImageView mImageBT;
    public AnimationDrawable mAnimationDrawable;


    private TextView recordTime;

    private int time = 0;

    public int bt_connected = 1;


    int subjectContactQuality_last;
    int subjectContactQuality_cnt;


    TextView tv_HeartRate,tv_HeartAge,tv_RespirationRate,tv_RelaxationLevel,tv_5minHeartAge,tv_rrInterval;
    TextView tv_Title;
    EditText et_age;
    int heartRate; byte poorSignal;
    public int average_heartrate = 0;
    int len = 0;
    int tem_heartrate_difference = 0;
    int tem_sum = 0;//sum of heart rate difference
    int value = 0;//new point
    int tem_value = 0;
    private GraphicalView chart;
    private LinearLayout linear;
    private XYSeries hseries;


    //draw section
    XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    XYSeriesRenderer dxyrenderer = new XYSeriesRenderer(),hxyrenderer;
    //make data store
    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    private int addX = -1, addY;
    int[] xv = new int[50];
    int[] yv = new int[50];



    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InitActionBar();



        subjectContactQuality_last = -1; /* start with impossible value */
        subjectContactQuality_cnt = 200; /* start over the limit, so it gets reported the 1st time */






        //setup the draw section
        renderer.setPointSize(3);
        renderer.setZoomButtonsVisible(true);
        //renderer.setShowGrid(show_grid);
        renderer.setXAxisMax(50);
        renderer.setXAxisMin(0);
        renderer.setYAxisMax(100);
        renderer.setYAxisMin(0);
        renderer.setXLabels(10);
        renderer.setYLabels(10);
        renderer.setAxesColor(Color.WHITE);




        //set up heart rate
        hxyrenderer = new XYSeriesRenderer();
        //hxyrenderer.setColor(Color.BLUE);
        hxyrenderer.setPointStyle(PointStyle.DIAMOND);
        renderer.addSeriesRenderer(hxyrenderer);
        hseries = new XYSeries("R-R интервал");
        dataset.addSeries(hseries);
        //setup the draw in screen
        linear = (LinearLayout)findViewById(R.id.linear1);
        chart = ChartFactory.getLineChartView(MainActivity.this, dataset, renderer);
        linear.addView(chart,new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));



    }



    private void InitActionBar() {

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.custom_action_bar);
        mview = getSupportActionBar().getCustomView();
        bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#131313")));

        btnMinimize = (ImageButton)mview.findViewById(R.id.action_bar_minimize);
        heart_Rate =(TextView)mview.findViewById(R.id.heartRate);
        btnMinimize.setColorFilter(Color.parseColor("#c6c5c5"));
        btnMinimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mImageView=(ImageView)mview.findViewById(R.id.action_bar_heartRateIcon);
        mImageView.setBackgroundResource(R.drawable.heart_anim);
        mAnimationDrawable = (AnimationDrawable)mImageView.getBackground();

        mImageBT=mview.findViewById(R.id.action_bar_bluetooth);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btnBluetooth = mview.findViewById(R.id.action_bar_bluetooth);
        btnBluetooth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                switch (bt_connected) {
                    case 1:
                        tgDevice = new TGDevice(bluetoothAdapter, handler);
                        tgDevice.connect(true);
                        bt_connected =2;
                        mImageBT.setImageResource(R.drawable.ic_connect_bt);
                        break;
                    case 2:
                        tgDevice.close();
                        bt_connected =10;
                        bt_connected=1;
                        mImageBT.setImageResource(R.drawable.ic_disconnect_bt);
                        break;


                }

            }
        });

        btnImageUsers = (ImageButton)mview.findViewById(R.id.action_bar_ids);
        btnImageUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(MainActivity.this, newUserActivity.class));
            }
        });



        btnSettings = (ImageButton)mview.findViewById(R.id.action_bar_settings);
        btnSettings.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });

        btnRecord = (ImageButton)mview.findViewById(R.id.action_bar_play);
        btnRecord.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(!isRecording)
                    startRecording();
                else
                    stopRecording();
            }
        });

        btnStopRecording=(ImageButton)mview.findViewById(R.id.action_bar_stop);
        btnStopRecording.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                    stopRecording();
            }
        });

        recordTime = (TextView)mview.findViewById(R.id.recordTime);

    }

    private boolean isRecording = false;
    private void startRecording(){
       isRecording = true;
        Thread timer = new Thread(){
            public void run(){
                Long startTime = System.currentTimeMillis();
                Long endTime;
                Long delta;
                //String seconds, minutes, milliseconds;
                while(isRecording){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    endTime = System.currentTimeMillis();
                    delta = endTime - startTime;
                    final String seconds = String.valueOf(new Long(delta / 1000 % 60).intValue());
                    final String minutes = String.valueOf(new Long(delta / 1000 / 60).intValue());
                    final String milliseconds = String.valueOf(new Long(delta % 1000).intValue());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            recordTime.setText(minutes + ":" + seconds + "." + String.format("%2s",milliseconds));//.substring(0, milliseconds.length() - 1));
                        }
                    });
                }

            }
        };
        timer.start();
        Thread fileWriterThread = new Thread(){
            public void run(){
                String path = Environment.getExternalStorageDirectory() + File.separator + "ECGGraphData";
                File folder = new File(path);
                folder.mkdirs();
                File file = new File(folder, DateFormat.getDateTimeInstance().format(new Date()) + ".txt");
                Date now = new Date();
                try{
                    file.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(file);
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);
                    outputStreamWriter.write("Record start date - " + DateFormat.getDateInstance().format(now) + "\n");
                    outputStreamWriter.write("Record start time - " + DateFormat.getTimeInstance().format(now) + "\n");
                    outputStreamWriter.write("Record from device - " + "Sichiray MAC here" + "\n");
                    outputStreamWriter.write("Record to device - " + "Device MAC here" + "\n\n");

                    outputStreamWriter.write("Raw data - \n\n");
                    while (isRecording){
                        outputStreamWriter.write(String.valueOf(heartRate) + "\n");
                        Thread.sleep(20);
                    }
                    outputStreamWriter.close();
                    fOut.close();
                } catch (IOException e){
                    Log.e(TAG, e.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        fileWriterThread.start();
    }


    private void stopRecording(){
        isRecording = false;
        Toast.makeText(getApplicationContext(),"Запись завершена!",Toast.LENGTH_LONG).show();
    }


    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // читаем цвет фона из ListPreference для цвета фона
        int color;
        String regular = prefs.getString(getString(R.string.pref_style), "");
        if (regular.contains("Черный")) {
            color = Color.BLACK;
            renderer.setBackgroundColor(color);

        } else if (regular.contains("Белый")) {
            color = Color.WHITE;
            renderer.setBackgroundColor(color);

        }
        renderer.setApplyBackgroundColor(true);

        // читаем цвет фона из ListPreference для цвета кривой графика
        int color_curve;
        String regular_curve = prefs.getString(getString(R.string.pref_mesh), "");
        if (regular_curve.contains("Синий")) {
            color_curve = Color.BLUE;
            hxyrenderer.setColor(color_curve);

        } else if (regular_curve.contains("Зеленый")) {
            color_curve = Color.GREEN;
            hxyrenderer.setColor(color_curve);

        } else if (regular_curve.contains("Красный")) {
            color_curve = Color.RED;
            hxyrenderer.setColor(color_curve);

        }

        // читаем установленное значение из CheckBoxPreference
        boolean show_grid;
        if (prefs.getBoolean(getString(R.string.pref_curve), true))
        {
            show_grid=true;
            renderer.setShowGrid(show_grid);
        }
        else {show_grid=false;
            renderer.setShowGrid(show_grid);}





    }


    //turn off app when touch return button of phone
    @Override
    public boolean onKeyDown(int keyCode,KeyEvent event)
    {
        if(keyCode==KeyEvent.KEYCODE_BACK&&event.getRepeatCount()==0)
        {
            tgDevice.close();
            this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    //update live curve
    public void updateChart(XYSeries series,int newValue)
    {
        dataset.removeSeries(series);
        int length = series.getItemCount();
        //only maximum 50 points
        if(length>=50)
        {

            for(int i = 0;i<length-1;i++)
            {
                xv[i] = (int)series.getX(i);
                yv[i] = (int)series.getY(i+1);
            }
            series.clear();
            addX = length-1;
            addY = newValue;
            for(int j = 0;j<length-1;j++)
            {
                series.add(xv[j], yv[j]);
            }
            series.add(addX, addY);
            dataset.addSeries(series);
        }
        else
        {
            addX = length;
            addY = newValue;
            series.add(addX, addY);
            dataset.addSeries(series);
        }

        chart.invalidate();

    }



    /**
     * Handles messages from TGDevice
     */

    private final Handler handler = new Handler() {




        @Override
        public void handleMessage(Message msg) {



            switch (msg.what) {
                case TGDevice.MSG_MODEL_IDENTIFIED:
        		/*
        		 * now there is something connected,
        		 * time to set the configurations we need
        		 */
                    //tv.append("Model Identified\n");
                    Toast.makeText(MainActivity.this, "Model Identified",Toast.LENGTH_LONG).show();
                    tgDevice.setBlinkDetectionEnabled(true); // not allowed on EKG hardware, here to show the override message
                    tgDevice.setRespirationRateEnable(true);
                    break;

                case TGDevice.MSG_STATE_CHANGE:

                    switch (msg.arg1) {
                        case TGDevice.STATE_IDLE:
                            break;
                        case TGDevice.STATE_CONNECTING:
                            Toast.makeText(MainActivity.this, "Connecting",Toast.LENGTH_LONG).show();
                            break;
                        case TGDevice.STATE_CONNECTED:
                            Toast.makeText(MainActivity.this, "Connected",Toast.LENGTH_LONG).show();
                            tgDevice.start();
                            mAnimationDrawable.start();

                            tgDevice.pass_seconds = 15;
                            break;
                        case TGDevice.STATE_NOT_FOUND:
                            Toast.makeText(MainActivity.this, "Can't find device",Toast.LENGTH_LONG).show();
                            Toast.makeText(MainActivity.this, "Bluetooth devices must be paired 1st",Toast.LENGTH_LONG).show();
                            break;
	                /*case TGDevice.STATE_NOT_PAIRED:
	                	tv.append("not paired\n");
	                	break;*/
                        case TGDevice.STATE_DISCONNECTED:
                            Toast.makeText(MainActivity.this, "Disconnected",Toast.LENGTH_LONG).show();
                    }

                    break;
                case TGDevice.MSG_POOR_SIGNAL:
            	/* display signal quality when there is a change of state, or every 30 reports (seconds) */
                    if (subjectContactQuality_cnt >= 30 || msg.arg1 != subjectContactQuality_last) {
                        if (msg.arg1 == 200) { //200 is for BMD
                            Toast.makeText(MainActivity.this, "SignalQuality: is Good",Toast.LENGTH_LONG).show();
                        }
                        else {
                            Toast.makeText(MainActivity.this, "SignalQuality: is POOR: " + msg.arg1,Toast.LENGTH_LONG).show();
                        }
                        subjectContactQuality_cnt = 0;
                        subjectContactQuality_last = msg.arg1;
                    }
                    else subjectContactQuality_cnt++;
                    break;
                case TGDevice.MSG_RAW_DATA:



                    //updateChart(hseries,msg.arg1);
                    break;



                case TGDevice.MSG_HEART_RATE:
                   //updateChart(hseries,msg.arg1);
                    heartRate=msg.arg1;


                    heart_Rate.setText(msg.arg1+"");

                    updateChart(hseries,msg.arg1);


                    //updateChart(hseries,msg.arg1);
                    break;
                case TGDevice.MSG_ATTENTION:


                    break;
                case TGDevice.MSG_MEDITATION:


                    break;
                case TGDevice.MSG_BLINK:


                    break;
                case TGDevice.MSG_RAW_COUNT:


                    break;

                case TGDevice.MSG_EKG_RRINT:


                    //updateChart(hseries,msg.arg1);


                    break;

                case TGDevice.MSG_LOW_BATTERY:
                    Toast.makeText(getApplicationContext(), "Low battery!", Toast.LENGTH_SHORT).show();
                    break;
                case TGDevice.MSG_RAW_MULTI:

                    break;
                case TGDevice.MSG_RELAXATION:
                    tv_RelaxationLevel.setText(msg.arg1+"");
                    break;
                case TGDevice.MSG_RESPIRATION:
                    //print out about 64s after touching, then update per 10s
                    //Float r = (Float)msg.obj;
                    //tv_RespirationRate.setText(String.valueOf(msg.obj));
                    //Toast.makeText(getApplicationContext(), "Resp Rate: "+String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
                    break;
                case TGDevice.MSG_HEART_AGE:
                    //tv_HeartAge.setText( msg.arg1+"" );
                    break;
                case TGDevice.MSG_HEART_AGE_5MIN:
                    //tv_5minHeartAge.setText( msg.arg1+"" );
                    break;
                case TGDevice.MSG_EKG_IDENTIFIED:
                    //updateChart(hseries,msg.arg1);
                    break;

                case TGDevice.MSG_ERR_CFG_OVERRIDE:
                    switch (msg.arg1) {
                        case TGDevice.ERR_MSG_BLINK_DETECT:
                            Toast.makeText(getApplicationContext(), "Override: blinkDetect", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKFAMILIARITY:
                            Toast.makeText(getApplicationContext(), "Override: Familiarity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKDIFFICULTY:
                            Toast.makeText(getApplicationContext(), "Override: Difficulty", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_POSITIVITY:
                            Toast.makeText(getApplicationContext(), "Override: Positivity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_RESPIRATIONRATE:
                            Toast.makeText(getApplicationContext(), "Override: Resp Rate", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            Toast.makeText(getApplicationContext(), "Override: code: "+msg.arg1+"", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case TGDevice.MSG_ERR_NOT_PROVISIONED:
                    switch (msg.arg1) {
                        case TGDevice.ERR_MSG_BLINK_DETECT:
                            Toast.makeText(getApplicationContext(), "No Support: blinkDetect", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKFAMILIARITY:
                            Toast.makeText(getApplicationContext(), "No Support: Familiarity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKDIFFICULTY:
                            Toast.makeText(getApplicationContext(), "No Support: Difficulty", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_POSITIVITY:
                            Toast.makeText(getApplicationContext(), "No Support: Positivity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_RESPIRATIONRATE:
                            Toast.makeText(getApplicationContext(), "No Support: Resp Rate", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            Toast.makeText(getApplicationContext(), "No Support: code: "+msg.arg1+"", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    };
}

