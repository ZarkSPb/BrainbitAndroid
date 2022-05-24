package com.zark.bbandroid.brainbitandroid;

import android.graphics.Color;
import android.graphics.Paint;

import com.androidplot.ui.HorizontalPositioning;
import com.androidplot.ui.Size;
import com.androidplot.ui.SizeMode;
import com.androidplot.ui.VerticalPositioning;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.zark.bbandroid.utils.MinMaxArrayHelper;
import com.zark.bbandroid.utils.PlotHolder;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

public class RhythmHolder {
   private final static String TAG = "[RhythmHolder]";
   private final XYPlot _plotSignal;
   private SignalDoubleModel _plotSeriesTheta;
   private SignalDoubleModel _plotSeriesAlpha;
   private SignalDoubleModel _plotSeriesBeta;

   private ZoomVal _zoomVal;

   public RhythmHolder(XYPlot plotSignal) {
      if (plotSignal == null)
         throw new NullPointerException("plotSignal can not be null");
      _plotSignal = plotSignal;
      initPlot();
   }

   public void startRender(ZoomVal zoomVal, float windowDurationSek, float sampleRate) {
      stopRender();
      float wndSizeSek = windowDurationSek <= 0 ? 5.0f : windowDurationSek;
      int size = (int) Math.ceil(sampleRate * wndSizeSek);
      _plotSeriesTheta = new SignalDoubleModel(size, sampleRate, zoomVal.isAuto(), zoomVal.getTop().doubleValue());
      _plotSeriesAlpha = new SignalDoubleModel(size, sampleRate, zoomVal.isAuto(), zoomVal.getTop().doubleValue());
      _plotSeriesBeta = new SignalDoubleModel(size, sampleRate, zoomVal.isAuto(), zoomVal.getTop().doubleValue());

      SignalFadeFormatter formatter1 = new SignalFadeFormatter(size);
      formatter1.setLegendIconEnabled(false);
      formatter1.getLinePaint().setColor(0x669900);
      _plotSignal.addSeries(_plotSeriesTheta, formatter1);

      SignalFadeFormatter formatter2 = new SignalFadeFormatter(size);
      formatter2.setLegendIconEnabled(false);
      formatter2.getLinePaint().setColor(0xFF4444);
      _plotSignal.addSeries(_plotSeriesAlpha, formatter2);

      SignalFadeFormatter formatter3 = new SignalFadeFormatter(size);
      formatter3.setLegendIconEnabled(false);
      formatter3.getLinePaint().setColor(0x0099CC);
      _plotSignal.addSeries(_plotSeriesBeta, formatter3);

      setZoomY(zoomVal);
      _plotSignal.setDomainBoundaries(0, wndSizeSek, BoundaryMode.FIXED);
      _plotSeriesTheta.setRenderRef(new WeakReference<>(_plotSignal.getRenderer(AdvancedLineAndPointRenderer.class)));
      _plotSeriesAlpha.setRenderRef(new WeakReference<>(_plotSignal.getRenderer(AdvancedLineAndPointRenderer.class)));
      _plotSeriesBeta.setRenderRef(new WeakReference<>(_plotSignal.getRenderer(AdvancedLineAndPointRenderer.class)));

      _plotSignal.setDomainStepValue(windowDurationSek + 1);
      _plotSignal.setRangeStepValue(6);
   }

   public void addData(double[] theta, double[] alpha, double[] beta) {
      _plotSeriesTheta.addData(theta);
      _plotSeriesAlpha.addData(alpha);
      _plotSeriesBeta.addData(beta);
   }

   public void setZoomY(ZoomVal zoomVal) {
      if (zoomVal == null)
         return;
      if (_zoomVal != zoomVal) {
         _zoomVal = zoomVal;
         if (zoomVal.ordinal() <= PlotHolder.ZoomVal.V_AUTO_1.ordinal()) {
            _plotSeriesTheta.setAutoRange(false);
            _plotSeriesAlpha.setAutoRange(false);
            _plotSeriesBeta.setAutoRange(false);
            _plotSignal.setRangeBoundaries(zoomVal.getBottom(), zoomVal.getTop(), zoomVal.isAuto() ? BoundaryMode.AUTO : BoundaryMode.FIXED);
         } else {
            _plotSeriesTheta.setAutoRangeScale(zoomVal.getTop().doubleValue());
            _plotSeriesAlpha.setAutoRangeScale(zoomVal.getTop().doubleValue());
            _plotSeriesBeta.setAutoRangeScale(zoomVal.getTop().doubleValue());
            _plotSeriesTheta.setAutoRange(true);
            _plotSeriesAlpha.setAutoRange(true);
            _plotSeriesBeta.setAutoRange(true);
         }
      }
   }

   public void stopRender() {
      if (_plotSeriesTheta != null) {
         _plotSignal.removeSeries(_plotSeriesTheta);
         _plotSeriesTheta = null;
      }
      if (_plotSeriesAlpha != null) {
         _plotSignal.removeSeries(_plotSeriesAlpha);
         _plotSeriesAlpha = null;
      }
      if (_plotSeriesBeta != null) {
         _plotSignal.removeSeries(_plotSeriesBeta);
         _plotSeriesBeta = null;
      }
   }

   private void initPlot() {
      _plotSignal.getGraph().getGridBackgroundPaint().setColor(Color.TRANSPARENT);
      _plotSignal.getBackgroundPaint().setColor(Color.TRANSPARENT);
      _plotSignal.getGraph().getBackgroundPaint().setColor(Color.TRANSPARENT);
      _plotSignal.getBorderPaint().setColor(Color.TRANSPARENT);
      _plotSignal.getGraph().setSize(new Size(0, SizeMode.FILL, 0, SizeMode.FILL));
      _plotSignal.getGraph().position(0, HorizontalPositioning.ABSOLUTE_FROM_LEFT, 0, VerticalPositioning.ABSOLUTE_FROM_TOP);
      _plotSignal.setLinesPerRangeLabel(3);
      new Redrawer(_plotSignal, 30, true);
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
//         this.setLegendIconEnabled(false);
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
//         getLinePaint().setColor(Color.GREEN);
         getLinePaint().setAlpha(Math.max(alpha, 0));
         return getLinePaint();
      }
   }

   public enum ZoomVal {
      V_3(3, -3),
      V_2(2, -2),
      V_1(1, -1),
      V_1_0(1, 0),
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
}
