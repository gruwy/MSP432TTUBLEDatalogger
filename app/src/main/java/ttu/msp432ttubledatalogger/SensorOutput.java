package ttu.msp432ttubledatalogger;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.UUID;

public class SensorOutput extends AppCompatActivity {
    private static final String TAG = "Data Output";

    // Declaring required items for sensor output
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private ProgressDialog mProgress;
    private TextView mTemperature, mBarometer, mHumidity, mOptic;

    /* Temp Service */
    private static final UUID TEMP_SERVICE = UUID.fromString("f000aa00-0451-4000-b000-000000000000");
    private static final UUID TEMP_DATA = UUID.fromString("f000aa01-0451-4000-b000-000000000000");
    private static final UUID TEMP_CONFIG = UUID.fromString("f000aa02-0451-4000-b000-000000000000");

    /* Humidity Service */
    public static final UUID HUMIDITY_SERVICE = UUID.fromString("f000aa20-0451-4000-b000-000000000000");
    public static final UUID HUMIDITY_DATA = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
    public static final UUID HUMIDITY_CONFIG = UUID.fromString("f000aa22-0451-4000-b000-000000000000");

    /* Barometer Service */
    public static final UUID BAROMETER_SERVICE = UUID.fromString("f000aa40-0451-4000-b000-000000000000");
    public static final UUID BAROMETER_DATA = UUID.fromString("f000aa41-0451-4000-b000-000000000000");
    public static final UUID BAROMETER_CONFIG = UUID.fromString("f000aa42-0451-4000-b000-000000000000");

    /* Optical Service */
    public static final UUID OPTIC_SERVICE = UUID.fromString("f000aa70-0451-4000-b000-000000000000");
    public static final UUID OPTIC_DATA = UUID.fromString("f000aa71-0451-4000-b000-000000000000");
    public static final UUID OPTIC_CONFIG = UUID.fromString("f000aa72-0451-4000-b000-000000000000");

    /* Client Configuration Descriptor */
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        BluetoothDevice device = Objects.requireNonNull(getIntent().getExtras()).getParcelable("Bluetooth_Device");

        mTemperature = findViewById(R.id.temperatureData);
        mBarometer = findViewById(R.id.barometerData);
        mHumidity = findViewById(R.id.humidityData);
        mOptic = findViewById(R.id.opticData);

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        assert manager != null;
        mBluetoothAdapter = manager.getAdapter();

        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);

        assert device != null;
        mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Connecting to the device\n'" + device.getName() + "'."));
        connect(device);
    }

    public void connect(BluetoothDevice device) {
        if (mBluetoothGatt == null) {
            mBluetoothGatt = device.connectGatt(SensorOutput.this, false, mGattCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mProgress.dismiss();
        finish();
        }

    @Override
    protected void onStop() {
        super.onStop();   
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            }
    }

    @Override
    public void onBackPressed() {
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        finish();
        Toast.makeText(getApplicationContext(),"Disconnected from the device.",Toast.LENGTH_SHORT).show();
    }

    // Design
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.data_menu, menu);
        MenuItem rssi = menu.findItem(R.id.rssi);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_disconnect:
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                finish();
                Toast.makeText(getApplicationContext(),"Disconnected from the device.",Toast.LENGTH_SHORT).show();
                return true;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /* State Machine */
        private int mState = 0;

        private void reset() { mState = 0; }

        private void advance() { mState++; }

		// Sensor enabling cases
        private void enableNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "Enabling temperature");
                    characteristic = gatt.getService(TEMP_SERVICE)
                            .getCharacteristic(TEMP_CONFIG);
                    characteristic.setValue(new byte[] {0x01});
                    break;
                case 1:
                    Log.d(TAG, "Enabling humidity");
                    characteristic = gatt.getService(HUMIDITY_SERVICE)
                            .getCharacteristic(HUMIDITY_CONFIG);
                    characteristic.setValue(new byte[] {0x01});
                    break;
                case 2:
                    Log.d(TAG, "Enabling barometer");
                    characteristic = gatt.getService(BAROMETER_SERVICE)
                            .getCharacteristic(BAROMETER_CONFIG);
                    characteristic.setValue(new byte[] {0x01});
                    break;
                case 3:
                    Log.d(TAG, "Enabling optic");
                    characteristic = gatt.getService(OPTIC_SERVICE)
                            .getCharacteristic(OPTIC_CONFIG);
                    characteristic.setValue(new byte[] {0x01});
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "Sensors enabled.");
                    return;
            }
            gatt.writeCharacteristic(characteristic);
        }

		// Sensor reading cases
        private void readNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "Reading temperature");
                    characteristic = gatt.getService(TEMP_SERVICE)
                            .getCharacteristic(TEMP_CONFIG);
                    break;
                case 1:
                    Log.d(TAG, "Reading humidity");
                    characteristic = gatt.getService(HUMIDITY_SERVICE)
                            .getCharacteristic(HUMIDITY_CONFIG);
                    break;
                case 2:
                    Log.d(TAG, "Reading barometer");
                    characteristic = gatt.getService(BAROMETER_SERVICE)
                            .getCharacteristic(BAROMETER_CONFIG);
                    break;
                case 3:
                    Log.d(TAG, "Reading optic");
                    characteristic = gatt.getService(OPTIC_SERVICE)
                            .getCharacteristic(OPTIC_CONFIG);
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "Sensors read.");
                    return;
            }
            gatt.readCharacteristic(characteristic);
        }

		// Notification enabling on sensors
        private void setNotifyNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "Set notify temperature");
                    characteristic = gatt.getService(TEMP_SERVICE)
                            .getCharacteristic(TEMP_DATA);
                    break;
                case 1:
                    Log.d(TAG, "Set notify humidity");
                    characteristic = gatt.getService(HUMIDITY_SERVICE)
                            .getCharacteristic(HUMIDITY_DATA);
                    break;
                case 2:
                    Log.d(TAG, "Set notify barometer");
                    characteristic = gatt.getService(BAROMETER_SERVICE)
                            .getCharacteristic(BAROMETER_DATA);
                    break;
                case 3:
                    Log.d(TAG, "Set notify optic");
                    characteristic = gatt.getService(OPTIC_SERVICE)
                            .getCharacteristic(OPTIC_DATA);
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "Notification enabled on all sensors.");
                    return;
            }

			//Enable notification on client device
            gatt.setCharacteristicNotification(characteristic, true);

			//Enabled notifications on server device
            BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }

        // If connection state changes, it reports to the terminal and meets the if statement
		@Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Connection State Change: " + status + " -> " + connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                gatt.readRemoteRssi();
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering Services..."));
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                mHandler.sendEmptyMessage(MSG_CLEAR);
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.disconnect();
                finish();
            }
        }

		// On services discovered the application moves to sensor enabling
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.d(TAG, "Service discovery status: " + status);
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors..."));
                reset();
                enableNextSensor(gatt);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //For each read, pass the data up to the UI thread to update the display
            if (TEMP_DATA.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_TEMP, characteristic));
            }
            if (HUMIDITY_DATA.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_HUMIDITY, characteristic));
            }
            if (BAROMETER_DATA.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_BAROMETER, characteristic));
            }
            if (OPTIC_DATA.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_OPTIC, characteristic));
            }
            //After reading the initial value, next we enable notifications
            setNotifyNextSensor(gatt);
        }

        //After writing the enable value, we read this value
		@Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            readNextSensor(gatt);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            gatt.readRemoteRssi();

            if (TEMP_DATA.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_TEMP, characteristic));
            }
            if (HUMIDITY_DATA.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_HUMIDITY, characteristic));
            }
            if (BAROMETER_DATA.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_BAROMETER, characteristic));
            }
            if (OPTIC_DATA.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_OPTIC, characteristic));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //Once notifications are enabled, we move to the next sensor and start over with enable
            advance();
            enableNextSensor(gatt);
        }

        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format("BluetoothGatt ReadRssi[%d]", rssi));
            }
        }

    };

    /*  We have a Handler to process event results on the main thread */

    private static final int MSG_TEMP = 101;
    private static final int MSG_OPTIC = 102;
    private static final int MSG_BAROMETER = 103;
    private static final int MSG_HUMIDITY = 104;

    private static final int MSG_PROGRESS = 201;
    private static final int MSG_DISMISS = 202;

    private static final int MSG_CLEAR = 301;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case MSG_TEMP:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining temperature value.");
                        Toast.makeText(getApplicationContext(),"Error obtaining temperature value. Returning to main screen.",Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    updateTemperatureValue(characteristic);
                    break;

                case MSG_HUMIDITY:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining humidity value.");
                        Toast.makeText(getApplicationContext(),"Error obtaining humidity value. Returning to main screen.",Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    updateHumidityValue(characteristic);
                    break;

                case MSG_OPTIC:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining optic value.");
                        Toast.makeText(getApplicationContext(),"Error obtaining optic value. Returning to main screen.",Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    updateOpticValue(characteristic);
                    break;

                case MSG_BAROMETER:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining barometer value.");
                        Toast.makeText(getApplicationContext(),"Error obtaining barometer value. Returning to main screen.",Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    updateBarometerValue(characteristic);
                    break;

                case MSG_PROGRESS:
                    mProgress.setMessage((String) msg.obj);
                    if (!mProgress.isShowing()) {
                        mProgress.show();
                    }
                    break;
                case MSG_DISMISS:
                    mProgress.hide();
                    break;
                case MSG_CLEAR:
                    break;
            }
        }
    };

    /* Methods to extract sensor data and update the UI */

    @SuppressLint("DefaultLocale")
    private void updateHumidityValue(BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();
        float f1 = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        mHumidity.setText(String.format("%.0f%%", f1));
    }

    @SuppressLint("DefaultLocale")
    private void updateTemperatureValue(BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();
        float f1 = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        mTemperature.setText(String.format("%.1f\u00B0C", f1));
    }

    @SuppressLint("DefaultLocale")
    private void updateOpticValue(BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();
        float f1 = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        mOptic.setText(String.format("%.1f\nlux", f1));
    }

    @SuppressLint("DefaultLocale")
    private void updateBarometerValue(BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();
        int f1 = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();
        mBarometer.setText(String.format("%d\nPa", f1));
    }


}