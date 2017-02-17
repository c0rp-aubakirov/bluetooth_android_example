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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;


public class MainActivity extends AppCompatActivity {

    private static final long SCAN_PERIOD = 3 * 1000;

    private Switch bluePay_scan_Switch;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private BlueAdapter adapter;
    private List<BluePayDevice> devices;
    private Button submitButton;
    private EditText phone;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private BluetoothDevice device = null;

    private Handler mHandler;

    private Boolean mScanning = false;

    private static final String TO_WRITE = "00003a51";
    private static final String TO_READ = "00003a52";
    public static final String SERVICE = "0000183a";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        // bluetooth shit
        initBluetoothStaff();

        // activity element configs
        configureActivityElements();
        Utils.saveDevicesToDB(getApplicationContext(), new HashSet<BluePayDevice>());
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

                if (device.getName().toLowerCase().startsWith("bluepay")) {

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

        phone = (EditText) findViewById(R.id.phone);

        submitButton = (Button) findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<BluetoothGattService> services = mBluetoothGatt.getServices();
                for (BluetoothGattService service : services) {

                    final List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    final String servUUID = service.getUuid().toString();

                    if (servUUID.startsWith(SERVICE)) {
                        Log.d("SERVICE", "+++++++++++++++++++++++++++++");
                        Log.d("SERVICE_UUID", servUUID);

                        for (BluetoothGattCharacteristic characteristic : characteristics) {

                            final String charUUID = characteristic.getUuid().toString();

                            //0x2A25
                            Log.d("CHARACTERISTIC_UUID", charUUID);

                            if (charUUID.startsWith(TO_WRITE)) {
//                                mBluetoothGatt.readCharacteristic(characteristic);
                                Log.d("START_WRITE", charUUID);
                                characteristic.setValue(phone.getText().toString());
                                mBluetoothGatt.beginReliableWrite();
                                mBluetoothGatt.writeCharacteristic(characteristic);
                            }

                        }
                        Log.d("SERVICE", "-----------------------------");
                    }
                }
            }
        });

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
    }

    private void doConnectToClosestDevice() {

        final Set<BluePayDevice> devices = Utils.loadDevicesToDB(getApplicationContext());
        if (devices.isEmpty()) {
            Log.d("ACTION_FINISHED", "Device list is empty!");
            Toast.makeText(this, "DEVICES NOT FOUND", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.INVISIBLE);
            return;
        }

        final ArrayList<BluePayDevice> listOfAllDevices = new ArrayList<>(devices);
        Collections.sort(listOfAllDevices);

        final BluePayDevice bestConnectionCandidate = listOfAllDevices.get(0);
        final BluetoothDevice bestDevice = bestConnectionCandidate.getDevice();
        Log.d("BEST_DEVICE", bestDevice.getName());
        Log.d("BEST_DEVICE_RSSI", String.valueOf(bestConnectionCandidate.getRssi()));

        device = mBluetoothAdapter.getRemoteDevice(bestDevice.getAddress());

        mBluetoothGatt = device.connectGatt(getApplication(), true, mGattCallback);
        Log.d("BEST_DEVICE", "\n\tConnected to GATT server\n");
        progressBar.setVisibility(View.INVISIBLE);
        submitButton.setVisibility(View.VISIBLE);
    }

    private void traceDiscoveredServices() {
        final List<BluetoothGattService> services = mBluetoothGatt.getServices();

        for (BluetoothGattService service : services) {

            final List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            final String servUUID = service.getUuid().toString();

            if (servUUID.startsWith("0x183A")) {
                Log.d("SERVICE", "+++++++++++++++++++++++++++++");
                Log.d("SERVICE_UUID", servUUID);

                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    final String charUUID = characteristic.getUuid().toString();
                    Log.d("CHARACTERISTIC_UUID", charUUID);
                }
                Log.d("SERVICE", "-----------------------------");
            }
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d("RSSI", String.valueOf(rssi));
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("GATT_CONNECTION", "Connected to GATT server.");

                if (device != null && gatt.getDevice().equals(device)) {
                    Log.d("GATT_DISCOVERY", "STARTED");
                    mBluetoothGatt.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("GATT_DISCONNECTION", "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("GATT_DISCOVERY", "FINISHED " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
//                traceDiscoveredServices();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            final String msg = new String(characteristic.getValue());

           Log.d("START_READ", msg);
            if (characteristic.getUuid().toString().startsWith(TO_READ)) {
                Log.d("START_READ", msg);

                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();

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

            if (characteristic.getUuid().toString().startsWith(TO_WRITE)) {
                Log.d("FINISH_WRITE", new String(characteristic.getValue()));
                mBluetoothGatt.executeReliableWrite();

                runOneShotTask(characteristic);

            }

            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };


    public ScheduledFuture<?> runOneShotTask(final BluetoothGattCharacteristic characteristic) {

        final ScheduledFuture<?> future = scheduler.schedule(getCommand(characteristic) , 3, SECONDS );

        return future;

    }

    @NonNull
    private Runnable getCommand(final BluetoothGattCharacteristic characteristic) {
        return new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.readCharacteristic(characteristic);
            }
        };
    }
}
