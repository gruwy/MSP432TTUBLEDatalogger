package ttu.msp432ttubledatalogger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;

public class Scan extends AppCompatActivity {

    // Declaring required items for scanner functionality
    private ListView listView;
    private ArrayList<String> mDeviceList = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter;
    private ProgressBar progressBar;

    // onCreate callback, sets content view to appropriate *.xml file and initializes the listView and progressBar
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        listView = findViewById(R.id.listView);
        progressBar = findViewById(R.id.progressBar);
    }

    // onStart callback, after onCreate callback reaction this callback asks checks the Bluetooth working state, asks for permission to turn on if it's off and gets Bluetooth adapter with service.
    @Override
    protected void onStart() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        askBluetoothPermission();
        progressBar.setVisibility(View.VISIBLE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        getSystemService(Context.BLUETOOTH_SERVICE);
        super.onStart();
    }

    // onStop callback stops the discovery, if the app is closed
    @Override
    protected void onStop() {
        mBluetoothAdapter.cancelDiscovery();
        super.onStop();
    }

    // onResume callback starts the discovery after onStart callback
    @Override
    protected void onResume() {
        mBluetoothAdapter.startDiscovery();
        super.onResume();
    }

    // onDestroy callback unregisters receiver, if the app gets closed
    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    // BroadcastReceiver is configured for scanning with callbacks
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                progressBar.setVisibility(View.INVISIBLE);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceList.add(device.getName() + "\n" + device.getAddress());
                Log.i("BT", device.getName() + "\n" + device.getAddress());
                listView.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, mDeviceList));
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Toast.makeText(getApplicationContext(), "Device: " + device.getName() + "\n" + "Address: " + device.getAddress(), Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(getApplicationContext(), SensorOutput.class);
                        intent.putExtra("Bluetooth_Device", device);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        }
    };

    // Ensures Bluetooth is available on the device and it is enabled. If not,
    // displays a dialog requesting user permission to enable Bluetooth.
    private void askBluetoothPermission() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_CODE_BLUETOOTH_PERMISSION = 2;
            startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH_PERMISSION);
        }
    }

    // Design
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.scan_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.Refresh:
                mBluetoothAdapter.cancelDiscovery();
                mDeviceList.clear();
                listView.setAdapter(null);
                progressBar.setVisibility(View.VISIBLE);
                mBluetoothAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(),"Refreshing...",Toast.LENGTH_SHORT).show();
                return true;
            default:

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
    }
}