package com.pxs.bleperipheral;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class FirstActivity extends AppCompatActivity {

    public static final String SERVER_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String RX_UUID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String TX_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String DESC_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    @InjectView(R.id.button)
    Button button;
    @InjectView(R.id.info)
    TextView info;
    @InjectView(R.id.link_state)
    TextView linkState;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothDevice device;
    private Handler handler = new Handler();
    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                FirstActivity.this.device = device;
            }
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            descriptor.setValue(value);
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
        }

        @Override
        public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(device, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            super.onPhyRead(device, txPhy, rxPhy, status);
        }
    };
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    button.setText("正在广播");
                }
            });
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    button.setText("开始广播");
                    info.setText("广播开启失败");
                }
            });
            super.onStartFailure(errorCode);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        ButterKnife.inject(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        bleInit();
        bleAdvInit();
        bleServerInit();
    }

    private void bleInit() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showToast("该设备不支持蓝牙低功耗通讯");
            this.finish();
            return;
        }

        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            showToast("该设备不支持蓝牙低功耗通讯");
            this.finish();
            return;
        }

        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (bluetoothLeAdvertiser == null) {
            showToast("该设备不支持蓝牙低功耗从设备通讯");
            this.finish();
            return;
        }

    }

    private void bleAdvInit() {
        AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();
        settingBuilder.setConnectable(true);
        settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingBuilder.setTimeout(0); //我填过别的，但是不能广播。后来我就坚定的0了
        settingBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        AdvertiseSettings settings = settingBuilder.build();
        //广播参数
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        if (!bluetoothAdapter.setName("PanPan"))
            showToast("蓝牙名称设置失败");
        dataBuilder.setIncludeDeviceName(true);
        dataBuilder.setIncludeTxPowerLevel(true);
        dataBuilder.addServiceUuid(ParcelUuid.fromString("00001234-0000-1000-8000-00805f9b34fb")); //可自定义UUID，看看官方有没有定义哦
        AdvertiseData data = dataBuilder.build();
        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
    }

    BluetoothGattCharacteristic characteristicRead;

    private void bleServerInit() {
        bluetoothGattServer = bluetoothManager.openGattServer(this,
                bluetoothGattServerCallback);
        BluetoothGattService service = new BluetoothGattService(UUID.fromString(SERVER_UUID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //特征值读写设置
        characteristicRead = new BluetoothGattCharacteristic(UUID.fromString(TX_UUID),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString(DESC_UUID), BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        characteristicRead.setValue(new byte[]{0x00, 0x01, 0x02});
        characteristicRead.addDescriptor(descriptor);
        //特征值读写设置
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID.fromString(RX_UUID),
                BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristicRead);
        service.addCharacteristic(characteristicWrite);
        bluetoothGattServer.addService(service);
    }

    public void advClick(View v) {
//        if (button.getText().toString().contains("开始广播")) {
//            button.setText("正在开启广播");
//            bleAdvInit();
//        }
//        if (button.getText().toString().contains("正在广播")) {
//            button.setText("开始广播");
//            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
//        }
        characteristicRead.setValue(new byte[]{0x12, 0x34, 0x56});
        bluetoothGattServer.notifyCharacteristicChanged(device,characteristicRead,false);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        super.onDestroy();
    }
}

