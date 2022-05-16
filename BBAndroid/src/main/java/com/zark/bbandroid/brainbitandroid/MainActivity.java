package com.zark.bbandroid.brainbitandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.neuromd.neurosdk.DeviceInfo;
import com.zark.bbandroid.utils.CommonHelper;
import com.zark.bbandroid.utils.DeviceHelper;
import com.zark.bbandroid.utils.SensorHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

   private final String TAG = "[MainActivity]";
   private final String DEV_NAME_KEY = "name";
   private final String DEV_ADDRESS_KEY = "address";

   private Button btSearch;
   private Button btResistance;
   private Button btSignal;
   private ListView lvDevices;

   private TextView tvDevState;
   private TextView tvDevBatteryPower;

   private BaseAdapter _lvDevicesAdapter;
   private final ArrayList<HashMap<String, String>> _deviceInfoList = new ArrayList<>();

   private final ExecutorService _es = Executors.newFixedThreadPool(1);

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      init();
   }

   private void init() {
      tvDevState = findViewById(R.id.txt_dev_state);
      tvDevBatteryPower = findViewById(R.id.txt_dev_battery_power);
      DevHolder.inst().init(this);
      DevHolder.inst().addCallback(new DevHolder.IDeviceHolderCallback() {
         @Override
         public void batteryChanged(int val) {
            tvDevBatteryPower.setText(getString(R.string.dev_power_prc, val));
         }

         @Override
         public void deviceState(boolean state) {
            if (state) {
               tvDevState.setText(R.string.dev_state_connected);
            } else {
               tvDevState.setText(R.string.dev_state_disconnected);
               tvDevBatteryPower.setText(R.string.dev_power_empty);
            }
         }
      });

      btSearch = findViewById(R.id.bt_search);
      btSearch.setOnClickListener(this);
      btResistance = findViewById(R.id.bt_resistance);
      btResistance.setOnClickListener(this);
      btSignal = findViewById(R.id.bt_signal);
      btSignal.setOnClickListener(this);

      initDevicesListView();
      DevHolder.inst().setDeviceEvent(new DeviceHelper.IDeviceEvent() {
         @Override
         public void searchStateChanged(final boolean searchState) {
            btSearch.post(() -> btSearch.setText(searchState ? R.string.btn_stop_search_title : R.string.btn_start_search_title));
         }

         @Override
         public void deviceListChanged() {
            updateDevicesListView();
         }
      });
   }

   private void initDevicesListView() {
      lvDevices = findViewById(R.id.lv_devices);
      _lvDevicesAdapter = new SimpleAdapter(this,
            _deviceInfoList,
            android.R.layout.simple_list_item_2,
            new String[]{DEV_NAME_KEY, DEV_ADDRESS_KEY},
            new int[]{android.R.id.text1, android.R.id.text2});
      lvDevices.setAdapter(_lvDevicesAdapter);
      lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
         @Override
         @SuppressWarnings("unchecked")
         public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Map<String, String> item = (Map<String, String>) _lvDevicesAdapter.getItem(position);
            if (item != null) {
               stopSearch();
               connectToDevice(item.get(DEV_ADDRESS_KEY));
            }
         }
      });
   }

   private void updateDevicesListView() {
      _deviceInfoList.clear();
      for (DeviceInfo it : DevHolder.inst().getDeviceInfoList()) {
         HashMap<String, String> map = new HashMap<>();
         map.put(DEV_NAME_KEY, it.name() + ": " + it.serialNumber());
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

   private void startSearch() {
      DevHolder.inst().disconnect();
      clearDevicesListView();
      DevHolder.inst().enabledSensor(new SensorHelper.ISensorEvent() {
         @Override
         public void ready() {
            DevHolder.inst().startSearch();
         }

         @Override
         public void cancel(String message, Exception error) {
            CommonHelper.showMessage(MainActivity.this, message);
         }
      });
   }

   public void stopSearch() {
      DevHolder.inst().stopSearch();
   }


   private void connectToDevice(final String address) {
      _es.execute(() -> {
         try {
            DevHolder.inst().connect(address);
            CommonHelper.showMessage(MainActivity.this, R.string.device_search_connected);
         } catch (Exception ex) {
            Log.d(TAG, "Failed connect to device", ex);
            CommonHelper.showMessage(MainActivity.this, R.string.device_search_connection_failed);
         }
      });
   }

   @Override
   public void onClick(View view) {
      switch (view.getId()) {
         case R.id.bt_search:
            if (DevHolder.inst().isSearchStarted()) {
               stopSearch();
            } else {
               startSearch();
            }
            break;
         case R.id.bt_resistance:
            startResistance();
            break;
         case R.id.bt_signal:
            startSignal();
            break;
      }
   }

   private void startResistance() {
      Resistance.inst().init(this);
      Resistance.inst().resistanceStart();
   }

   private void startSignal() {
      Signal.inst().init(this);
      Signal.inst().signalStart();
   }

}