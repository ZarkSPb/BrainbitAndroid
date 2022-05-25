package com.zark.bbandroid.brainbitandroid;

import android.nfc.Tag;
import android.util.Log;
import android.widget.TextView;

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

import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import brainflow.BrainFlowError;
import brainflow.DataFilter;
import brainflow.DetrendOperations;
import brainflow.FilterTypes;
import brainflow.WindowFunctions;

final class Signal {
   private final static String TAG = "[SignalJAVA]";
   private final static Object _mutex = new Object();
   private static volatile Signal _inst;

   private SignalHolder plotO1_T3;
   private SignalHolder plotO2_T4;
   private RhythmHolder plotRhythmO1_T3;
   private RhythmHolder plotRhythmO2_T4;

   private BrainbitSyncChannel _channel;
   private INotificationCallback<Integer> _notificationCallback;

   private AppCompatActivity _activity;

   private final double[] _dataO1_T3 = new double[450000];
   private final double[] _dataO2_T4 = new double[450000];
   private final double[] _dataFilteredO1_T3 = new double[450000];
   private final double[] _dataFilteredO2_T4 = new double[450000];

   private int _lastIndex;
   private int _lastFilteredIndex;
   private int _sampleRate;
   private int _last10Index;
   private int _nfft;

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
      plotO1_T3 = new SignalHolder((XYPlot) _activity.findViewById(R.id.plot_signal_1));
      plotO2_T4 = new SignalHolder((XYPlot) _activity.findViewById(R.id.plot_signal_2));
      plotRhythmO1_T3 = new RhythmHolder((XYPlot) _activity.findViewById(R.id.plot_signal_3));
      plotRhythmO2_T4 = new RhythmHolder((XYPlot) _activity.findViewById(R.id.plot_signal_4));
   }

   public void signalStart() {
      stopProcess();
      _lastIndex = -1;
      _lastFilteredIndex = -1;
      _last10Index = 0;
      try {
         Device device = DevHolder.inst().device();
         if (device != null) {
            _channel = new BrainbitSyncChannel(device);
            _sampleRate = (int) _channel.samplingFrequency();
            _nfft = DataFilter.get_nearest_power_of_two(_sampleRate);
            configureDevice(device);
            plotO1_T3.startRender(SignalHolder.ZoomVal.V_00001, 5.0f, _channel.samplingFrequency());
            plotO2_T4.startRender(SignalHolder.ZoomVal.V_00001, 5.0f, _channel.samplingFrequency());
            plotRhythmO1_T3.startRender(RhythmHolder.ZoomVal.V_1_0, 5.0f, _channel.samplingFrequency());
            plotRhythmO2_T4.startRender(RhythmHolder.ZoomVal.V_1_0, 5.0f, _channel.samplingFrequency());

            _notificationCallback = new INotificationCallback<Integer>() {
               int _offsetData;

               @Override
               public void onNotify(Object sender, Integer nParam) {
                  _offsetData += signalDataReceived(_channel, _offsetData);
               }
            };

            _channel.dataLengthChanged.subscribe(_notificationCallback);
         }
      } catch (Exception ex) {
         Log.d(TAG, "Failed start signal", ex);
         CommonHelper.showMessage(_activity, R.string.err_start_signal);
      }
   }

   private void updRhythms() {
      try {
         double[] dataFiltered = Arrays.copyOfRange(_dataFilteredO1_T3, _lastFilteredIndex + 1 - 1000, _lastFilteredIndex + 1);
         Pair<double[], double[]> psd = DataFilter.get_psd_welch(dataFiltered, _nfft, _nfft / 2, _sampleRate, WindowFunctions.HANNING.get_code());
         double bandPowerTheta = DataFilter.get_band_power(psd, 4.0, 8.0);
         double bandPowerAlpha = DataFilter.get_band_power(psd, 8.0, 13.0);
         double bandPowerBeta = DataFilter.get_band_power(psd, 14.0, 30.0);
         double theta1 = bandPowerTheta / (bandPowerTheta + bandPowerAlpha + bandPowerBeta);
         double alpha1 = bandPowerAlpha / (bandPowerTheta + bandPowerAlpha + bandPowerBeta);
         double beta1 = bandPowerBeta / (bandPowerTheta + bandPowerAlpha + bandPowerBeta);

         plotRhythmO1_T3.addData(new double[]{theta1}, new double[]{alpha1}, new double[]{beta1});

         _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
               updateViewValue((TextView) _activity.findViewById(R.id.txt_O1T3_theta_value), theta1);
               updateViewValue((TextView) _activity.findViewById(R.id.txt_O1T3_alpha_value), alpha1);
               updateViewValue((TextView) _activity.findViewById(R.id.txt_O1T3_beta_value), beta1);
            }
         });

         dataFiltered = Arrays.copyOfRange(_dataFilteredO2_T4, _lastFilteredIndex + 1 - 1000, _lastFilteredIndex + 1);
         psd = DataFilter.get_psd_welch(dataFiltered, _nfft, _nfft / 2, _sampleRate, WindowFunctions.HANNING.get_code());
         bandPowerTheta = DataFilter.get_band_power(psd, 4.0, 8.0);
         bandPowerAlpha = DataFilter.get_band_power(psd, 8.0, 13.0);
         bandPowerBeta = DataFilter.get_band_power(psd, 14.0, 30.0);
         double theta2 = bandPowerTheta / (bandPowerTheta + bandPowerAlpha + bandPowerBeta);
         double alpha2 = bandPowerAlpha / (bandPowerTheta + bandPowerAlpha + bandPowerBeta);
         double beta2 = bandPowerBeta / (bandPowerTheta + bandPowerAlpha + bandPowerBeta);

         plotRhythmO2_T4.addData(new double[]{theta2}, new double[]{alpha2}, new double[]{beta2});

         _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
               updateViewValue((TextView) _activity.findViewById(R.id.txt_O2T4_theta_value), theta2);
               updateViewValue((TextView) _activity.findViewById(R.id.txt_O2T4_alpha_value), alpha2);
               updateViewValue((TextView) _activity.findViewById(R.id.txt_O2T4_beta_value), beta2);
            }
         });
      } catch (BrainFlowError bfe) {
         Log.d(TAG, bfe.toString());
      }
   }

   private void updateViewValue(TextView txtValue, double rhythmVal) {
      if (txtValue != null) {
         txtValue.setText(_activity.getString(R.string.el_rhythm_value, rhythmVal * 100));
      }
   }

   private int signalDataReceived(BrainbitSyncChannel channel, int offset) {
      int length = channel.totalLength() - offset;
      if (length > 0) {
         BrainbitSyncData[] data = channel.readData(offset, length);
         for (int i = 0; i < length; i++) {
            _lastIndex++;
            _dataO1_T3[_lastIndex] = data[i].O1 - data[i].T3;
            _dataO2_T4[_lastIndex] = data[i].O2 - data[i].T4;
         }
         filteredPrepare(length);

//         Log.d(TAG, _lastIndex + " - " + _lastFilteredIndex);
         int index = (int) (Math.ceil(_lastFilteredIndex / 10) * 10);
         if (index > _last10Index) {
            _last10Index = index;
//            Log.d(TAG, "" + _last10Index);
            if (_lastFilteredIndex > 1000) updRhythms();
         }
      }
      return length;
   }

   private void filteredPrepare(int length) {
      int startIndex = Math.max(_lastIndex - 1000 - length + 1, 0);
      double[] dataFilteredO1_T3 = Arrays.copyOfRange(_dataO1_T3, startIndex, _lastIndex + 1);
      double[] dataFilteredO2_T4 = Arrays.copyOfRange(_dataO2_T4, startIndex, _lastIndex + 1);
      signalFiltering(dataFilteredO1_T3);
      signalFiltering(dataFilteredO2_T4);
      dataFilteredO1_T3 = Arrays.copyOfRange(dataFilteredO1_T3, Math.max(_lastIndex + 1 - startIndex - length, 0), _lastIndex + 1 - startIndex);
      dataFilteredO2_T4 = Arrays.copyOfRange(dataFilteredO2_T4, Math.max(_lastIndex + 1 - startIndex - length, 0), _lastIndex + 1 - startIndex);
      plotO1_T3.addData(dataFilteredO1_T3);
      plotO2_T4.addData(dataFilteredO2_T4);
      for (int i = 0; i < length; i++) {
         _lastFilteredIndex += 1;
         _dataFilteredO1_T3[_lastFilteredIndex] = dataFilteredO1_T3[i];
         _dataFilteredO2_T4[_lastFilteredIndex] = dataFilteredO2_T4[i];
      }
   }

   private void signalFiltering(double[] data) {
      try {
         DataFilter.detrend(data, DetrendOperations.CONSTANT.get_code());
         DataFilter.perform_bandstop(data, _sampleRate, 2.25, 4.5, 4, FilterTypes.BUTTERWORTH.get_code(), 0.0);
         DataFilter.perform_bandstop(data, _sampleRate, 55.0, 20.0, 4, FilterTypes.BUTTERWORTH.get_code(), 0.0);
         DataFilter.perform_bandpass(data, _sampleRate, 17.0, 26.0, 4, FilterTypes.BUTTERWORTH.get_code(), 0.0);
      } catch (BrainFlowError bfe) {
         // skip
      }
   }

   private void configureDevice(Device device) {
      device.execute(Command.StartSignal);
   }

   public void stopProcess() {
      if (_channel != null && _notificationCallback != null) {
         _channel.dataLengthChanged.unsubscribe(_notificationCallback);
         _channel = null;
         _notificationCallback = null;
      }
      if (plotO1_T3 != null) {
         plotO1_T3.stopRender();
      }
      if (plotO2_T4 != null) {
         plotO2_T4.stopRender();
      }
      if (plotRhythmO1_T3 != null) {
         plotRhythmO1_T3.stopRender();
      }
      if (plotRhythmO2_T4 != null) {
         plotRhythmO2_T4.stopRender();
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
