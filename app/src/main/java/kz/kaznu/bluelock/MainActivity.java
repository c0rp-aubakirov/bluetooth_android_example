package kz.kaznu.bluelock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    private static final long SCAN_PERIOD = 3 * 1000;
    public static final int ANSWER_FROM_SERVER = 1;
    private static final int DISCOVER_STARTED = 2;
    public static final int DISCOVER_FINISHED = 3;
    private static final int STATE_DISCONNECTED = 4;

    private Switch bluePay_scan_Switch;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private BlueAdapter adapter;
    private List<BluePayDevice> devices;
    private Button submitButton;
    private EditText phone;
    private TextView textView;
    private LinearLayout sendMessageLayout;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private BluetoothDevice currentConnectedDevice = null;

    private Handler mHandler;

    private Boolean mScanning = false;

    private BluetoothGattCharacteristic writeChar;
    private BluetoothGattCharacteristic readChar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configureLooper();

        // bluetooth shit
        initBluetoothStaff();

        // activity element configs
        configureActivityElements();
        Utils.saveDevicesToDB(getApplicationContext(), new HashSet<BluePayDevice>());
    }

    private void configureLooper() {
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                textView.setText(message.obj.toString());
                Toast.makeText(MainActivity.this, message.obj.toString(), Toast.LENGTH_SHORT).show();

                if (message.what == ANSWER_FROM_SERVER) {
                    progressBar.setVisibility(View.INVISIBLE);
                }

                if (message.what == DISCOVER_FINISHED) {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }
        };
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

        sendMessageLayout = (LinearLayout) findViewById(R.id.send_message);

        textView = (TextView) findViewById(R.id.answer_view);

        phone = (EditText) findViewById(R.id.phone);

        submitButton = (Button) findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                final String value = phone.getText().toString();
                Log.d("START_WRITE", value);

                writeChar.setValue(value);
                mBluetoothGatt.writeCharacteristic(writeChar);
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
                                startScanLeDevice();
                                progressBar.setVisibility(View.VISIBLE);
                            }

                        } else {
                            Log.d("MAIN", "Check false");
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
            textView.setText("Устройства BluePay не найдены");
            progressBar.setVisibility(View.INVISIBLE);
            return;
        }

        final ArrayList<BluePayDevice> listOfAllDevices = new ArrayList<>(devices);
        Collections.sort(listOfAllDevices);

        final BluePayDevice bestConnectionCandidate = listOfAllDevices.get(0);
        final BluetoothDevice bestDevice = bestConnectionCandidate.getDevice();
        Log.d("BEST_DEVICE", bestDevice.getName());
        Log.d("BEST_DEVICE_RSSI", String.valueOf(bestConnectionCandidate.getRssi()));

        currentConnectedDevice = mBluetoothAdapter.getRemoteDevice(bestDevice.getAddress());

        mBluetoothGatt = currentConnectedDevice.connectGatt(getApplication(), true, mGattCallback);

        textView.setText("Подключаемся к устройству " + bestDevice.getName());
    }

    private void traceDiscoveredServices() {
        final List<BluetoothGattService> services = mBluetoothGatt.getServices();

        for (BluetoothGattService service : services) {

            final List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            final String servUUID = service.getUuid().toString();

            if (servUUID.startsWith(Constants.SERVICE)) {

                for (BluetoothGattCharacteristic characteristic : characteristics) {

                    final String charUUID = characteristic.getUuid().toString();

                    if (charUUID.startsWith(Constants.TO_WRITE)) {
                        Log.d("DISCOVER", "Write characteristic detected: " + charUUID);
                        writeChar = characteristic;
                    }

                    if (charUUID.startsWith(Constants.TO_READ)) {
                        Log.d("DISCOVER", "Read characteristic detected: " + charUUID);
                        readChar = characteristic;
                    }

                }
            }

            if (servUUID.startsWith(Constants.SERVICE_GAP)) {

                for (BluetoothGattCharacteristic characteristic : characteristics) {

                    final String charUUID = characteristic.getUuid().toString();

                    if (charUUID.startsWith(Constants.DEVICE_NAME_CHARACTERISTIC)) {
                        mBluetoothGatt.readCharacteristic(characteristic);
                    }
                }
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
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d("CON_ST_CHAN", "Connected to GATT server");

                    if (currentConnectedDevice != null && gatt.getDevice().equals(
                            currentConnectedDevice)) {
                        Log.d("CON_ST_CHAN", "Discovery started");
                        mBluetoothGatt.discoverServices();
                        mHandler.obtainMessage(DISCOVER_STARTED, "Начинаем настройку").sendToTarget();
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d("CON_ST_CHAN", "Disconnected from GATT server.");
                    mHandler.obtainMessage(STATE_DISCONNECTED, "Отключен от устройства. Запустите сканирование еще раз").sendToTarget();
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("GATT_DISCOVERY", "FINISHED " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                traceDiscoveredServices();
                mHandler.obtainMessage(DISCOVER_FINISHED, "Настройка закочена. Можно начинать сообщение между устройствами").sendToTarget();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            final String msg = new String(characteristic.getValue());
            Log.d("START_READ", msg);

            if (characteristic.getUuid().toString().startsWith(Constants.TO_READ)) {
                Log.d("START_READ", msg);
                // And this is how you call it from the worker thread:
                mHandler.obtainMessage(ANSWER_FROM_SERVER,
                        "Получен ответ от сервера allpay: \n" + msg).sendToTarget();

            }
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d("CHAR_CHANGED", new String(characteristic.getValue()));
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {

            if (characteristic.getUuid().toString().startsWith(Constants.TO_WRITE)) {
                final String msg = new String(characteristic.getValue());
                Log.d("FINISH_WRITE", msg);

                mHandler.obtainMessage(0, "Сообщение отправлено. Ждем ответа\n\t" + msg).sendToTarget();

                runOneShotTask(readChar);
            }
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };


    public void runOneShotTask(final BluetoothGattCharacteristic characteristic) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        Log.d("BACKGROUND_TASK", "Started");
                        mBluetoothGatt.readCharacteristic(characteristic);
                        return null;
                    }
                }.execute();
            }
        };
        mHandler.postDelayed(r, 3000);
    }

}
