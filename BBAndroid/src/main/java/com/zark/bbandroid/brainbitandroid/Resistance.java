package com.zark.bbandroid.brainbitandroid;

import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.neuromd.neurosdk.ChannelInfo;
import com.neuromd.neurosdk.ChannelType;
import com.neuromd.neurosdk.Command;
import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.INotificationCallback;
import com.neuromd.neurosdk.ResistanceChannel;
import com.neuromd.neurosdk.SourceChannel;
import com.zark.bbandroid.utils.CommonHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

final class Resistance {
   private final static String TAG = "[Resistance]";
   private final static Object _mutex = new Object();
   private static volatile Resistance _inst;

   private Future<?> _futureUpd;

   private AppCompatActivity _activity;

   private ResistanceChannel _resistChannelO1;
   private ResistanceChannel _resistChannelO2;
   private ResistanceChannel _resistChannelT3;
   private ResistanceChannel _resistChannelT4;

   private final AtomicReference<Double> _resO1Val = new AtomicReference<>(0.0);
   private final AtomicReference<Double> _resO2Val = new AtomicReference<>(0.0);
   private final AtomicReference<Double> _resT3Val = new AtomicReference<>(0.0);
   private final AtomicReference<Double> _resT4Val = new AtomicReference<>(0.0);

   public static Resistance inst() {
      Resistance instRef = _inst;
      if (instRef == null) {
         synchronized (_mutex) {
            if (instRef == null) {
               _inst = instRef = new Resistance();
            }
         }
      }
      return instRef;
   }

   public void init(AppCompatActivity activity) {
      if (activity != null)
         _activity = activity;
   }

   public void resistanceStart() {
      try {
         Device device = DevHolder.inst().device();
         if (device != null) {
            ChannelInfo channelInfoO1 = DevHolder.inst().getDevChannel(SourceChannel.O1.name(), ChannelType.Resistance);
            ChannelInfo channelInfoO2 = DevHolder.inst().getDevChannel(SourceChannel.O2.name(), ChannelType.Resistance);
            ChannelInfo channelInfoT3 = DevHolder.inst().getDevChannel(SourceChannel.T3.name(), ChannelType.Resistance);
            ChannelInfo channelInfoT4 = DevHolder.inst().getDevChannel(SourceChannel.T4.name(), ChannelType.Resistance);
            if (channelInfoO1 != null && channelInfoO2 != null && channelInfoT3 != null && channelInfoT4 != null) {
               configureDevice(device);
               initChannels(device, channelInfoO1, channelInfoO2, channelInfoT3, channelInfoT4);
               _futureUpd = Executors.newFixedThreadPool(1).submit(new Runnable() {
                  @Override
                  public void run() {
                     try {
                        while (!Thread.currentThread().isInterrupted()) {
                           Thread.sleep(500);
                           updateData();
                        }
                     } catch (Exception ignored) {
                        // skip
                     }
                  }
               });
            }
         }
      }catch (Exception ex) {
         Log.d(TAG, "Failed start signal", ex);
         CommonHelper.showMessage(_activity, R.string.err_start_signal);
      }
   }

   /**
    * Update value in view
    *
    * @param txtValue display text field
    * @param resVal   resistance value in Ohms
    */
   private void updateViewValue(TextView txtValue, double resVal) {
      if (txtValue != null) {
         txtValue.setText(resVal >= Double.POSITIVE_INFINITY || resVal <= Double.NEGATIVE_INFINITY ?
               _activity.getString(R.string.el_resistance_infinity_value) :
               _activity.getString(R.string.el_resistance_value, resVal / 1000.0));
      }
   }

   private void updateData() {
      if (_activity != null) {
         _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                  updateViewValue((TextView) _activity.findViewById(R.id.txt_o1_value), _resO1Val.get());
                  updateViewValue((TextView) _activity.findViewById(R.id.txt_o2_value), _resO2Val.get());
                  updateViewValue((TextView) _activity.findViewById(R.id.txt_t3_value), _resT3Val.get());
                  updateViewValue((TextView) _activity.findViewById(R.id.txt_t4_value), _resT4Val.get());
               }
         });
      }
   }

   private void configureDevice(Device device) {
      device.execute(Command.StartResist);
   }

   private void updateResist(ResistanceChannel resCh, AtomicReference<Double> resVal) {
      int tt = resCh.totalLength();
      if (tt > 0) {
         double[] resVals = resCh.readData(tt - 1, 1);
         if (resVals != null && resVals.length > 0) {
            resVal.set(resVals[0]);
         }
      }
   }

   private void initChannels(Device device,
                             ChannelInfo chInfO1,
                             ChannelInfo chInfO2,
                             ChannelInfo chInfT3,
                             ChannelInfo chInfT4) {
      _resistChannelO1 = new ResistanceChannel(device, chInfO1);
      _resistChannelO2 = new ResistanceChannel(device, chInfO2);
      _resistChannelT3 = new ResistanceChannel(device, chInfT3);
      _resistChannelT4 = new ResistanceChannel(device, chInfT4);

      _resistChannelO1.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
         @Override
         public void onNotify(Object sender, Integer nParam) {
            updateResist(_resistChannelO1, _resO1Val);
         }
      });

      _resistChannelO2.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
         @Override
         public void onNotify(Object sender, Integer nParam) {
            updateResist(_resistChannelO2, _resO2Val);
         }
      });
      _resistChannelT3.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
         @Override
         public void onNotify(Object sender, Integer nParam) {
            updateResist(_resistChannelT3, _resT3Val);
         }
      });
      _resistChannelT4.dataLengthChanged.subscribe(new INotificationCallback<Integer>() {
         @Override
         public void onNotify(Object sender, Integer nParam) {
            updateResist(_resistChannelT4, _resT4Val);
         }
      });
   }
}
