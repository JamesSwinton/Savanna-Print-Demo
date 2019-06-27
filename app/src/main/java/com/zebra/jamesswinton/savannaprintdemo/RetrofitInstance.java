package com.zebra.jamesswinton.savannaprintdemo;

import com.google.gson.GsonBuilder;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitInstance {

  private static Retrofit retrofitInstance = null;
  private static final String BASE_URL = "https://sandbox-api.zebra.com/v2/printers-basic/";

  public static Retrofit getInstance() {
    if (retrofitInstance == null) {
      retrofitInstance = new Retrofit.Builder()
          .baseUrl(BASE_URL)
          .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
          .build();
    }
    return retrofitInstance;
  }

  private RetrofitInstance() { }
}