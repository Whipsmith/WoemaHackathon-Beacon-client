package com.example.WoemaBeacon;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class Home extends Activity {

    private Thread wsThread;

    public static volatile boolean threadStop = false;

    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;

//    1 second post interval
    private int postInterval = 1000;

    private boolean scan;
    private volatile boolean post;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ButterKnife.inject(this);

        Runnable wsRunnable = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (threadStop) {
                        break;
                    }

                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            post = !post;
                            Log.i("post flip", String.valueOf(post));
                        }
                    };
                    runOnUiThread(r);

                    try {
                        Thread.sleep(postInterval);
                    } catch (InterruptedException e) {
                        post = false;
                    }
                }
            }


        };

        wsThread = new Thread(wsRunnable);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


    }

    private void startPostTimer(boolean start) {
        if (start  && wsThread.isAlive()) {
            threadStop = false;
            wsThread.start();
        } else {
            threadStop = true;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @OnClick(R.id.startbutton)
    public void startClick() {
        if (!scan) {
            scan = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            startPostTimer(true);
        }

    }



    @OnClick(R.id.stopbutton)
    public void stopClick() {
        if (scan) {
            scan = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            startPostTimer(false);
        }

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("LeScanCallBack",
                                    String.format("Device name:[%s], RSSI:[%d], Address[%s], DeviceType[%s], Device.toString:[%s]"
                                            , device.getName()
                                            , rssi,device.getAddress()
                                            , device.getBluetoothClass().toString()
                                            , device.toString())
                            );
                        }
                    });
                }
            };
}
