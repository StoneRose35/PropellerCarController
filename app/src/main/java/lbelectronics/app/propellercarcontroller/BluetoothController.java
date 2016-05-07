package lbelectronics.app.propellercarcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.app.Activity;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by philipp on 02.05.15.
 */
public class BluetoothController implements BluetoothThreadListener{

    private int REQUEST_ENABLE_BT=1;
    private int state=0;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothConnectedListener btListener;
    private BluetoothSocket socket;
    private MyBroadcastReceiver myBroadcastReceiver;
    private ConnectedThread connectedThread;
    private String BLUETOOTH_NAME_CAR;
    private String BLUETOOTH_REMOTE_CONTROL="PropCarControl";
    private UUID APP_UUID=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private Activity parentActivity;
    private SettingsActivity settingsActivity;
    public BluetoothController()
    {

    }



    public BluetoothController(BluetoothConnectedListener bluetoothConnectedListener,Activity baseActivity)
    {
        parentActivity=baseActivity;
        btListener=bluetoothConnectedListener;
    }


    public boolean Initialize()
    {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(parentActivity);
        BLUETOOTH_NAME_CAR = prefs.getString("pref_btname","");
        boolean res=false;
        if(state<1) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                btListener.IsConnected(false);
            }
            else if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                parentActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else
            {
                state=1;
                Initialize();
            }
        }
        else
        {

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
// If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    if(device.getName().contains(BLUETOOTH_NAME_CAR))
                    {
                        res=true;
                        mBluetoothDevice=device;
                        /*try {
                            socket = device.createRfcommSocketToServiceRecord(APP_UUID);
                            ReturnedBluetoothSocket(this.socket);
                        }
                        catch (IOException exc)
                        {
                            exc.printStackTrace();
                        }*/

                    }
                }
            }

            if(!res) // device not connected yet, look for it
            {
                myBroadcastReceiver = new MyBroadcastReceiver(this);
// Register the BroadcastReceiver
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                parentActivity.registerReceiver(myBroadcastReceiver, filter); // Don't forget to unregister during onDestroy
                mBluetoothAdapter.startDiscovery();
            }
            else {
                ConnectThread connectThread = new ConnectThread(this);
                connectThread.start();
            }
        }
        return res;
    }

    public void TearDown()
    {
        state=0;
        btListener.IsConnected(false);
        if(connectedThread!=null) {
            connectedThread.cancel();
        }
        if(myBroadcastReceiver!=null) {
            parentActivity.unregisterReceiver(myBroadcastReceiver);
        }
    }

    public void setState(int newstate)
    {
        state=newstate;
    }

    public void ReturnedBluetoothSocket(BluetoothSocket socket)
    {
        this.socket=socket;
        connectedThread=new ConnectedThread(this.socket);
        connectedThread.start();
        btListener.IsConnected(true);
    }

    public void SendCoordinates(int x,int y)
    {
        String cmdString;
        if(connectedThread!=null) {
            if (connectedThread.isAlive()) {
                cmdString=String.valueOf(x) + "," + String.valueOf(y) + "%";
                connectedThread.write(cmdString);
            }
        }
    }

    public void SendDirection(boolean forward)
    {
        String cmdString;
        if(connectedThread!=null) {
            if (connectedThread.isAlive()) {
                if(forward)
                {
                    cmdString="F%";
                }
                else
                {
                    cmdString="B%";
                }
                connectedThread.write(cmdString);
            }
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        private BluetoothThreadListener listener;
        public ConnectThread(BluetoothThreadListener listenerIn) {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(APP_UUID);  //.createRfcommSocketToServiceRecord(APP_UUID);
                listener=listenerIn;
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    mmSocket.connect();
                    if(listener!=null)
                    {
                        listener.ReturnedBluetoothSocket(mmSocket);
                    }
                } catch (IOException e) {
                    try {
                        mmSocket.close();
                    }
                    catch(IOException ce){}
                }
                break;
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket ctmmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private int waitdelay;
        private String writebuffer="";
        public ConnectedThread(BluetoothSocket socket) {
            SharedPreferences prefs;
            prefs = PreferenceManager.getDefaultSharedPreferences(parentActivity);
            waitdelay=Integer.parseInt(prefs.getString("pref_srate","20"));
            ctmmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                /*
                try {
                    // Read from the InputStream
                    //bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                    //        .sendToTarget();
                } catch (IOException e) {
                    break;
                }*/
                try {
                    mmOutStream.write("S".getBytes());
                } catch (IOException exc) {}
                for(int cc=0;cc<10;cc++) {
                    if(writebuffer.length()>0 && (cc==0 || cc==3 || cc==6))
                    {
                        try {
                            mmOutStream.write(writebuffer.getBytes());
                            writebuffer="";
                        } catch (IOException exc) {}
                    }
                    try {
                        Thread.sleep(waitdelay / 10);
                    } catch (InterruptedException exc) {
                        try {
                            ctmmSocket.close();
                        } catch (IOException exc1) {
                        }
                    }
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String toWrite) {
            writebuffer=toWrite;
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                ctmmSocket.close();
            } catch (IOException e) { }
        }
    }

    class MyBroadcastReceiver extends BroadcastReceiver
    {
        private BluetoothController btcntrl;
        public MyBroadcastReceiver(BluetoothController parent)
        {
            btcntrl=parent;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                if(device.getName().contains(BLUETOOTH_NAME_CAR)) {
                    mBluetoothDevice = device;
                    mBluetoothAdapter.cancelDiscovery();
                    ConnectThread connectThread = new ConnectThread(btcntrl);
                    connectThread.start();
                }
            }
        }
    }
}
