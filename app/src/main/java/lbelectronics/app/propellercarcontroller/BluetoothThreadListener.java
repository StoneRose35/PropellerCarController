package lbelectronics.app.propellercarcontroller;

import android.bluetooth.BluetoothSocket;
/**
 * Created by philipp on 04.05.15.
 */
public interface BluetoothThreadListener {
    public void ReturnedBluetoothSocket(BluetoothSocket socket);
}
