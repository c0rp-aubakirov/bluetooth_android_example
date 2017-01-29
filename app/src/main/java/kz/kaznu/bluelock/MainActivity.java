package kz.kaznu.bluelock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {
    private CheckBox Bluetooth_supported_CheckBox;
    private CheckBox Bluetooth_low_energy_supported_CheckBox;
    private Switch bluePay_scan_Switch;


    private Vibrator mVibrator;


    public ListView Devices_List;
    public String[] Data_list = {};
    public ArrayList<String> listItems;
    public ArrayAdapter<String> List_Adapter;


    public BluetoothAdapter mBluetoothAdapter;
    private RadioButton Bluetooth_enable_RadioButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        this.registerReceiver(mReceiver, filter);


        //Prepare interface objects
        Bluetooth_supported_CheckBox = (CheckBox) findViewById(R.id.Bluetooth_supported_CheckBox);
        Bluetooth_low_energy_supported_CheckBox =
                (CheckBox) findViewById(R.id.Bluetooth_low_energy_supported_CheckBox);
        Bluetooth_enable_RadioButton =
                (RadioButton) findViewById(R.id.Bluetooth_enable_RadioButton);
        Bluetooth_enable_RadioButton =
                (RadioButton) findViewById(R.id.Bluetooth_enable_RadioButton);
        bluePay_scan_Switch = (Switch) findViewById(R.id.bluePay_scan_Switch);


        Devices_List = (ListView) findViewById(R.id.Devices_List);
        listItems = new ArrayList<>(Arrays.asList(Data_list));
        List_Adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        Devices_List.setAdapter(List_Adapter);


        //Prepare vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        //Check basic functions for Bluetooth
        if (mBluetoothAdapter != null) {
            Bluetooth_supported_CheckBox.setChecked(true);

            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Bluetooth_low_energy_supported_CheckBox.setChecked(true);
            } else {
                Toast.makeText(this, "Device not suported BLE", Toast.LENGTH_SHORT).show();
                Bluetooth_low_energy_supported_CheckBox.setChecked(false);
            }// if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        } else {
            Toast.makeText(this, "Device not suported Bluetooth", Toast.LENGTH_SHORT).show();
            Bluetooth_supported_CheckBox.setChecked(false);
        }//if (mBluetoothAdapter != null)


        bluePay_scan_Switch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked || !mBluetoothAdapter.isDiscovering()) {
                            Log.d("MAIN", "Check true");
                            mBluetoothAdapter.startDiscovery();
                        } else {
                            mBluetoothAdapter.cancelDiscovery();
                            Log.d("MAIN", "Check false");
                        }
                    }
                });
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("ACTION", action);

            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                if (mBluetoothAdapter.isDiscovering()) {
                    Log.d("MAIN", "Cancel discovering");
                    mBluetoothAdapter.cancelDiscovery();
                }
            }

            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if (!mBluetoothAdapter.isDiscovering()) {
                    Log.d("MAIN", "Start discovering");
                    mBluetoothAdapter.startDiscovery();
                }
            }
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // RSSI
                final short rssi =
                        intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Log.d("ACTION_FOUND", "\n=================================");
                    Log.d("ACTION_FOUND", device.getName() + "\t" + device.getAddress());
//                    for (ParcelUuid parcelUuid : device.getUuids()) {
//                        Log.d("UUID", parcelUuid.getUuid().toString());
//                    }
                    Log.d("ACTION_FOUND", Short.toString(rssi));
                    Log.d("ACTION_FOUND", "=================================\n");

                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d("ACTION_FINISHED", "Finished");
            }
        }
    };

}//public class MainActivity extends AppCompatActivity




/*

if (mBluetoothAdapter.isEnabled()) {
    Bluetooth_enable_RadioButton.setChecked(true);
}
else
{
    Bluetooth_enable_RadioButton.setChecked(false);

    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

}//if (mBluetoothAdapter.isEnabled())

*/
