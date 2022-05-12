package com.zark.bbandroid.brainbitandroid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.neuromd.neurosdk.DeviceInfo;
import com.zark.bbandroid.brainbitandroid.utils.DeviceHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

   private final String LOG_TAG = "mylog";
   private final String DEV_NAME_KEY = "name";
   private final String DEV_ADDRESS_KEY = "address";
   private final int REQUEST_ENABLE_BT = 35;
   private final int REQUEST_PERMISSION_BT = 111;

   private BluetoothAdapter _btAdapter;
   private Button btEnableBt;
   private Button btRequestPerm;
   private Button btSearch;
   private ListView lvDevices;

   private BaseAdapter _lvDevicesAdapter;
   private final ArrayList<HashMap<String, String>> _deviceInfoList = new ArrayList<>();

   private boolean _isBtPermissionGranted = false;


   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      init();
   }

   private void init() {
      DevHolder.inst().init(this);

      BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
      _btAdapter = bluetoothManager.getAdapter();

      btEnableBt = findViewById(R.id.btEnableBt);
      btRequestPerm = findViewById(R.id.btRequestPerm);
      btSearch = findViewById(R.id.btSearch);
      btEnableBt.setOnClickListener(this);
      btRequestPerm.setOnClickListener(this);
      btSearch.setOnClickListener(this);
      initDevicesListView();

      DevHolder.inst().setDeviceEvent(new DeviceHelper.IDeviceEvent() {
         @Override
         public void searchStateChanged(final boolean searchState) {
            btSearch.post(new Runnable() {
               @Override
               public void run() {
                  btSearch.setText(searchState ? R.string.btn_stop_search_title :
                        R.string.btn_start_search_title);
               }
            });
         }

         @Override
         public void deviceListChanged() {
            updateDevicesListView();
         }
      });

   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == REQUEST_ENABLE_BT) {
         if (resultCode == RESULT_OK) {
         }
      }
   }

   @Override
   public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
      if (requestCode == REQUEST_PERMISSION_BT) {
         if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            _isBtPermissionGranted = true;
         } else {
            Toast.makeText(this, "No permission", Toast.LENGTH_SHORT).show();
         }
      } else {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      }
   }

   private void getBtPermission() {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
         ActivityCompat.requestPermissions(this,
               new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_BT);
      } else {
         _isBtPermissionGranted = true;
      }
   }

   private void enableBt() {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
   }

   @Override
   public void onClick(View view) {
      switch (view.getId()) {
         case R.id.btEnableBt:
            enableBt();
            break;
         case R.id.btRequestPerm:
            getBtPermission();
            break;
         case R.id.btSearch:
//            if (_started) {
            if (DevHolder.inst().isSearchStarted()) {
               stopSearch();
            } else {
               startSearch();
            }
            break;
      }
   }

   public void startSearch() {
      DevHolder.inst().startSearch();
      clearDevicesListView();
      DevHolder.inst().startSearch();
   }

   public void stopSearch() {
      DevHolder.inst().stopSearch();
   }

   private void initDevicesListView() {
      lvDevices = findViewById(R.id.lv_devices);
      _lvDevicesAdapter = new SimpleAdapter(this,
            _deviceInfoList,
            android.R.layout.simple_list_item_2,
            new String[]{DEV_NAME_KEY, DEV_ADDRESS_KEY},
            new int[]{android.R.id.text1, android.R.id.text2});
      lvDevices.setAdapter(_lvDevicesAdapter);
   }

   private void updateDevicesListView() {
      _deviceInfoList.clear();
      for (DeviceInfo it : DevHolder.inst().getDeviceInfoList()) {
         HashMap<String, String> map = new HashMap<>();
         map.put(DEV_NAME_KEY, it.name() + ": " +it.serialNumber());
         map.put(DEV_ADDRESS_KEY, it.address());
         _deviceInfoList.add(map);
      }
      _lvDevicesAdapter.notifyDataSetInvalidated();
   }

   private void clearDevicesListView() {
      if (!_deviceInfoList.isEmpty()) {
         _deviceInfoList.clear();
         _lvDevicesAdapter.notifyDataSetInvalidated();
      }
   }
}