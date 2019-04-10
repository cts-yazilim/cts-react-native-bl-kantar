package com.blkantar;

import android.util.Log;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.os.IBinder;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.ComponentName;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class RNAndroidBLKantarModule extends ReactContextBaseJavaModule {

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
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private List<BluetoothGattService> ServiceList;

    @ReactMethod
    public void Init() {
        mDeviceAddress = "00:A0:50:55:03:66";
        Intent gattServiceIntent = new Intent(reactContext, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @ReactMethod
    public void ConnectDevice(String DeviceAdress) {
        mDeviceAddress = DeviceAdress;
        mBluetoothLeService.connect(mDeviceAddress);
    }

    private void sendNotification() {
        for (BluetoothGattService gattService : ServiceList) {

            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                if (gattCharacteristic.getUuid().toString().equalsIgnoreCase("E2B976D6-EB3D-4225-95C8-5CD621D36265")) {
                    final int charaProp = gattCharacteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = gattCharacteristic;
                        mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
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
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                ServiceList = mBluetoothLeService.getSupportedGattServices();
                sendNotification();

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void updateConnectionState(final int durum) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
        	Log.d("Receive",data);
            mDataField.setText(data);
            
        }
    }


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            Log.d("OK", "Connected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("OK", "Disconnected");
            mBluetoothLeService = null;
        }
    };

    private void sendEvent(String eventName, WritableMap map) {
        try {

            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, map);
        } catch (Exception e) {
            Log.d("ReactNativeJS", "Exception in sendEvent in ReferrerBroadcastReceiver is:" + e.toString());
        }

    }


}