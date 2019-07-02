package com.zebra.jamesswinton.savannaprintdemo;

import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.ScannerException;
import com.zebra.jamesswinton.savannaprintdemo.databinding.ActivityMainBinding;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

  // Debugging
  private static final String TAG = "MainActivity";

  // Constants
  private static final String API_KEY = "YOUR_API_KEY";
  private static final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

  // Static Variables
  private static PrintApi mPrintApi;
  private static String mZplToPrint;
  private static String mSerialNumber;

  // Non-Static Variables
  private ActivityMainBinding mDataBinding;
  private DataListener mDataListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);


    if (Build.MANUFACTURER.contains("Zebra Technologies")
        || Build.MANUFACTURER.contains("Motorola Solutions")) {
      mDataListener = scanDataCollection -> {
        // Get Scanner Data as []
        ScanDataCollection.ScanData[] scannedData = scanDataCollection.getScanData().toArray(
            new ScanDataCollection.ScanData[scanDataCollection.getScanData().size()]);

        // Debugging
        for (ScanDataCollection.ScanData scanData : scannedData) {
          Log.i(TAG, "Label Type: " + scanData.getLabelType().name());
          Log.i(TAG, "Barcode: " + scanData.getData());
          Log.i(TAG, "Label Type: " + scanData.getLabelType().toString());
        }
      };
    }

    // Init Retrofit
    mPrintApi = RetrofitInstance.getInstance().create(PrintApi.class);

    // Set Click Listener
    mDataBinding.printButton.setOnClickListener(v -> {
      // Validate Barcode Entered
      if (TextUtils.isEmpty(mDataBinding.printerSerialNumber.getText())
          || mDataBinding.printerSerialNumber.getText() == null) {
        mDataBinding.printerSerialNumber.setError("Please Enter a Serial Number");
        return;
      }

      // Get Serial Number
      mSerialNumber = mDataBinding.printerSerialNumber.getText().toString();
      mZplToPrint = mDataBinding.zpl.getText().toString();

      // Init Body
      RequestBody body = RequestBody.create(MediaType.parse("text/plain;charset=UTF-8"), mZplToPrint);

      // Send Print Job
      mPrintApi.sendPrintJob(API_KEY, mSerialNumber, body).enqueue(sendPrintJobCallback);
    });
  }

  private Callback<Object> sendPrintJobCallback = new Callback<Object>() {
    @Override
    public void onResponse(Call<Object> call, Response<Object> response) {
      // Validate HTTP Response (200, 404, etc...)
      if (!response.isSuccessful()) {
        Log.e(TAG, "Unsuccessful Response: " + response.code());
        mMainThreadHandler.post(() -> Toast.makeText(App.mContext,
            "Unsuccessful Response: " + response.code(), Toast.LENGTH_LONG).show());
        return;
      }

      // Validate Body
      if (response.body() == null) {
        Log.e(TAG, "Response Body Was Null!");
        mMainThreadHandler.post(() -> Toast.makeText(App.mContext, "Response Body Was Null!",
            Toast.LENGTH_LONG).show());
        return;
      }

      Log.i(TAG, "Success");
    }

    @Override
    public void onFailure(Call<Object> call, Throwable t) {
      Log.e(TAG, "onResponse: Unsuccessful - " + t.getMessage(), t);
      mMainThreadHandler.post(() -> Toast.makeText(App.mContext,
          "onResponse: Unsuccessful - " + t.getMessage(), Toast.LENGTH_LONG).show());
    }
  };

  @Override
  protected void onResume() {
    super.onResume();
    // Enable Scanner
    if (Build.MANUFACTURER.contains("Zebra Technologies")
        || Build.MANUFACTURER.contains("Motorola Solutions")) {
      mDataBinding.zpl.postDelayed(this::enableScanner, 100);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Disable Scanner
    if (Build.MANUFACTURER.contains("Zebra Technologies")
        || Build.MANUFACTURER.contains("Motorola Solutions")) {
      disableScanner();
    }
  }

  private void enableScanner() {
    final Scanner.DataListener dataListener = mDataListener;
    try {
      ((App) getApplicationContext()).enableScanner(dataListener);
    } catch (ScannerException e) {
      Log.e(TAG, "ScannerException: " + e.getMessage());
    }
  }

  private void disableScanner() {
    try {
      ((App) getApplicationContext()).disableScanner(mDataListener);
    } catch (ScannerException e) {
      Log.e(TAG, "ScannerException: " + e.getMessage());
    }
  }
}
