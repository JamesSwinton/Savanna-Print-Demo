package com.zebra.jamesswinton.savannaprintdemo;

import android.provider.SyncStateContract.Constants;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PrintApi {

  @POST("{printerSN}/sendRawData")
  Call<Object> sendPrintJob(
      @Header("apikey") String apiKey,
      @Path("printerSN") String serialNumber,
      @Body RequestBody zpl
  );

}
