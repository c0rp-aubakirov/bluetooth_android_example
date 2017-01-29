package kz.kaznu.bluelock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


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

    private List<BluePayDevice> devices = new ArrayList<>();

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

        // SCREEN OFF ACTION registration
        filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        this.registerReceiver(mReceiver, filter);

        // SCREEN ON ACTION registration
        filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        this.registerReceiver(mReceiver, filter);

        // activity element configs
        configureActivityElements();
    }

    /**
     * Здесь прописаны настройки элементов на экране activity_main
     */
    private void configureActivityElements() {
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
            final String action = intent.getAction();
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
                    devices.clear();
                }
            }
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // DO NOT TRY TO READ UUID WHILE IN DISCOVERY MODE !!!

                // Get the BluetoothDevice object from the Intent
                final BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // RSSI
                final short rssi =
                        intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Log.d("ACTION_FOUND", "\n=================================");
                    Log.d("ACTION_FOUND", device.getName() + "\t" + device.getAddress());
                    Log.d("ACTION_FOUND", Short.toString(rssi));
                    // DO NOT TRY TO READ UUID WHILE IN DISCOVERY MODE !!!
                    Log.d("ACTION_FOUND", "=================================\n");

                    if (true || device.getName().toLowerCase().startsWith("bluepay")) {

                        final String[] complexName = device.getName().split("y");
                        final String name = complexName[0];

                        final String uniqueCode;
                        if (complexName.length > 1) {
                            uniqueCode = complexName[1];
                        } else {
                            uniqueCode = "FAILED_TO_PARSE";
                        }

                        final BluePayDevice bluePayDevice =
                                new BluePayDevice(name, uniqueCode, device.getAddress(), rssi,
                                        device);
                        devices.add(bluePayDevice);
                    }

                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d("ACTION_FINISHED", "Finished");

                if (devices.isEmpty()) {
                    Log.d("ACTION_FINISHED", "Device list is empty!");
                    return;
                }

                Collections.sort(devices);

                Log.d("ACTION_FINISHED", devices.toString());

                final BluePayDevice bestConnectionCandidate = devices.get(0);
                final BluetoothDevice device = bestConnectionCandidate.getDevice();
                device.fetchUuidsWithSdp();

                for (ParcelUuid parcelUuid : device.getUuids()) {
                    Log.d("UUID", parcelUuid.getUuid().toString());
                }

                try {
                    // Here write connection code
                    device.createInsecureRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
                    device.createRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());

                } catch (IOException e) {
                    Log.d("ERROR_CONNECT", e.toString());
                    e.printStackTrace();
                }
            }
        }
    };
}
