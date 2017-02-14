package kz.kaznu.bluelock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    private static final long SCAN_PERIOD = 10 * 1000;

    private Switch bluePay_scan_Switch;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private BlueAdapter adapter;
    private List<BluePayDevice> devices;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;

    private Handler mHandler;

    private Boolean mScanning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        // bluetooth shit
        initBluetoothStaff();

        // activity element configs
        configureActivityElements();

        Set<BluePayDevice> devices = Utils.loadDevicesToDB(getApplicationContext());
        System.out.println(devices);
    }

    // Init of bluetooth shit
    private void initBluetoothStaff() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, final int blerssi,
                                 byte[] scanRecord) {
                Log.d("ACTION_FOUND", "\n=================================");
                Log.d("ACTION_FOUND", device.getName() + "\t" + device.getAddress() + "\t");
                Log.d("ACTION_FOUND", Integer.toString(blerssi));
                Log.d("ACTION_FOUND", "=================================\n");


                if (device.getName() == null) return;

                if (device.getName().toLowerCase().startsWith("preved")) {

                    final String[] complexName = device.getName().split("_");
                    final String name = complexName[0];


                    final String uniqueCode;
                    if (complexName.length > 1) {
                        uniqueCode = complexName[1];
                    } else {
                        uniqueCode = "FAILED_TO_PARSE";
                    }

                    final BluePayDevice bluePayDevice =
                            new BluePayDevice(name, uniqueCode, device.getAddress(), blerssi,
                                    device);
                    devices.clear();
                    devices.addAll(Utils.addOneDeviceToDB(getApplicationContext(), bluePayDevice));
                    adapter.notifyDataSetChanged();
                }
            }
        };
    }

    /**
     * Здесь прописаны настройки элементов на экране activity_main
     */
    private void configureActivityElements() {
        devices = new ArrayList<>(Utils.loadDevicesToDB(getApplicationContext()));

        adapter = new BlueAdapter(getApplicationContext(), devices);
        recyclerView = (RecyclerView) findViewById(R.id.scanned_devices);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        //Prepare interface objects
        bluePay_scan_Switch = (Switch) findViewById(R.id.bluePay_scan_Switch);

        bluePay_scan_Switch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {

                            if (!mScanning && !mBluetoothAdapter.isDiscovering()) {
                                Log.d("SWITCH_ON", "Check true");
                                Toast.makeText(MainActivity.this, "Start scanning",
                                        Toast.LENGTH_SHORT).show();
//                                devices.clear();
                                startScanLeDevice();
                                progressBar.setVisibility(View.VISIBLE);
                            }

                        } else {
                            Log.d("MAIN", "Check false");
                            Toast.makeText(MainActivity.this, "Start connection",
                                    Toast.LENGTH_SHORT).show();
                            doConnectToClosestDevice();
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
    }

    // Run scanning and post delayed stop
    private void startScanLeDevice() {
        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanLeDevice();
            }
        }, SCAN_PERIOD);

        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        bluePay_scan_Switch.setChecked(true);
    }

    // Stop scanning
    private void stopScanLeDevice() {
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        bluePay_scan_Switch.setChecked(false);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void doConnectToClosestDevice() {

        final Set<BluePayDevice> devices = Utils.loadDevicesToDB(getApplicationContext());
        if (devices.isEmpty()) {
            Log.d("ACTION_FINISHED", "Device list is empty!");
            Toast.makeText(this, "DEVICES NOT FOUND", Toast.LENGTH_SHORT).show();
            return;
        }

        final ArrayList<BluePayDevice> listOfAllDevices = new ArrayList<>(devices);
        Collections.sort(listOfAllDevices);

        final BluePayDevice bestConnectionCandidate = listOfAllDevices.get(0);
        final BluetoothDevice best_device = bestConnectionCandidate.getDevice();
        Log.d("BEST_DEVICE", best_device.getName());
        Log.d("BEST_DEVICE_RSSI", String.valueOf(bestConnectionCandidate.getRssi()));

        final BluetoothDevice device =
                mBluetoothAdapter.getRemoteDevice(best_device.getAddress());

        mBluetoothGatt = device.connectGatt(getApplication(), true, mGattCallback);
        Log.d("BEST_DEVICE", "\n\tConnected to GATT server\n");
    }

    private void traceDiscoveredServices() {
        final List<BluetoothGattService> services = mBluetoothGatt.getServices();

        for (BluetoothGattService service : services) {

            final List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            final String servUUID = service.getUuid().toString();

            Log.d("SERVICE", "+++++++++++++++++++++++++++++");
            Log.d("SERVICE_UUID", servUUID);

            for (BluetoothGattCharacteristic characteristic : characteristics) {

                final String charUUID = characteristic.getUuid().toString();

                //0x2A25
                Log.d("CHARACTERISTIC_UUID", charUUID);

                if (charUUID.startsWith("00002a00")) {
                    mBluetoothGatt.readCharacteristic(characteristic);
                }
            }
            Log.d("SERVICE", "-----------------------------");
        }
    }


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("GATT_CONNECTION", "Connected to GATT server.");
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("GATT_DISCONNECTION", "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("GATT_DISCOVERY", "onServicesDiscovered: " + status);
                traceDiscoveredServices();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (characteristic.getUuid().toString().startsWith("00002a00")) {
                Log.d("INFO_SERIAL_NUMBER", new String(characteristic.getValue()));

                characteristic.setValue("PREVED MEDVED!");
                mBluetoothGatt.beginReliableWrite();
                mBluetoothGatt.writeCharacteristic(characteristic);

            }
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            Log.d("CHAR_WRITE_CALLBACK", new String(characteristic.getValue()));

            if (characteristic.getUuid().toString().startsWith("00002a00")) {
                Log.d("INFO_SERIAL_NUMBER", new String(characteristic.getValue()));
                mBluetoothGatt.executeReliableWrite();
            }
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };
}
