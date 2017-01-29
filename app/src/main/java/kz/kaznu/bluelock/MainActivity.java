package kz.kaznu.bluelock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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


public class MainActivity extends AppCompatActivity
{
    //Public flag for bluePay scan Activity
    public boolean Start_Scan_bluePay_flag=false;

    private CheckBox Bluetooth_supported_CheckBox;
    private CheckBox Bluetooth_low_energy_supported_CheckBox;
    private RadioButton Bluetooth_enable_RadioButton;
    private Switch bluePay_scan_Switch;


    private Vibrator mVibrator;


    public ListView Devices_List;
    public String[] Data_list = {};
    public ArrayList<String> listItems;
    public ArrayAdapter<String> List_Adapter;


    public BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;


    int clickCounter=0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Prepare interface objects
        Bluetooth_supported_CheckBox = (CheckBox) findViewById(R.id.Bluetooth_supported_CheckBox);
        Bluetooth_low_energy_supported_CheckBox = (CheckBox) findViewById(R.id.Bluetooth_low_energy_supported_CheckBox);
        Bluetooth_enable_RadioButton = (RadioButton) findViewById(R.id.Bluetooth_enable_RadioButton);
        Bluetooth_enable_RadioButton = (RadioButton) findViewById(R.id.Bluetooth_enable_RadioButton);
        bluePay_scan_Switch = (Switch) findViewById(R.id.bluePay_scan_Switch);


        Devices_List = (ListView) findViewById(R.id.Devices_List);
        listItems=new ArrayList<>(Arrays.asList(Data_list));
        List_Adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        Devices_List.setAdapter(List_Adapter);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(new BroadcastReceiver() {
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
        }, filter);



        //Prepare vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


//        mBluetoothAdapter.


        //Setup parameters
//        bluePay_scan_Switch.setChecked(false);
//        Start_Scan_bluePay_flag = false;



        //Check basic functions for Bluetooth
        if (mBluetoothAdapter != null)
        {
            Bluetooth_supported_CheckBox.setChecked(true);

            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
            {
                Bluetooth_low_energy_supported_CheckBox.setChecked(true);
            }
            else
            {
                Toast.makeText(this, "Device not suported BLE", Toast.LENGTH_SHORT).show();
                Bluetooth_low_energy_supported_CheckBox.setChecked(false);
            }// if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        }
        else
        {
            Toast.makeText(this, "Device not suported Bluetooth", Toast.LENGTH_SHORT).show();
            Bluetooth_supported_CheckBox.setChecked(false);
        }//if (mBluetoothAdapter != null)



        bluePay_scan_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if(isChecked)
                {
                    //listItems.add("ololoshechka");
                    //List_Adapter.notifyDataSetChanged();
//                    mBluetoothAdapter.startLeScan(mLeScanCallback);
                }
                else
                {
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                }//if(isChecked)
            }//public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        }); //bluePay_scan_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()




    }// protected void onCreate(Bundle savedInstanceState)








//    private void scanLeDevice(final boolean enable) {
//
//
//        //!Develop
//        //Find classical devices
//        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            mBluetoothAdapter.getBluetoothLeScanner().startScan(filterList, scanSetting, scanCallback);
//        } else {
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
//        }*/
//
//        if (enable) {
//            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                }
//            }, SCAN_PERIOD);
//
//            mScanning = true;
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
//        } else {
//            mScanning = false;
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//        }
//
//    }

//    // Device scan callback.
//    private BluetoothAdapter.LeScanCallback mLeScanCallback =
//            new BluetoothAdapter.LeScanCallback() {
//
//                @Override
//                public void onLeScan(final BluetoothDevice device, int RSSI, byte[] scanRecord) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            //List_Adapter.clear();
//                            listItems.add(device.getName() );
//                            List_Adapter.notifyDataSetChanged();
//                        }
//                    });
//                }
//            };



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
