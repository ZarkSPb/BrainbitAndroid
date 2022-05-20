package com.zark.bbandroid.brainbitandroid;

import android.util.Log;

import com.neuromd.neurosdk.BrainbitSyncChannel;
import com.neuromd.neurosdk.BrainbitSyncData;
import com.neuromd.neurosdk.INotificationCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClearSignal {

   private BrainbitSyncChannel _channel;
   private INotificationCallback<Integer> _notificationCallback;
   private ArrayList<BrainbitSyncData> _data = new ArrayList<BrainbitSyncData>();

   public ClearSignal() {
      // constructor code here
   }

   public void startSignal(BrainbitSyncChannel channel) {
      if (channel == null || channel == _channel)
         return;
      _notificationCallback = new INotificationCallback<Integer>() {
         int _offsetData;
         @Override
         public void onNotify(Object sender, Integer nParam) {
            _offsetData += signalDataReceived(_channel, _offsetData);
         }
      };
      _channel = channel;
      channel.dataLengthChanged.subscribe(_notificationCallback);
   }

   private int signalDataReceived(BrainbitSyncChannel channel, int offset) {
      int length = channel.totalLength() - offset;
      BrainbitSyncData[] data;
      data = channel.readData(offset, length);
      Log.d("[ClearSignal]", " ");
      Log.d("[ClearSignal]", "---------" + length + "---------");
      if (length > 0) {
         for(int i = 0; i < length; i++) {
            Log.d("[ClearSignal]", data[i].O1 + " " + data[i].O2 + " " + data[i].T3 + " " + data[i].T4);
         }
      }
      _data.addAll(Arrays.asList(data)); // not right solution
      Log.d("[ClearSignal]", "" + _data.size());
      BrainbitSyncData lastData = _data.get(_data.size() - 1);
      Log.d("[ClearSignal]", lastData.O1 + " " + lastData.O2 + " " + lastData.T3 + " " + lastData.T4);
      return length;
   }
}
