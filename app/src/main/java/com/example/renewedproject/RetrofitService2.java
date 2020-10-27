package com.example.renewedproject;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RetrofitService2 {
    //서버에서 데이터를 얻는 GET: 상품 정보
    //편의점 분류 변수를 받아오는 것을 기본으로 한다.
    @GET("product/")
    Call<List<Product>> getData(@Query("event_cd") String event_cd);

}
