package com.zark.bbandroid.brainbitandroid;

import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.androidplot.xy.XYPlot;
import com.neuromd.neurosdk.ChannelInfo;
import com.neuromd.neurosdk.ChannelType;
import com.neuromd.neurosdk.Command;
import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.SignalChannel;
import com.neuromd.neurosdk.SourceChannel;
import com.zark.bbandroid.utils.CommonHelper;
import com.zark.bbandroid.utils.PlotHolder;

final class Signal {
   private final static String TAG = "[Signal]";
   private final static Object _mutex = new Object();
   private static  volatile Signal _inst;

   private PlotHolder plotO1;
   private PlotHolder plotO2;
   private PlotHolder plotT3;
   private PlotHolder plotT4;

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
      if(activity != null) {
         _activity = activity;
         initPlot();
      }
   }

   private void initPlot() {
      plotO1 = new PlotHolder((XYPlot) _activity.findViewById(R.id.plot_signal_1));
      plotO2 = new PlotHolder((XYPlot) _activity.findViewById(R.id.plot_signal_2));
      plotT3 = new PlotHolder((XYPlot) _activity.findViewById(R.id.plot_signal_3));
      plotT4 = new PlotHolder((XYPlot) _activity.findViewById(R.id.plot_signal_4));
   }

   public void signalStart() {
      try {
         Device device = DevHolder.inst().device();
         if(device != null) {
            ChannelInfo channelInfoO1 = DevHolder.inst().getDevChannel(SourceChannel.O1.name(), ChannelType.Signal);
            ChannelInfo channelInfoO2 = DevHolder.inst().getDevChannel(SourceChannel.O2.name(), ChannelType.Signal);
            ChannelInfo channelInfoT3 = DevHolder.inst().getDevChannel(SourceChannel.T3.name(), ChannelType.Signal);
            ChannelInfo channelInfoT4 = DevHolder.inst().getDevChannel(SourceChannel.T4.name(), ChannelType.Signal);
            if (channelInfoO1 != null) {
               configureDevice(device);
               plotO1.startRender(new SignalChannel(device, channelInfoO1), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
               plotO2.startRender(new SignalChannel(device, channelInfoO2), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
               plotT3.startRender(new SignalChannel(device, channelInfoT3), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
               plotT4.startRender(new SignalChannel(device, channelInfoT4), PlotHolder.ZoomVal.V_AUTO_M_S2, 5.0f);
            }
         }
      } catch (Exception ex) {
         Log.d(TAG, "Failed start signal", ex);
         CommonHelper.showMessage(_activity, R.string.err_start_signal);
      }
   }

   private void configureDevice(Device device) {
      device.execute(Command.StartSignal);
   }
}
