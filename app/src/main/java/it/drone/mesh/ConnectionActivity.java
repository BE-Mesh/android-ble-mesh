package it.drone.mesh;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.NoSuchElementException;

import it.drone.mesh.models.User;
import it.drone.mesh.models.UserList;
import it.drone.mesh.tasks.ConnectBLETask;
import it.drone.mesh.utility.Constants;

import static it.drone.mesh.utility.Constants.EXTRAS_DEVICE_ADDRESS;
import static it.drone.mesh.utility.Constants.EXTRAS_DEVICE_NAME;

public class ConnectionActivity extends Activity {

    private final static String TAG = ConnectionActivity.class.getSimpleName();
    private final static int DO_UPDATE_TEXT = 0;
    private final static int DO_THAT = 1;
    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
    };
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private Handler mHandler; // TODO: 07/11/2018 che fa l'handler 
    private User user;

    private TextView outputText;
    private EditText inputText;
    private Button sendButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        outputText = findViewById(R.id.outputText);
        inputText = findViewById(R.id.inputText);
        sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(inputText.getText().toString());
                inputText.setText("");
            }
        });

        mDeviceName = getIntent().getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = getIntent().getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                final int what = msg.what;
                Log.d(TAG, "OUD: " + "handleMessage: SONO ENTRATO NELL'HANDLER");
                switch (what) {
                    case DO_UPDATE_TEXT:
                        doUpdate();
                        break;
                    case DO_THAT:
                        break;
                }
            }
        };

        //IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //registerReceiver(mReceiver, filter);

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "OUD: " + "Unable to initialize BluetoothManager.");
                return;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "OUD: " + "Unable to obtain a BluetoothAdapter.");
            return;
        }

        try {
            user = UserList.getUser(mDeviceName);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            Log.e(TAG, "Lista :" + UserList.printList());
        }


        //ConnectBLETask connectBLETask = new ConnectBLETask(UserList.getUser(mDeviceName), this);
        //connectBLETask.startClient();

        //outputText.setText(user.getBluetoothDevice().getName());


        //ConnectTask connectTask = new ConnectTask(device);
        //connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Invia il messaggio messagge al device selezionato nella schermata precedente
     *
     * @param message messaggio da inviare
     */
    private void sendMessage(String message) {
        Log.d(TAG, "OUD: " + "sendMessage: Inizio invio messaggio");
        //final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        final BluetoothGatt gatt = /*UserList.getUser(mDeviceName).getBluetoothGatt();*/ user.getBluetoothGatt();
        ConnectBLETask connectBLETask = null;

        while (!(BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT_SERVER)) || !(BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT))) {
            connectBLETask = new ConnectBLETask(user, this);
            connectBLETask.startClient();
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                Log.d(TAG, "OUD: " + "Andata male la wait");
            }
            Log.d(TAG, "OUD: " + "Restauro connessione");
            Log.d(TAG, "OUD: " + "StateServer connesso ? -> " + (BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT_SERVER)));
            Log.d(TAG, "OUD: " + "StateGatt connesso? -> " + (BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT)));
        }

        if (connectBLETask != null) {
            while (!connectBLETask.getServiceDiscovered()) {
                Log.d(TAG, "OUD: " + "Wait for services");
            }
        }

        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "OUD: " + "sendMessage: inizio ciclo");
            if (service.getUuid().toString().equals(Constants.Service_UUID.toString())) {
                Log.d(TAG, "OUD: " + "sendMessage: service.equals");
                if (service.getCharacteristics() != null) {
                    for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                        Log.d(TAG, "OUD:" + "Char: " + chars.toString());
                        if (chars.getUuid().toString().equals(Constants.Characteristic_UUID.toString())) {
                            int i = 0;
                            while (i < 3) {
                                chars.setValue(message + i);
                                gatt.beginReliableWrite();
                                boolean res = gatt.writeCharacteristic(chars);
                                gatt.executeReliableWrite();
                                Log.d(TAG, "OUD: " + message + i);
                                Log.d(TAG, "OUD: " + "Inviato? -> " + res);
                                try {
                                    Thread.sleep(500);
                                } catch (Exception e) {
                                    Log.d(TAG, "OUD: " + "Andata male la wait");
                                }
                                i++;
                            }

                        }
                    }
                }
            }

        }
        Log.d(TAG, "OUD: " + "sendMessage: end ");
    }

    /*
     quando s6 scrive (log di s6)
     OUD: Service UUID                 : 00001814-0000-1000-8000-00805f9b34fb
     OUD: Constants Service UUID       : 00001814-0000-1000-8000-00805f9b34fb
     OUD: Constants Service UUID client: 00002a14-0000-1000-8000-00805f9b34fb
     OUD: Chars UUID    : 00000000-0000-1000-8000-00805f9b34fb // da dove dovrebbe uscire l'indirizzo di default?
     OUD: Constants UUID: 1111b81d-0000-1000-8000-00805f9b34fb

     quando J5 scrive (log di J5)
     OUD: Service UUID                 : 00001814-0000-1000-8000-00805f9b34fb
     OUD: Constants Service UUID       : 00001814-0000-1000-8000-00805f9b34fb
     OUD: Constants Service UUID client: 00002a14-0000-1000-8000-00805f9b34fb
     OUD:Chars UUID    : 1111b81d-0000-1000-8000-00805f9b34fb
     OUD:Constants UUID: 1111b81d-0000-1000-8000-00805f9b34fb

     */

    /**
     * Aggiorna l'output con il messaggio nuovo
     * <p>
     * NB questo metodo viene invocato se e solo se c'è stata un'effettiva scrittura, quindi potrebbe non comparire subito
     *
     * @param message messaggio da aggiungere
     */
    private void addOutputMessage(String message) {
        outputText.setText(outputText.getText().toString().concat("\n").concat(message));
    }

    private void doUpdate() {
        outputText.setText(outputText.getText().toString().concat("\n").concat(String.valueOf(System.currentTimeMillis())));
    }

}
