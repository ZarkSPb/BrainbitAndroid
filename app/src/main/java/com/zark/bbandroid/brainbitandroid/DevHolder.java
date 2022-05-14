package com.zark.bbandroid.brainbitandroid;

import android.text.TextUtils;
import android.util.Pair;

import androidx.appcompat.app.AppCompatActivity;

import com.neuromd.neurosdk.BatteryChannel;
import com.neuromd.neurosdk.Command;
import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.DeviceInfo;
import com.neuromd.neurosdk.DeviceState;
import com.neuromd.neurosdk.DeviceType;
import com.neuromd.neurosdk.INotificationCallback;
import com.neuromd.neurosdk.ParameterName;
import com.zark.bbandroid.brainbitandroid.utils.DeviceHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

final class DevHolder {
   private final static String TAG = "[DevHolder]";
   private final static Object _mutex = new Object();
   private static volatile DevHolder _inst;

   private DeviceHelper _deviceHelper;
   private WeakReference<AppCompatActivity> _wCtx;

   private final ReentrantLock _deviceLock = new ReentrantLock();
   private Pair<String, Device> _device;

   private BatteryChannel _batteryChannel;

   private final List<IDeviceHolderCallback> _callbacks = Collections.synchronizedList(new ArrayList<IDeviceHolderCallback>());
   private final AtomicBoolean _connectionState = new AtomicBoolean();

   public static DevHolder inst() {
      DevHolder instRef = _inst;
      if (instRef == null) {
         synchronized (_mutex) {
            instRef = _inst;
            if (instRef == null) {
               _inst = instRef = new DevHolder();
            }
         }
      }
      return instRef;
   }

   public void init(AppCompatActivity context) {
      if (context != null) {
         _wCtx = new WeakReference<>(context);
         _deviceHelper = new DeviceHelper(context, DeviceType.BrainbitAny);
      }
   }

   public void setDeviceEvent(DeviceHelper.IDeviceEvent devEvent) {
      _deviceHelper.setDeviceEvent(devEvent);
   }

   public List<DeviceInfo> getDeviceInfoList() {
      return _deviceHelper.getDeviceInfoList();
   }

   public boolean isSearchStarted() {
      return _deviceHelper.isSearchStarted();
   }

   public void startSearch() {
      _deviceHelper.startSearch();
   }

   public void stopSearch() {
      _deviceHelper.stopSearch();
   }

   private void invokeBatteryStateChanged(final int val) {
      AppCompatActivity ctx = _wCtx.get();
      if (ctx != null) {
         ctx.runOnUiThread(new Runnable() {
            @Override
            public void run() {
               for (IDeviceHolderCallback cb : _callbacks) {
                  cb.batteryChanged(val);
               }
            }
         });
      }
   }

   private void invokeDevStateChanged(final boolean val) {
      AppCompatActivity ctx = _wCtx.get();
      if (ctx != null && _connectionState.getAndSet(val) != val) {
         ctx.runOnUiThread(new Runnable() {
            @Override
            public void run() {
               for (IDeviceHolderCallback cb : _callbacks) {
                  cb.deviceState(val);
               }
            }
         });
      }
   }

   private void initBatteryChannel(Device device) {
      if (_batteryChannel == null) {
         _batteryChannel = new BatteryChannel(device);
         _batteryChannel.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
            @Override
            public void onNotify(Object sender, Integer nParam) {
               BatteryChannel bCh = _batteryChannel;
               if (bCh != null) {
                  int[] batVal = bCh.readData(bCh.totalLength() - 1, 1);
                  if (batVal != null && batVal.length > 0)
                     invokeBatteryStateChanged(batVal[0]);
               }
            }
         });
      }
   }

   private void initDevState(Device device) {
      if (device != null) {
         device.parameterChanged.unsubscribe();
         device.parameterChanged.subscribe(new INotificationCallback<ParameterName>() {
            @Override
            public void onNotify(Object sender, ParameterName nParam) {
               if (nParam == ParameterName.State) {
                  Device dev = device();
                  if (dev != null) {
                     invokeDevStateChanged(dev.readParam(ParameterName.State) ==
                           DeviceState.Connected);
                  }
               }
            }
         });
      }
   }

   private void freeBatteryChannel() {
      if (_batteryChannel != null) {
         _batteryChannel.dataLengthChanged.unsubscribe();
         _batteryChannel = null;
      }
   }

   private void freeDevice() throws InterruptedException {
      if (_device != null) {
         _device.second.parameterChanged.unsubscribe();
         _device.second.disconnect();
         _device.second.close();
         _device = null;
      }
      freeBatteryChannel();
   }

   private Device connect(Device device) throws Exception {
      if (device.readParam(ParameterName.State) != DeviceState.Connected) {
         try {
            initDevState(device);
            device.connect();
         } catch (Exception ex) {
            device.parameterChanged.unsubscribe();
            device.close();
            throw ex;
         }
         initBatteryChannel(device);
      } else {
         device.execute(Command.FindMe);
      }
      return device;
   }

   public void connect(String address) throws Exception {
      if (!TextUtils.isEmpty(address)) {
         if (_deviceLock.tryLock(100, TimeUnit.MILLISECONDS)) {
            try {
               if (_device != null && !TextUtils.equals(_device.first, address)) {
                  freeDevice();
               }
               if (_device == null) {
                  _device = new Pair<>(address, connect(_deviceHelper.createDevice(address)));
               } else {
                  connect(_device.second);
               }
            } finally {
               _deviceLock.unlock();
            }
         }
      }
   }

   public void disconnected() {
      try {
         if (_deviceLock.tryLock(100, TimeUnit.MILLISECONDS))
            try {
               freeDevice();
            } finally {
               _deviceLock.unlock();
               invokeDevStateChanged(false);
            }
      } catch (Exception exception) {
         // skip
      }
   }

   public Device device() {
      try {
         if (_deviceLock.tryLock(100, TimeUnit.MILLISECONDS)) {
            try {
               return _device != null ? _device.second : null;
            } finally {
               _deviceLock.unlock();
            }
         }
      } catch (InterruptedException e) {
         // skip
      }
      return null;
   }

   public void addCallback(IDeviceHolderCallback callback) {
      if (callback == null)
         return;
      if (!_callbacks.contains(callback))
            _callbacks.add(callback);
   }

   public interface IDeviceHolderCallback {
      void batteryChanged(int val);

      void deviceState(boolean state);
   }

}
