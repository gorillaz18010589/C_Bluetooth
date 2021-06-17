package com.example.c_bluetooth;
//https://cmnocsexperience.blogspot.com/2019/01/android.html
//目的:傳統藍芽二代,但連線Mac可以,但Windows不行可能是uuid的問題
//手機NFC:並不是每隻手機都支援,短距離高頻的支援
//rfid:無線射頻辨識 距離有限制的,東西一拿出倉庫就被偵測出或了
//beacon:近距離路過某的店家,就收到訊息,空間三個點
//blueTooth:無限超高頻技術,藍芽耳機就要配對,兩邊發出一個呼應,配對完後會在手機傳送資料
/*
* 藍芽第二代 -> 距離50~70公尺
* 藍芽第三代 >速度變快8倍,一般開發在車載系統上
* 藍芽第四代 -> 最重要是節能省點,如果要外銷到歐美國家一定要通過環保檢定,這代就會用在物鏈網,我只要有藍芽你只要有手機就好,不用WIFI買SIN卡直接連行動網路
* 藍芽第五代 -> 距離到300公尺,傳送快兩倍
* 手機4.3以上才有支援藍芽
* 手機還要看有沒有藍芽
* 搜尋不是一直搜尋,當你要連接時記得要把搜尋做取消
*搜尋時會註冊廣播,什麼時候搜尋到不知道,如果搜尋到去做處理
 * */

/*連線過程
* 1.準備BluetoothSeveerSocket,會拿到這個裝置的uuid
* 2.accept去探聽遠端是否願意與你配對
* 3.點下去進行配對
* */

/* 藍芽2
 * 1.權限設定
 * 2.透過BlueToothAdapter去設定藍芽,BlueToothAdapter是自己這方
 * 3如果藍芽沒有啟動就去intent啟動藍芽
 * 4.查詢曾經配對過得對方藍芽
 * 5.準備一個廣播監聽是否有搜尋到藍芽
 * 6.在有允許藍芽啟動的地方註冊intentFilter -> ACTION_FOUND.送出找尋藍芽的動作
 * 7.startDiscovery 搜尋藍芽的intent
 * 8.cancelDiscovery :停止搜尋藍芽
 * 9.ACTION_FOUND ->廣播接受到藍芽有被找到取得附近裝置的藍芽資訊
 * 10.收集資料用LinkedList,在broadcastReceiver ->ACTION_FOUND取得資料實作處存
 * 11.用ListView呈現資料
 * 12.adapter設定
 * 13.進行連線配對
 * 14.要取得uuid是一個工業標準,你是什麼裝置id就是多少 ->此處有uuidhttps://cmnocsexperience.blogspot.com/2019/01/android.html此處有uuid
 * */
//RX232

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter; //BlueToothAdapter是自己這方
    private LinkedList<HashMap<String,Object>> devices = new LinkedList<>();//廣播會取得多筆被搜尋到的藍芽裝置資料

    private ListView listDevices;
    private SimpleAdapter adapter;
    private String[] from = {"name","mac"};
    private int [] to = {R.id.item_name,R.id.item_mac};

        //可連上mac的UUID
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//    private final UUID MY_UUID = UUID.fromString("00001000-0000-1000-8000-00805F9B34FB");



    //5.準備一個廣播監聽是否有搜尋到藍芽
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            //9.ACTION_FOUND ->藍芽有被找到取得附近裝置的藍芽資訊
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address

                    //10.資料儲存,一筆一筆被搜尋到的藍芽裝置儲存
                    HashMap<String, Object> deviceData = new HashMap<>();
                    deviceData.put("name", deviceName);
                    deviceData.put("mac", deviceHardwareAddress);
                    deviceData.put("device", device);


                    //判斷資料是否重複
                    boolean isRepeat = false;
                    for (HashMap<String, Object> dd : devices) {
                        if (dd.get("device") == device) {
                            isRepeat = true;
                            break;
                        }
                    }
                    if (!isRepeat) {
                        devices.add(deviceData);
                        //12.有新增資料通知條便器更新
                        adapter.notifyDataSetChanged();
                    }

                    Log.d("hank", "broadcastReceiver() ACTION_FOUND-> deviceName:" + deviceName + "/deviceHardwareAddress:" + deviceHardwareAddress);
                } catch (Exception e) {
                    Log.d("hank", "broadcastReceiver() ACTION_FOUND-> e:" + e.toString());

                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //1.取得ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //沒有權限去要
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    123);
        } else {
            init();
        }

    }

    private void init() {
        //12.adapter設定
        listDevices = findViewById(R.id.listDevices);
        adapter = new SimpleAdapter(this,devices,R.layout.item_device,from,to);
        listDevices.setAdapter(adapter);

        //14.點下去要連線的藍芽item
        listDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                doPair(position);
            }
        });


        //2.透過BlueToothAdapter去設定藍芽,BlueToothAdapter是自己這方,runApp的這方
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            //3.如果藍芽沒有啟動就去intent啟動藍芽,如果允許啟用藍芽,狀態列藍芽符號會出現
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);//啟動藍芽的intent
            startActivityForResult(enableBtIntent, 123);
            Log.d("hank", "藍芽沒有啟動去啟動..");
        } else {
            //如果已經啟動藍芽就跑這
            regReceiver();
            Log.d("hank", "藍芽有啟動..");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //3.在onResume時,如果藍芽沒有啟動就去intent啟動藍芽,如果允許啟用藍芽,狀態列藍芽符號會出現

    }

    //6.在有允許藍芽啟動的地方註冊intentFilter -> ACTION_FOUND.送出找尋藍芽的動作
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == RESULT_OK) {
            //有允許啟動藍芽
            regReceiver();
            Log.d("hank","onActivityResult -> regReceiver()");
        }else if(requestCode == 111){ //被人搜尋時發出
            Log.d("hank","onActivityResult -> ACTION_REQUEST_DISCOVERABLE");
        }
    }

    //6.尋找藍芽的Intent
    private void regReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);//找尋附近藍芽的intent
        registerReceiver(broadcastReceiver, filter);
    }




    //權限有允許的話初始化
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
        Log.d("hank","onRequestPermissionsResult -> init()");
    }

    //解除廣播註冊
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        Log.d("hank","onDestroy -> unregisterReceiver");
    }

    //4.查詢曾經配對過得對方藍芽
    public void test1(View view) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices(); //獲取曾經綁過對方的藍芽

        if (pairedDevices.size() > 0) {
            //有綁定過藍芽裝置有綁定的資料
            for (BluetoothDevice bluetoothDevice : pairedDevices) {
                String deviceName = bluetoothDevice.getName();
                String deviceHardwareAddress = bluetoothDevice.getAddress(); //MAC address

                Log.d("hank", "test1 -> 取得曾經綁定過得對方藍芽資訊() deviceName:" + deviceName + "/deviceHardwareAddress:" + deviceHardwareAddress);
            }
        }
    }

    //搜尋藍芽
    public void test2(View view) {
        if (!bluetoothAdapter.isDiscovering()) {//如果沒在搜尋時,才去做搜尋動作
            devices.clear();//要搜尋時先把資料清掉
            bluetoothAdapter.startDiscovery();
            Log.d("hank", "test2 -> 搜尋藍芽");

        }
    }

    //停止搜尋藍芽
    public void test3(View view) {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            Log.d("hank", "test3 -> 停止搜尋藍芽");
        }
    }

    //被人搜尋
    public void test4(View view) {
        Log.d("hank", "test4 -> 被人搜尋藍芽");
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); //300是5分鐘
        startActivityForResult(discoverableIntent,111);
    }

    //13.進行連線配對,取得deviec
    private  void doPair(int i){
        //i -> mac
        BluetoothDevice device = (BluetoothDevice) devices.get(i).get("device");//取得點到item位置的資料裡面的device強轉成 BluetoothDevice
        new ConnectThread(device).start();
    }



    //13.客戶連線端
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            Log.d("hank","ConnectThread-> device:" + mmDevice.getName());
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                //發現mac可以連但windows不行,也許是uuid對方不收
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID); //跟這個device的藍芽者創建socket,1.uuid:看文件
            } catch (IOException e) {
                Log.e("hank", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
                bluetoothAdapter.cancelDiscovery();

            Log.d("hank","run");
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("hank", "run -> mmSocket.connect()成功;");
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("hank", "run -> mmSocket.close(); IOException:" + connectException.toString());
                } catch (IOException closeException) {
                    Log.e("hank", "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
//            manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("hank", "Could not close the client socket", e);
            }
        }
    }


    //我要被配對的那一方
//    private class AcceptThread extends Thread {
//        private final BluetoothServerSocket mmServerSocket;
//
//        public AcceptThread() {
//            // Use a temporary object that is later assigned to mmServerSocket
//            // because mmServerSocket is final.
//            BluetoothServerSocket tmp = null;
//            try {
//                // MY_UUID is the app's UUID string, also used by the client code.
//                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("", "");//1.名稱,2.uuid
//            } catch (IOException e) {
//                Log.e("hank", "Socket's listen() method failed", e);
//            }
//            mmServerSocket = tmp;
//        }
//
//        public void run() {
//            BluetoothSocket socket = null;
//            //持續去接收直到成功為止
//            while (true) {
//                try {
//                    socket = mmServerSocket.accept();
//                } catch (IOException e) {
//                    Log.e("hank", "Socket's accept() method failed", e);
//                    break;
//                }
//
//                //成功了
//                if (socket != null) {
//                    // A connection was accepted. Perform work associated with
//                    // the connection in a separate thread.
////                    manageMyConnectedSocket(socket);
//                    try {
//                        mmServerSocket.close();
//                        break;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//
//                    }finally {
//                        //反正大家都要脫離寫在finally
//                        break;
//                    }
//                }
//            }
//        }
//
//        // Closes the connect socket and causes the thread to finish.
//        public void cancel() {
//            try {
//                mmServerSocket.close();
//            } catch (IOException e) {
//                Log.e("hank", "Could not close the connect socket", e);
//            }
//        }
//    }
}