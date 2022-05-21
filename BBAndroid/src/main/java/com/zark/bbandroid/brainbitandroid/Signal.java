package com.zark.bbandroid.brainbitandroid;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.androidplot.xy.XYPlot;
import com.neuromd.neurosdk.BrainbitSyncChannel;
import com.neuromd.neurosdk.ChannelInfo;
import com.neuromd.neurosdk.ChannelType;
import com.neuromd.neurosdk.Command;
import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.DeviceState;
import com.neuromd.neurosdk.EegChannel;
import com.neuromd.neurosdk.ParameterName;
import com.neuromd.neurosdk.SourceChannel;
import com.zark.bbandroid.utils.CommonHelper;
import com.zark.bbandroid.utils.PlotHolder;

final class Signal {
   private final static String TAG = "[Signal]";
   private final static Object _mutex = new Object();
   private static volatile Signal _inst;

   private PlotHolder plotO1;
   private PlotHolder plotO2;
   private PlotHolder plotT3;
   private PlotHolder plotT4;

   private ClearSignal _signalBase;

   private AppCompatActivity _activity;

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
         initRAWSignal();
      }
   }

   private void initPlot() {
      plotO1 = new PlotHolder((XYPlot) _activity.findViewById(R.id.plot_signal_1));
      plotO2 = new PlotHolder((XYPlot) _activity.findViewById(R.id.plot_signal_2));
      plotT3 = new PlotHolder((XYPlot) _activity.findViewById(R.id.plot_signal_3));
      plotT4 = new PlotHolder((XYPlot) _activity.findViewById(R.id.plot_signal_4));
   }

   private void initRAWSignal() {
      _signalBase = new ClearSignal((XYPlot) _activity.findViewById(R.id.plot_signal_1));
   }

   public void signalStart() {
      try {
         Device device = DevHolder.inst().device();
         if (device != null) {
            ChannelInfo channelInfoO1 = DevHolder.inst().getDevChannel(SourceChannel.O1.name(), ChannelType.Signal);
            ChannelInfo channelInfoO2 = DevHolder.inst().getDevChannel(SourceChannel.O2.name(), ChannelType.Signal);
            ChannelInfo channelInfoT3 = DevHolder.inst().getDevChannel(SourceChannel.T3.name(), ChannelType.Signal);
            ChannelInfo channelInfoT4 = DevHolder.inst().getDevChannel(SourceChannel.T4.name(), ChannelType.Signal);
            if (channelInfoO1 != null && channelInfoO2 != null && channelInfoT3 != null && channelInfoT4 != null) {
               configureDevice(device);
//               plotO1.startRender(new SignalChannel(device, channelInfoO1), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
//               plotO2.startRender(new SignalChannel(device, channelInfoO2), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
//               plotT3.startRender(new SignalChannel(device, channelInfoT3), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
//               plotT4.startRender(new SignalChannel(device, channelInfoT4), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
               plotO1.startRender(new EegChannel(device, channelInfoO1), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
               plotO2.startRender(new EegChannel(device, channelInfoO2), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
               plotT3.startRender(new EegChannel(device, channelInfoT3), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
               plotT4.startRender(new EegChannel(device, channelInfoT4), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
            }
         }
      } catch (Exception ex) {
         Log.d(TAG, "Failed start signal", ex);
         CommonHelper.showMessage(_activity, R.string.err_start_signal);
      }
   }


   // RAW Signal
   public void syncSignalStart() {
      try {
         Device device = DevHolder.inst().device();
         if (device != null) {
            configureDevice(device);
            _signalBase.startRender(new BrainbitSyncChannel(device), ClearSignal.ZoomVal.V_000005, 5.0f);
         }
      } catch (Exception ex) {
         Log.d(TAG, "Failed start signal", ex);
         CommonHelper.showMessage(_activity, R.string.err_start_signal);
      }
   }

   private void configureDevice(Device device) {
      device.execute(Command.StartSignal);
   }

   public void stopProcess() {
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

      if (_signalBase != null) {
         _signalBase.stopRender();
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
