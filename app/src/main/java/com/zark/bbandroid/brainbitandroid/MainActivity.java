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
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.neuromd.neurosdk.DeviceEnumerator;
import com.neuromd.neurosdk.DeviceInfo;
import com.neuromd.neurosdk.DeviceType;
import com.neuromd.neurosdk.INotificationCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

   private final String LOG_TAG = "mylog";
   private final int REQUEST_ENABLE_BT = 35;
   private final int REQUEST_PERMISSION_BT = 111;

   private boolean _started = false;
   private final ReentrantLock _searchLock = new ReentrantLock();
   private DeviceEnumerator _deviceEnum;
   private final List<DeviceInfo> _deviceInfoList = new ArrayList<>();
   private IDeviceEvent _devEvent;

   private BluetoothAdapter _btAdapter;
   private Button _btOnOff;
   private Button _btPermission;
   private Button _btSearch;
   private TextView _tvBtStatus;
   private boolean _isBtPermissionGranted = false;


   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      init();
   }

   private void init() {
      BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
      _btAdapter = bluetoothManager.getAdapter();

      _btOnOff = findViewById(R.id.btOnOff);
      _btPermission = findViewById(R.id.btPermission);
      _btSearch = findViewById(R.id.btSearch);
      _tvBtStatus = findViewById(R.id.tvBtStatus);
      _btOnOff.setOnClickListener(this);
      _btPermission.setOnClickListener(this);
      _btSearch.setOnClickListener(this);
      setBtnOnOffText();
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == REQUEST_ENABLE_BT) {
         if (resultCode == RESULT_OK) {
            setBtnOnOffText();
         }
      }
   }

   @Override
   public void onClick(View view) {
      switch (view.getId()) {
         case R.id.btOnOff:
            if (!_btAdapter.isEnabled()) {
               enableBt();
            } else {
               _btAdapter.disable();
               _btOnOff.setText("Turn on bluetooth");
               _tvBtStatus.setText("Bluetooth adapter is OFF");
            }
            break;
         case R.id.btPermission:
            getBtPermission();
            break;
         case R.id.btSearch:
            if (_started) {
               stopSearch();
            } else {
               startSearch();
            }
            break;
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

   private void setBtnOnOffText() {
      if (_btAdapter != null) {
         if (_btAdapter.isEnabled()) {
            _btOnOff.setText("Turn off bluetooth");
            _tvBtStatus.setText("Bluetooth adapter is ON");
         } else {
            _btOnOff.setText("Turn on bluetooth");
            _tvBtStatus.setText("Bluetooth adapter is OFF");
         }
      }
   }

   private void setBtnSearchText() {
      if (_started) {
         _btSearch.setText("Stop search");
      } else {
         _btSearch.setText("Start search");
      }
   }

   private void enableBt() {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
   }

   public void startSearch() {
      if (_searchLock.tryLock()) {
         try {
            if (_started) return;
            if (_deviceEnum == null)
               _deviceEnum = new DeviceEnumerator(this, DeviceType.Brainbit);
            _deviceInfoList.clear();
            _deviceEnum.deviceListChanged.subscribe(new INotificationCallback() {
               @Override
               public void onNotify(Object sender, Object nParam) {
                  updateDevInfo();
               }
            });
            _started = true;
         } finally {
            _searchLock.unlock();
         }
         setBtnSearchText();
      }
   }

   public void stopSearch() {
         if (_searchLock.tryLock()) {
            try {
               if (!_started)
                  return;
               _started = false;
               if (_deviceEnum != null)
                  _deviceEnum.deviceListChanged.unsubscribe();
            } finally {
               _searchLock.unlock();
            }
            invokeSearchState(false);
         }
      setBtnSearchText();
   }

   private void invokeSearchState(boolean searchState) {
      final IDeviceEvent ev = _devEvent;
      if(ev != null)
         ev.searchStateChanged(searchState);
   }

   private void invokeDeviceListChanged() {
      final IDeviceEvent ev = _devEvent;
      if (ev != null) {
         this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
               ev.deviceListChanged();
            }
         });
      }
   }

   private void updateDevInfo() {
      if (_searchLock.tryLock()) {
         boolean hasChanged = false;
         try {
            // Checking the change of the device info list
            // This is a slow solution
            List<DeviceInfo> deviceInfoList = _deviceEnum.devices();
            if (deviceInfoList.size() != _deviceInfoList.size()) {
               hasChanged = true;
            } else {
               boolean founded;
               for (DeviceInfo it : _deviceInfoList) {
                  founded = false;
                  for (DeviceInfo itIn : deviceInfoList) {
                     if (TextUtils.equals(itIn.address(), it.address())) {
                        founded = true;
                        break;
                     }
                  }
                  if (!founded) {
                     hasChanged = true;
                     break;
                  }
               }
            }
            if (hasChanged) {
               _deviceInfoList.clear();
               if (deviceInfoList.size() > 0)
                  _deviceInfoList.addAll(deviceInfoList);
            }
         } finally {
            _searchLock.unlock();
         }
         if (hasChanged) {
            // fire event
            invokeDeviceListChanged();
         }
      }
   }

   public interface IDeviceEvent {
      void searchStateChanged(boolean searchState);
      void deviceListChanged();
   }
}