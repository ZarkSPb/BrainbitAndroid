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

//   private boolean _started;
//   private final ReentrantLock _searchLock = new ReentrantLock();
//   private DeviceEnumerator _deviceEnum;
//   private final List<DeviceInfo> _deviceInfoList = new ArrayList<>();

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

      BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
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


//      if (_searchLock.tryLock()) {
//         try {
//            if (_started) return;
//            if (_deviceEnum == null) {
//               _deviceEnum = new DeviceEnumerator(this, DeviceType.BrainbitAny);
//            }
//            _deviceInfoList.clear();
//            _deviceEnum.deviceListChanged.subscribe(new INotificationCallback() {
//               @Override
//               public void onNotify(Object sender, Object nParam) {
//                  updateDevInfo();
//               }
//            });
//            _started = true;
//            Log.d(LOG_TAG, "Search is started");
//            btSearch.setText(R.string.btn_stop_search_title);
//         } finally {
//            _searchLock.unlock();
//         }
//      }
   }

   public void stopSearch() {
      DevHolder.inst().stopSearch();
//      if (_searchLock.tryLock()) {
//         try {
//            if (!_started) return;
//            if (_deviceEnum != null) _deviceEnum.deviceListChanged.unsubscribe();
//
//            _started = false;
//            Log.d(LOG_TAG, "Search is stopped");
//            btSearch.setText(R.string.btn_start_search_title);
//         } finally {
//            _searchLock.unlock();
//         }
//      }
   }

//   private void updateDevInfo() {
//      if (_searchLock.tryLock()) {
//         boolean hasChanged = false;
//         try {
//            // Checking the change of the device info list
//            // This is a slow solution
//            List<DeviceInfo> deviceInfoList = _deviceEnum.devices();
//            if (deviceInfoList.size() != _deviceInfoList.size()) {
//               hasChanged = true;
//            } else {
//               boolean founded;
//               for (DeviceInfo it : _deviceInfoList) {
//                  founded = false;
//                  for (DeviceInfo itIn : deviceInfoList) {
//                     if (TextUtils.equals(itIn.address(), it.address())) {
//                        founded = true;
//                        break;
//                     }
//                  }
//                  if (!founded) {
//                     hasChanged = true;
//                     break;
//                  }
//               }
//            }
//            if (hasChanged) {
//               _deviceInfoList.clear();
//               if (deviceInfoList.size() > 0)
//                  _deviceInfoList.addAll(deviceInfoList);
//            }
//         } finally {
//            _searchLock.unlock();
//         }
//         if (hasChanged) {
//            // fire event
//            Log.d(LOG_TAG, "Device is finding");
//
//            runOnUiThread(new Runnable() {
//               @Override
//               public void run() {
//                  updateDevicesListView();
//               }
//            });
//         }
//      }
//   }




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
         map.put(DEV_NAME_KEY, it.name());
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