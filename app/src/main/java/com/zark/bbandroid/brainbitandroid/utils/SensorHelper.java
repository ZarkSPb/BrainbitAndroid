package com.zark.bbandroid.brainbitandroid.utils;


import androidx.appcompat.app.AppCompatActivity;

/**
 * Checking permissions to use Bluetooth and GPS.<br/>
 * Bluetooth enable request<br/>
 * GPS enable request
 */
public class SensorHelper {
   private final AppCompatActivity _context;

   /**
    * Constructor
    *
    * @param context Context is needed to request and check permissions
    */
   public SensorHelper(AppCompatActivity context) {
      _context = context;

   }

}
