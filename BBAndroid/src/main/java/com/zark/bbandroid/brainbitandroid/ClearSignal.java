package com.zark.bbandroid.brainbitandroid;

import android.graphics.Paint;
import android.util.Log;

import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.neuromd.neurosdk.BrainbitSyncChannel;
import com.neuromd.neurosdk.BrainbitSyncData;
import com.neuromd.neurosdk.INotificationCallback;
import com.zark.bbandroid.utils.MinMaxArrayHelper;
import com.zark.bbandroid.utils.PlotHolder;

import brainflow.BrainFlowError;
import brainflow.DataFilter;
import brainflow.DetrendOperations;
import brainflow.FilterTypes;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ClearSignal {
   private final XYPlot _plotSignal;
   private SignalDoubleModel _plotSeries;
   private BrainbitSyncChannel _channel;
   private INotificationCallback<Integer> _notificationCallback;
   private ZoomVal _zoomVal;
   private double[][] _data = new double[4][450000];
   private int _lastIndex = -1;
   private int _sampleRate;

   public ClearSignal(XYPlot plotSignal) {
      if(plotSignal == null)
         throw new NullPointerException("plotSignal can not be null");
      _plotSignal = plotSignal;
   }

   public void startRender(BrainbitSyncChannel channel, ZoomVal zoomVal, float windowDurationSek) {
      if (channel == null || channel == _channel)
         return;
      stopRender();
      float wndSizeSek = windowDurationSek <= 0 ? 5.0f : windowDurationSek;
      _sampleRate = (int) channel.samplingFrequency();
      int size = (int) Math.ceil(channel.samplingFrequency() * wndSizeSek);
      _plotSeries = new SignalDoubleModel(size, channel.samplingFrequency(), zoomVal.isAuto(), zoomVal.getTop().doubleValue());
      SignalFadeFormatter formatter = new SignalFadeFormatter(size);
      formatter.setLegendIconEnabled(false);
      _plotSignal.addSeries(_plotSeries, formatter);
      setZoomY(zoomVal);
      _plotSignal.setDomainBoundaries(0, wndSizeSek, BoundaryMode.FIXED);
      _plotSeries.setRenderRef(new WeakReference<>(_plotSignal.getRenderer(AdvancedLineAndPointRenderer.class)));

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

   public void setZoomY(ZoomVal zoomVal) {
      if (zoomVal == null)
         return;
      if (_zoomVal != zoomVal) {
         _zoomVal = zoomVal;
         if (zoomVal.ordinal() <= PlotHolder.ZoomVal.V_AUTO_1.ordinal()) {
            _plotSeries.setAutoRange(false);
            _plotSignal.setRangeBoundaries(zoomVal.getBottom(), zoomVal.getTop(), zoomVal.isAuto() ? BoundaryMode.AUTO : BoundaryMode.FIXED);
         } else {
            _plotSeries.setAutoRangeScale(zoomVal.getTop().doubleValue());
            _plotSeries.setAutoRange(true);
         }
      }
   }

   private int signalDataReceived(BrainbitSyncChannel channel, int offset) {
      int length = channel.totalLength() - offset;
      BrainbitSyncData[] data = channel.readData(offset, length);
      if (length > 0) {
         for (int i = 0; i < length; i++) {
            _lastIndex++;
            _data[0][_lastIndex] = data[i].O1;
            _data[1][_lastIndex] = data[i].O2;
            _data[2][_lastIndex] = data[i].T3;
            _data[3][_lastIndex] = data[i].T4;
         }

         double [] signalFiltered = Arrays.copyOfRange(_data[0], _lastIndex - 1000 - length, _lastIndex);
         signalFiltering(signalFiltered);

         SignalDoubleModel ser = _plotSeries;
         if (ser != null)
//            ser.addData(Arrays.copyOfRange(_data[0], _lastIndex - length, _lastIndex));
            ser.addData(Arrays.copyOfRange(signalFiltered, 1000, 1000 + length));
      }

//      Log.d("[ClearSignal]", " ");
//      Log.d("[ClearSignal]", "---------" + length + "---------");
//      if (length > 0) {
//         for(int i = 0; i < length; i++) {
//            Log.d("[ClearSignal]", data[i].O1 + " " + data[i].O2 + " " + data[i].T3 + " " + data[i].T4);
//         }
//      }
//      Log.d("[ClearSignal]", "" + _lastIndex);
//      Log.d("[ClearSignal]", _data[0][_lastIndex] + " " + _data[1][_lastIndex] + " " + _data[2][_lastIndex] + " " + _data[3][_lastIndex]);

//      DataFilter.detrend(, DetrendOperations.CONSTANT);

      return length;
   }

   public void stopRender() {
      if (_channel != null && _notificationCallback != null) {
         _channel.dataLengthChanged.unsubscribe(_notificationCallback);
         _channel = null;
         _notificationCallback = null;
      }
      if (_plotSeries != null) {
         _plotSignal.removeSeries(_plotSeries);
         _plotSeries = null;
      }
   }

   private final static class SignalDoubleModel implements XYSeries {
      private final Number[][] _data;
      private WeakReference<AdvancedLineAndPointRenderer> _rendererRef;
      private int _latestIndex;
      private final double _xStep;
      private final int _dataSize;
      private final MinMaxArrayHelper _minMaxYHelper;
      private Number _minYLast;
      private Number _maxYLast;
      private boolean _autoRange;
      private final AtomicReference<Double> _autoRangeScale = new AtomicReference<>(0.0);

      public SignalDoubleModel(int size, float freqHz, boolean autoRange, double autoRangeScale) {
         _data = new Number[size][2];
         _dataSize = size;
         _xStep = 1.0 / freqHz;
         for (int i = 0; i < _dataSize; ++i) {
            _data[i][0] = _xStep * i;
         }
         _latestIndex = 0;
         _minMaxYHelper = new MinMaxArrayHelper(size);
         _autoRange = autoRange;
         _autoRangeScale.set(autoRangeScale);
      }

      public void setRenderRef(final WeakReference<AdvancedLineAndPointRenderer> rendererRef) {
         _rendererRef = rendererRef;
      }

      public void addData(double[] data) {
         AdvancedLineAndPointRenderer render = _rendererRef.get();
         if (render == null || data == null || data.length <= 0)
            return;
         int idx = _latestIndex;
         for (int i = 0; i < data.length; ++i, idx = (idx + 1) % _dataSize) {
            _data[idx][0] = idx * _xStep;
            _data[idx][1] = data[i];
            _minMaxYHelper.addValue(data[i]);
         }
         _latestIndex = idx;

         if (_autoRange) {
            boolean rangeChanged = false;
            Number min = _minMaxYHelper.getMin();
            if (_minYLast == null || Double.compare(_minYLast.doubleValue(), min.doubleValue()) != 0) {
               _minYLast = min;
               rangeChanged = true;
            }
            Number max = _minMaxYHelper.getMax();
            if (_maxYLast == null || Double.compare(_maxYLast.doubleValue(), max.doubleValue()) != 0) {
               _maxYLast = max;
               rangeChanged = true;
            }
            if (rangeChanged) {
               double offset = Math.abs(_maxYLast.doubleValue() - _minYLast.doubleValue()) * _autoRangeScale.get();
               render.getPlot().setRangeBoundaries(_minYLast.doubleValue() - offset, _maxYLast.doubleValue() + offset, BoundaryMode.FIXED);
            }
         }

         render.setLatestIndex(_latestIndex);
      }

      @Override
      public int size() {
         return _dataSize;
      }

      @Override
      public Number getX(int index) {
         return _data[index][0];
      }

      @Override
      public Number getY(int index) {
         return _data[index][1];
      }

      @Override
      public String getTitle() {
         return "Signal";
      }

      public void setAutoRange(boolean autoRange) {
         _autoRange = autoRange;
      }

      public void setAutoRangeScale(double autoRangeScale) {
         _autoRangeScale.set(autoRangeScale);
      }
   }

   private static class SignalFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {
      private final int _trailSize;

      SignalFadeFormatter(int trailSize) {
         this._trailSize = trailSize;
      }

      @Override
      public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
         int offset;
         if (thisIndex >= latestIndex) {
            offset = latestIndex + (seriesSize - thisIndex);
         } else {
            offset = latestIndex - thisIndex;
         }

         float scale = 255f / _trailSize;
         int alpha = (int) (255 - (offset * scale));
         getLinePaint().setAlpha(Math.max(alpha, 0));
         return getLinePaint();
      }
   }

   public enum ZoomVal {
      V_3(3, -3),
      V_2(2, -2),
      V_1(1, -1),
      V_05(0.5, -0.5),
      V_02(0.2, -0.2),
      V_01(0.1, -0.1),
      V_005(0.05, -0.05),
      V_002(0.02, -0.02),
      V_001(0.01, -0.01),
      V_0005(0.005, -0.005),
      V_0002(0.002, -0.002),
      V_0001(0.001, -0.001),
      V_00005(0.0005, -0.0005),
      V_00002(0.0002, -0.0002),
      V_00001(0.0001, -0.0001),
      V_000005(0.00005, -0.00005),
      V_000002(0.00002, -0.00002),
      V_000001(0.00001, -0.00001),
      V_AUTO_1(1, -1, true),
      V_AUTO_M_S05(0.5, -1, false),
      V_AUTO_M_S2(2, -1, false),
      V_AUTO_M_S2_5(2.5, -1, false),
      V_AUTO_M_S3(3, -1, false),
      V_AUTO_M_S5(5, -1, false),
      V_AUTO_M_S7(7, -1, false),
      V_AUTO_M_S10(10, -1, false),
      ;


      private ZoomVal(Number top, Number bottom, boolean auto) {
         _top = top;
         _bottom = bottom;
         _auto = auto;
      }

      private ZoomVal(Number top, Number bottom) {
         _top = top;
         _bottom = bottom;
         _auto = false;
      }

      private final Number _top;
      private final Number _bottom;
      private final boolean _auto;

      Number getTop() {
         return _top;
      }

      Number getBottom() {
         return _bottom;
      }

      boolean isAuto() {
         return _auto;
      }
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
}
