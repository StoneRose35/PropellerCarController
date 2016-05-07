package lbelectronics.app.propellercarcontroller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity implements JoystickEventListener,BluetoothConnectedListener{

    private int seekbar1Value,seekbar2Value;
    private BluetoothController btcontroller;
    private boolean bluetooth_connected=false;
    private int currentXX,currentY;
    private SeekBar.OnSeekBarChangeListener seekBar1Listener,seekBar2Listener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onStart()
    {
        super.onStart();
        JoystickControl jscontrol;

        btcontroller = new BluetoothController(this,this);
        setContentView(R.layout.activity_main);
        SeekBar sb;

        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        jscontrol=(JoystickControl)findViewById(R.id.jscontroller);
        jscontrol.registerEventListener(this);
        jscontrol.SetCalibDelta(Float.parseFloat(prefs.getString("pref_calibx","0.0f")),Float.parseFloat(prefs.getString("pref_caliby","0.0f")));

        Button btn=(Button)findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bluetooth_connected)
                {
                    btcontroller.Initialize();
                }
            }
        });

        Button btn2=(Button)findViewById(R.id.button2);
        btn2.setOnClickListener(new View.OnClickListener(){
            @Override
        public void onClick(View v)
            {
                JoystickControl jscontrol=(JoystickControl)findViewById(R.id.jscontroller);
                jscontrol.SetCalibDelta(currentXX,currentY);
                SharedPreferences prefs;
                prefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("pref_calibx",currentXX+"");
                editor.putString("pref_caliby",currentY+"");
                editor.commit();
            }
        });
    }

    public void IsConnected(boolean res)
    {
        bluetooth_connected=res;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv2;
        tv2=(TextView)findViewById(R.id.textView2);
        if(bluetooth_connected) {
            tv2.setText("Bluetooth Connected");
        }
        else
        {
            tv2.setText("No Bluetooth Connection");
        }}});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent=new Intent(this,SettingsActivity.class);
            //intent.setAction("lbelectronics.app.propellercarcontroller.SettingsActivity");
            this.startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    public void startDragging(int x,int y)
    {
        currentXX=x;
        currentY=y;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv;
                tv=(TextView)findViewById(R.id.textView2);
                tv.setText("Start dragging at: (" + currentXX + "/" + currentY + ")");
            }
        });
        if(bluetooth_connected)
        {
            btcontroller.SendCoordinates(x,y);
        }
    }


    @Override
    protected void onActivityResult(int requestcode,int resultcode,Intent data)
    {
        if(resultcode!=RESULT_OK)
        {
            btcontroller.setState(-1);

        }
        else
        {
            btcontroller.setState(1);
            btcontroller.Initialize();
        }
    }

    @Override
    protected void onStop()
    {
        btcontroller.TearDown();
        super.onStop();
    }



    @Override
    protected void onDestroy() {
        btcontroller.TearDown();
        super.onDestroy();

    }
}
