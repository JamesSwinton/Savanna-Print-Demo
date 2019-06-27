package com.zebra.jamesswinton.savannaprintdemo;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKResults.STATUS_CODE;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import java.util.ArrayList;
import java.util.List;

public class App extends Application {

  // Debugging
  private static final String TAG = "ApplicationClass";

  // Constants
  public static final Handler mUiThread = new Handler(Looper.getMainLooper());

  // Static Variables
  public static Context mContext;

  public static Scanner mScanner;
  private static boolean mIsScanning = false;

  private static EMDKManager mEmdkManager;
  private static BarcodeManager mBarcodeManager;
  private static List<DataListener> mDataListeners;

  // Non-Static Variables


  @Override
  public void onCreate() {
    super.onCreate();

    // Init DataListener Array
    mDataListeners = new ArrayList<>();

    // Init Context
    mContext = this;

    // Init EMDK if Available
    if (Build.MANUFACTURER.contains("Zebra Technologies")
        || Build.MANUFACTURER.contains("Motorola Solutions")) {

      // Init Listener
      EMDKListener emdkListener = new EMDKListener() {
        @Override
        public void onOpened(EMDKManager emdkManager) {
          // Log Results
          Log.i(TAG, "onOpened: EMDK Manager Initialised");

          // Assign EMDK Reference
          mEmdkManager = emdkManager;

          // Get Barcode Manager
          mBarcodeManager = (BarcodeManager) mEmdkManager.getInstance(FEATURE_TYPE.BARCODE);

          // Init Scanner
          try {
            initScanner();
          } catch (ScannerException e) {
            Log.e(TAG, "onOpened: Scanner Exception - " + e.getMessage(), e);
          }
        }

        @Override
        public void onClosed() {
          // Log EMDK Closed
          Log.i(TAG, "onClosed: EMDK Closed");

          // Release EMDK Manager
          if (mEmdkManager != null) {
            mEmdkManager.release();
            mEmdkManager = null;
          }
        }
      };

      // Get EMDK
      EMDKResults emdkManagerResults = EMDKManager.getEMDKManager(this, emdkListener);

      // Verify EMDK Manager
      if (emdkManagerResults == null || emdkManagerResults.statusCode != STATUS_CODE.SUCCESS) {
        // Log Error
        Log.e(TAG, "onCreate: Failed to get EMDK Manager -> " + emdkManagerResults.statusCode);
      }

    }
  }

  private

  void initScanner() throws ScannerException {
    // Init Scanner
    mScanner = mBarcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);
    // Set Scanner Listeners
    mScanner.addDataListener(scanDataCollection -> mUiThread.post(() -> {
      // Handle Data
      for (DataListener dataListener : mDataListeners) {
        dataListener.onData(scanDataCollection);
      }

      // Restart Scanner
      if (mScanner != null) {
        try {
          if (!mScanner.isReadPending()) mScanner.read();
        } catch (ScannerException e) {
          Log.e(TAG, "onData: ScannerException: " + e.getMessage(), e);
        }
      }
    }));
    mScanner.addStatusListener(statusData -> {
      switch (statusData.getState()) {
        case IDLE:
          try {
            try { Thread.sleep(200); }
            catch (InterruptedException e) { e.printStackTrace(); }
            mScanner.read();
          } catch (ScannerException e) {
            Log.e(TAG, "onStatus: ScannerException - " + e.getMessage(), e);
          }
          break;
        case WAITING:
          Log.i(TAG, "onStatus: Scanner Waiting...");
          break;
        case SCANNING:
          Log.i(TAG, "onStatus: Scanner Scanning...");
          break;
        case DISABLED:
          Log.i(TAG, "onStatus: Scanner Disabled...");
          break;
        case ERROR:
          Log.i(TAG, "onStatus: Scanner Error!");
          break;
      }
    });
    // Enable Scanner if needed
    if (mIsScanning) {
      enableScanner(null);
    }
  }

  public void enableScanner(DataListener dataListener) throws ScannerException {
    Log.i(TAG, "enableScanner: Enabling Scanner...");

    // Add DataListener to List if Exists
    if (dataListener != null && !mDataListeners.contains(dataListener)) {
      mDataListeners.add(dataListener);
    }

    // Enable Scanner
    mScanner.enable();
    mIsScanning = true;

    // Build & Set Scanner Meta (Can only be done after Scanner is Enabled)
    ScannerConfig config = mScanner.getConfig();
    config.readerParams.readerSpecific.imagerSpecific.pickList = ScannerConfig.PickList.ENABLED;
    config.scanParams.decodeAudioFeedbackUri = "system/media/audio/notifications/decode-short.wav";
    config.scanParams.decodeHapticFeedback = true;
    config.decoderParams.upce0.enabled = true;
    config.decoderParams.upce1.enabled = true;
    config.decoderParams.upca.enabled = true;
    mScanner.setConfig(config);
  }

  public void disableScanner(DataListener dataListener) throws ScannerException {
    Log.i(TAG, "disableScanner: Disabling Scanner...");

    // Remove DataListener from List if Exists
    if (mDataListeners.contains(dataListener)) {
      mDataListeners.remove(dataListener);
      mScanner.removeDataListener(dataListener);
    }

    // Disable Scanner
    mScanner.disable();
    mIsScanning = false;
  }

  @Override
  public void onTerminate() {
    mContext = null;
    super.onTerminate();
  }
}
