package com.zark.bbandroid.brainbitandroid;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.androidplot.xy.XYPlot;
import com.neuromd.neurosdk.BrainbitSyncChannel;
import com.neuromd.neurosdk.BrainbitSyncData;
import com.neuromd.neurosdk.Command;
import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.DeviceState;
import com.neuromd.neurosdk.INotificationCallback;
import com.neuromd.neurosdk.ParameterName;
import com.zark.bbandroid.utils.CommonHelper;

import java.util.Arrays;

import brainflow.BrainFlowError;
import brainflow.DataFilter;
import brainflow.DetrendOperations;
import brainflow.FilterTypes;

final class Signal {
   private final static String TAG = "[Signal]";
   private final static Object _mutex = new Object();
   private static volatile Signal _inst;

   private SignalHolder plotO1;
   private SignalHolder plotO2;
   private SignalHolder plotT3;
   private SignalHolder plotT4;

   private BrainbitSyncChannel _channel;
   private INotificationCallback<Integer> _notificationCallback;

   private AppCompatActivity _activity;

   private double[][] _data = new double[4][450000];
   private int _lastIndex;
   private int _sampleRate;

   public static Signal inst() {
      Signal instRef = _inst;
      if (instRef == null) {
         synchronized (_mutex) {
            if (instRef == null) {
               _inst = instRef = new Signal();
            }
         }
      }
      return instRef;
   }

   public void init(AppCompatActivity activity) {
      if (activity != null) {
         _activity = activity;
         initPlot();
      }
   }

   private void initPlot() {
      plotO1 = new SignalHolder((XYPlot) _activity.findViewById(R.id.plot_signal_1));
      plotO2 = new SignalHolder((XYPlot) _activity.findViewById(R.id.plot_signal_2));
      plotT3 = new SignalHolder((XYPlot) _activity.findViewById(R.id.plot_signal_3));
      plotT4 = new SignalHolder((XYPlot) _activity.findViewById(R.id.plot_signal_4));
   }

   public void signalStart() {
      stopProcess();
      try {
         Device device = DevHolder.inst().device();
         if (device != null) {
            _channel = new BrainbitSyncChannel(device);
            _sampleRate = (int) _channel.samplingFrequency();
            configureDevice(device);
            plotO1.startRender(SignalHolder.ZoomVal.V_AUTO_M_S2, 5.0f, _channel.samplingFrequency());
            plotO2.startRender(SignalHolder.ZoomVal.V_AUTO_M_S2, 5.0f, _channel.samplingFrequency());
            plotT3.startRender(SignalHolder.ZoomVal.V_AUTO_M_S2, 5.0f, _channel.samplingFrequency());
            plotT4.startRender(SignalHolder.ZoomVal.V_AUTO_M_S2, 5.0f, _channel.samplingFrequency());

            _notificationCallback =  new INotificationCallback<Integer>() {
               int _offsetData;
               @Override
               public void onNotify(Object sender, Integer nParam) {
                  _offsetData += signalDataReceived(_channel, _offsetData);
               }
            };
            _lastIndex = -1;
            _channel.dataLengthChanged.subscribe(_notificationCallback);
         }
      } catch (Exception ex) {
         Log.d(TAG, "Failed start signal", ex);
         CommonHelper.showMessage(_activity, R.string.err_start_signal);
      }
   }

   private int signalDataReceived(BrainbitSyncChannel channel, int offset) {
      int length = channel.totalLength() - offset;
      if (length > 0) {
         BrainbitSyncData[] data = channel.readData(offset, length);
         for (int i = 0; i < length; i++) {
            _lastIndex++;
            _data[0][_lastIndex] = data[i].O1;
            _data[1][_lastIndex] = data[i].O2;
            _data[2][_lastIndex] = data[i].T3;
            _data[3][_lastIndex] = data[i].T4;
         }

         double[] dataFiltered = Arrays.copyOfRange(_data[0], _lastIndex - 1000 - length + 1, _lastIndex + 1);
         Log.d(TAG, " ");
         Log.d(TAG, length + " - " + dataFiltered.length);
         signalFiltering(dataFiltered);

//         SignalHolder.SignalDoubleModel ser = _plotSeries;
//         if (ser != null) {
//            ser.addData(Arrays.copyOfRange(dataFiltered, 1000, 1000 + length));
//         }

         plotO1.addData(Arrays.copyOfRange(dataFiltered, 1000, 1000 + length));
      }
      return length;
   }

   private void signalFiltering(double[] data) {
      try {
         DataFilter.detrend(data, DetrendOperations.CONSTANT.get_code());
         DataFilter.perform_bandpass(data, _sampleRate, 17.0, 26.0, 4, FilterTypes.BUTTERWORTH.get_code(), 0.0);
         DataFilter.perform_bandpass(data, _sampleRate, 17.0, 26.0, 4, FilterTypes.BUTTERWORTH.get_code(), 0.0);
         DataFilter.perform_bandstop(data, _sampleRate, 50.0, 4.0, 4, FilterTypes.BUTTERWORTH.get_code(), 0.0);
         DataFilter.perform_bandstop(data, _sampleRate, 60.0, 4.0, 4, FilterTypes.BUTTERWORTH.get_code(), 0.0);
      } catch (BrainFlowError bfe) {
         // skip
      }
   }

   private void configureDevice(Device device) {
      device.execute(Command.StartSignal);
   }

   public void stopProcess() {
      if(_channel != null && _notificationCallback != null) {
         _channel.dataLengthChanged.unsubscribe(_notificationCallback);
         _channel = null;
         _notificationCallback = null;
      }

      if (plotO1 != null) {
         plotO1.stopRender();
      }
      if (plotO2 != null) {
         plotO2.stopRender();
      }
      if (plotT3 != null) {
         plotT3.stopRender();
      }
      if (plotT4 != null) {
         plotT4.stopRender();
      }

      try {
         Device device = DevHolder.inst().device();
         if (device != null) {
            if (device.readParam(ParameterName.State) == DeviceState.Connected) {
               device.execute(Command.StopSignal);
            }
         }
      } catch (Exception ex) {
         Log.d(TAG, "Failed stop signal", ex);
         CommonHelper.showMessage(_activity, R.string.err_stop_signal);
      }
   }
}
