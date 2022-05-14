package com.zark.bbandroid.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Checking permissions to use Bluetooth and GPS.<br/>
 * Bluetooth enable request<br/>
 * GPS enable request
 */
public class SensorHelper {
   private final AppCompatActivity _context;
   private String[] BLE_PERM;
   private final AtomicBoolean _started = new AtomicBoolean();
   private GoogleApiClient _googleApiClient;
   private final BluetoothAdapter _bluetoothAdapter;

   /**
    * Constructor
    *
    * @param context Context is needed to request and check permissions
    */
   public SensorHelper(AppCompatActivity context) {
      _context = context;
      _bluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
   }

   private void initBlePerm() {
      List<String> blePermTmp = new ArrayList<>();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
         blePermTmp.add(Manifest.permission.ACCESS_FINE_LOCATION);
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
      {
         blePermTmp.add(Manifest.permission.BLUETOOTH_SCAN);
         blePermTmp.add(Manifest.permission.BLUETOOTH_CONNECT);
      } else {
         blePermTmp.add(Manifest.permission.BLUETOOTH);
         blePermTmp.add(Manifest.permission.BLUETOOTH_ADMIN);
      }

      //noinspection ToArrayCallWithZeroLengthArrayArgument
      BLE_PERM = blePermTmp.toArray(new String[blePermTmp.size()]);
   }

   private boolean isPermissionReady() {
      if (BLE_PERM == null) {
         initBlePerm();
      }
      for (String perm : BLE_PERM)
         if (ContextCompat.checkSelfPermission(_context, perm) != PackageManager.PERMISSION_GRANTED) {
            return false;
         }
      return true;
   }

   private void invokeSensorReady(final ISensorEvent callback) {
      _started.set(false);
      if (callback != null)
         callback.ready();
   }

   private void invokeSensorCancel(final ISensorEvent callback, final String msg, final Exception ex) {
      _started.set(false);
      if (msg != null)
         Log.d("[SensorHelper]", msg, ex);
      if (callback != null)
         callback.cancel(msg, ex);
   }

   private void continueBleEnabled(final ISensorEvent callback) {
      if (_started.get()) { // Check ble module status and start scanner
         if (_bluetoothAdapter.isEnabled()) {
            invokeSensorReady(callback);
         } else {
            _context.runOnUiThread(new Runnable() {
               @Override
               public void run() {
                  _context.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                     @Override
                     public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                           invokeSensorReady(callback);
                        } else {
                           invokeSensorCancel(callback, "User cancel operation", null);
                        }
                     }
                  }).launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
               }
            });
         }
      }
   }

   private void continuePermReady(final ISensorEvent callback) {
      if (_started.get()) {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) { // GPS Enabled required
            final LocationManager lm = (LocationManager) _context.getSystemService(Context.LOCATION_SERVICE);
            if (!LocationManagerCompat.isLocationEnabled(lm)) { // GPS Disabled
               // You can implement another mechanism for enabling GPS
               if (_googleApiClient == null) {
                  _googleApiClient = new GoogleApiClient.Builder(_context).addApi(LocationServices.API).build();
               }
               _googleApiClient.connect();
               LocationRequest locationRequestL = createLocationRequest();
               LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                     .addLocationRequest(locationRequestL)
                     .setAlwaysShow(false)
                     .setNeedBle(true);
               LocationServices
                     .getSettingsClient(_context)
                     .checkLocationSettings(builder.build())
                     .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                           try {
                              if (_started.get())
                                 task.getResult(ApiException.class);
                           } catch (ApiException ex) {
                              if (ex.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                                 try {
                                    ResolvableApiException resolvable = (ResolvableApiException) ex;
                                    _context.registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), new ActivityResultCallback<ActivityResult>() {
                                       @Override
                                       public void onActivityResult(ActivityResult result) {
                                          if (LocationManagerCompat.isLocationEnabled(lm)) {
                                             continueBleEnabled(callback);
                                          } else {
                                             invokeSensorCancel(callback, "User cancel operation", null);
                                          }
                                       }
                                    }).launch(new IntentSenderRequest.Builder(resolvable.getResolution()).build());
                                 } catch (Exception e) {
                                    invokeSensorCancel(callback, "An error occurred while GPS enabled", e);
                                 }

                              } else {
                                 continueBleEnabled(callback);
                              }
                           }
                        }
                     });
            } else {
               continueBleEnabled(callback);
            }
         } else {
            continueBleEnabled(callback);
         }
      }
   }

   private LocationRequest createLocationRequest() {
      return LocationRequest.create()
            .setInterval(TimeUnit.MINUTES.toMillis(5))
            .setFastestInterval(TimeUnit.MINUTES.toMillis(5))
            .setPriority(LocationRequest.PRIORITY_LOW_POWER);
   }

   /**
    * Checking permissions to use Bluetooth and GPS.<br/>
    * Bluetooth enable request<br/>
    * GPS enable request
    *
    * @param callback result operation callback
    */
   public void enabledSensor(final ISensorEvent callback) {
      if (!_started.getAndSet(true)) {
         if (!isPermissionReady()) { // Check permission
            // Request permissions
            _context.registerForActivityResult(
                  new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
                     @Override
                     public void onActivityResult(Map<String, Boolean> result) {
                        for (boolean itRes : result.values())
                           if (!itRes) {
                              invokeSensorCancel(callback, "User cancel operation", null);
                              return;
                           }
                        continuePermReady(callback);
                     }
                  }).launch(BLE_PERM);
         } else {
            continuePermReady(callback);
         }
      }
   }

   /**
    * Cancel process
    */
   public void cancel() {
      _started.set(false);
   }

   public interface ISensorEvent {
      /**
       * Invoked when Bluetooth and GPS ready to use
       */
      void ready();

      /**
       * Invoked when Bluetooth or GPS not ready to use
       *
       * @param message reason
       * @param error   error or null
       */
      void cancel(String message, Exception error);
   }
}