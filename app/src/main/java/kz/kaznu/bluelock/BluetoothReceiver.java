package kz.kaznu.bluelock;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothReceiver extends android.content.BroadcastReceiver {

    public BluetoothReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BLE", "RECEIVE");

        String action = intent.getAction();

        Log.d("BLE", action);

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            //the device is found
            Log.d("BLE", device.getName());
            Log.d("BLE", device.toString());
        }
    }
}
