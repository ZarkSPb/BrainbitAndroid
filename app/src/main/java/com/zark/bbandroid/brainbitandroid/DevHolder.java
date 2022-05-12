package com.zark.bbandroid.brainbitandroid;

import androidx.appcompat.app.AppCompatActivity;

import com.neuromd.neurosdk.DeviceInfo;
import com.neuromd.neurosdk.DeviceType;
import com.zark.bbandroid.brainbitandroid.utils.DeviceHelper;

import java.util.List;

final class DevHolder {
   private final static Object _mutex = new Object();
   private static volatile DevHolder _inst;

   private DeviceHelper _deviceHelper;


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


}
