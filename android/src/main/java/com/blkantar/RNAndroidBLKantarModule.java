package com.blkantar;

import java.util.List;
import android.util.Log;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.os.IBinder;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class RNAndroidBLKantarModule extends ReactContextBaseJavaModule implements ServiceConnection {

    public static ReactApplicationContext reactContext;

    public RNAndroidBLKantarModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNAndroidBLKantar";
    }

    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private boolean hasService = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private List<BluetoothGattService> ServiceList;

    @ReactMethod
    public void Init(String DeviceAdress) {
        Log.d("INIT", "OK");
        // mDeviceAddress = "00:A0:50:55:03:66";
        reactContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        mDeviceAddress = DeviceAdress;
        Intent gattServiceIntent = new Intent(reactContext, BluetoothLeService.class);
        reactContext.bindService(gattServiceIntent, this, Context.BIND_AUTO_CREATE);
        Log.d("INIT", "FIN");
    }

    @ReactMethod
    public void ConnectDevice(String DeviceAdress) {

        // if (mConnected) {
        // DisconnectDevice();
        // }
        if(mBluetoothLeService != null)
        {

        mBluetoothLeService.disconnect();
        mDeviceAddress = DeviceAdress;
        mBluetoothLeService.connect(mDeviceAddress);
    }
   
    }

    @ReactMethod
    public void DisconnectDevice() {
        try {
            // mBluetoothLeService.disconnect();
            // mDeviceAddress = DeviceAdress;
            mBluetoothLeService.disconnect();
            // Add new
            mBluetoothLeService.close();
            reactContext.unbindService(this);
        } catch (Exception ex) {
        }
    }

    private void sendNotification() {
        Log.d("SERVICES", "SENDNOTFY");
        if (ServiceList != null) {
            for (BluetoothGattService gattService : ServiceList) {
                Log.d("SERVICES", "SENDNOTFY");
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString()
                            .equalsIgnoreCase("E2B976D6-EB3D-4225-95C8-5CD621D36265")) {
                        final int charaProp = gattCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = gattCharacteristic;
                            mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                        }
                    }
                }
            }
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                hasService = false;

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d("SERVICES", "SERVICES");
                if (mBluetoothLeService != null) {
                    ServiceList = mBluetoothLeService.getSupportedGattServices();
                    sendNotification();
                }
                // WritableMap map = new WritableNativeMap();
                // map.putString("connection_state", "true");
                // sendEvent("KantarConnectionState", map);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // WritableMap map = new WritableNativeMap();
                // map.putString("kantar_data",
                // intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                // sendEvent("KantarData", map);

            }
        }
    };

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {

        Log.d("INIT", "Connected");
        mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
        if (!mBluetoothLeService.initialize()) {
            Log.e("INIT", "Unable to initialize Bluetooth");
        }
        mBluetoothLeService.connect(mDeviceAddress);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d("OK", "Disconnected");
        mBluetoothLeService = null;
    }

    // private final ServiceConnection mServiceConnection = new ServiceConnection()
    // {

    // @Override
    // public void onServiceConnected(ComponentName componentName, IBinder service)
    // {

    // Log.d("OK", "Connected");
    // mBluetoothLeService = ((BluetoothLeService.LocalBinder)
    // service).getService();
    // if (!mBluetoothLeService.initialize()) {
    // Log.e("TEST", "Unable to initialize Bluetooth");
    // }
    // mBluetoothLeService.connect(mDeviceAddress);
    // }

    // @Override
    // public void onServiceDisconnected(ComponentName componentName) {
    // Log.d("OK", "Disconnected");
    // mBluetoothLeService = null;
    // }
    // };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}