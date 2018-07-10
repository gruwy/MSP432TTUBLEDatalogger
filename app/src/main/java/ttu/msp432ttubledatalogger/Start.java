package ttu.msp432ttubledatalogger;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Start extends AppCompatActivity {

    // Declaring BluetoothAdapter in order to ask to turn it on.
    private BluetoothAdapter mBluetoothAdapter;

    // Boolean for double "back" pressing functionality
    private static boolean userPressedBackAgain = false;

    // onCreate callback, initializes theme back from the TUT logo to activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Setting back the original theme
        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    // This callback is executing methods after activity creation
    @Override
    protected void onResume() {
        initializeBluetoothAdapter();
        checkBluetoothSupport();
        askCoarsePermission();
        askBluetoothPermission();
        initializeButton();
        super.onResume();
    }

    // If the user presses "Back" button, then this method handles it's activity
    @Override
    public void onBackPressed() {
        if (!userPressedBackAgain) {
                Toast.makeText(this, "Press BACK again to exit the application", Toast.LENGTH_SHORT).show();
                userPressedBackAgain = true;
        } else {
                 finish();
                 System.exit(0);
        }

        new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
                userPressedBackAgain = false;
            }
        }.start();
    }

    @Override
    public void finish() {
        super.finish();
    }

    // Initializing button functionality
    private void initializeButton(){

        // Initialize ScanButton functionality
        Button scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openScanBle();
            }
            private void openScanBle(){
                Intent intent;
                intent = new Intent(getApplicationContext(), Scan.class);
                startActivity(intent);
            }
        });
    }

    // Ensures location is available on the device and permission is granted.
    // If not, displays a dialog requesting user permission to access coarse location.
    private void askCoarsePermission() {
        if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            int REQUEST_CODE_COARSE_PERMISSION = 1;
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_COARSE_PERMISSION);
        }
    }

    // Ensures Bluetooth is available on the device and it is enabled. If not,
    // displays a dialog requesting user permission to enable Bluetooth.
    private void askBluetoothPermission() {
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                int REQUEST_CODE_BLUETOOTH_PERMISSION = 2;
                startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH_PERMISSION);
            }
    }

    // Bluetooth Low Energy availability check
        private void checkBluetoothSupport() {
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    // Initializing Bluetooth adapter
        private void initializeBluetoothAdapter(){
            getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

}
