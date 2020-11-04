package com.example.renewedproject;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.ResultPoint;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QRActivity extends AppCompatActivity implements DecoratedBarcodeView.TorchListener {

    TTSAdapter tts;
    SoundManager sManager;
    BaseApplication base;

    //GPS
    double latitude;
    double longitude;
    GpsTracker gps_tracker = null;
    LocationManager locationManager; //GPS 켰는지 확인할 것임
    TMapPoint tmappoint; //현재 위치 포인트
    TMapView tMapView = null;
    String cvs_name = "";
    boolean cvs_found = false;
    String cvs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_qr);

        //바코드 인식 관련
        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        intentIntegrator.setCaptureActivity(AnyOrientationCaptureActivity.class);
        intentIntegrator.setBeepEnabled(true);//바코드 인식시 소리 나도록 설정하기
        intentIntegrator.setOrientationLocked(true); //세로 모드로 설정하기
        intentIntegrator.setPrompt("상품 진열대의 QR 코드를 인식해 보세요!");
        intentIntegrator.initiateScan();

        tts = TTSAdapter.getInstance(this);
        tts.speak("QR 코드를 인식해보세요! 상품 정보를 알려드립니다.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String productName = result.getContents(); //추출된 상품 정보
            if (productName == null) { //아무것도 추출 안 됐을 때
                tts.speak("상품 정보가 출력되지 않았네요.");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "스캔완료: " + result.getContents(), Toast.LENGTH_LONG).show();
                base.progressON(this, "상품 정보 추출중"); //로딩 화면 켜기
                tts.speak("상품 정보 추출 중입니다. 잠시만 기다려 주세요.");

                //위치
                String location = getLocation();


                //서버로 productName과 편의점 위치 보내기


                //product html에서 상품 정보 가져오기 GET
                getRetrofit(productName);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void getRetrofit(String prod_name) {
        Log.d("상황: ", "retrofitGet 메소드에 진입");
        Retrofit retrofit2 = new Retrofit.Builder().baseUrl("http://52.14.75.37:8000/myapp/").addConverterFactory(GsonConverterFactory.create()).build();

        //@GET/@POST 설정해 놓은 인터페이스와 연결
        RetrofitService2 retrofitService = retrofit2.create(RetrofitService2.class);
        retrofitService.getData(prod_name).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                List<Product> data2 = response.body();
                String result = "";

                Log.d("상황: ", "상품 인식에서 GET 성공");

                //어차피 상품 정보 하나만 있으니까 for문 돌릴 필요 없음.
                if (data2.get(0).getEvent_cd() == null) { //이벤트 값 없으면
                    result = "상품 이름 " + data2.get(0).getProd_name() + "\n가격 " + data2.get(0).getProd_price() + "원.\n일반 상품.";
                } else {
                    result = "상품 이름 " + data2.get(0).getProd_name() + "\n가격 " + data2.get(0).getProd_price() + "원.\n할인 행사 " + data2.get(0).getEvent_cd() + ".";
                }

                show(result);
                Log.d("상황: ", result);
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Log.d("상황: ", "상품 인식에서 GET 실패");
            }
        });
    }

    void show(String result) {
        base.progressOFF(); //로딩 화면 끄기
        tts.speak(result + "입니다.");
        //화면 꺼짐 방지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("상품 정보");
        builder.setMessage(result);
        AlertDialog alertDialot = builder.create();
        builder.show();
        try {
            builder.wait(5000);
        } catch (Exception e) {
            Log.d("상황: ", "팝업 오류");
        }
    }


    private String getLocation() {
        gps_tracker = new GpsTracker(this);
        latitude = gps_tracker.getLatitude();
        longitude = gps_tracker.getLongitude();
        tmappoint = new TMapPoint(latitude, longitude);
        //"편의점" 키워드로 검색
        TMapData tMapData = new TMapData();
        tMapData.findAroundNamePOI(tmappoint, "편의점", new TMapData.FindAroundNamePOIListenerCallback() {
            @Override
            public void onFindAroundNamePOI(ArrayList poiItem) {
                if (poiItem == null) return;
                TMapPoint my_point = new TMapPoint(latitude, longitude); // 현재 위치

                //제일 가까운 편의점 찾기
                double min_distance = Double.POSITIVE_INFINITY;
                int min_index = -1;
                TMapPOIItem item;
                for (int i = 0; i < poiItem.size(); i++) {
                    item = (TMapPOIItem) poiItem.get(i);
                    double distance = item.getDistance(my_point);
                    if (distance < min_distance) {
                        min_distance = distance;
                        min_index = i;
                    }
                }

                //제일 가까운 편의점에서 20m 이내에 있으면 동일 편의점으로 간주
                if (min_index >= 0 && min_distance <= 20) { // 20 meters
                    item = (TMapPOIItem) poiItem.get(min_index);
                    cvs_name = item.getPOIName().toString();
                } else
                    cvs_name = "not_found";
                Log.d("QRActivity", "편의점이름: " + cvs_name);
                // String title = cvs_name + "@(" + latitude + "," + longitude + ")";

                //편의점 이름을 cvs_code로 변환해서 title에 저장
                String title = get_cvs_code(cvs_name);
                Log.d("QRActivity", "CVS name: " + title);
                cvs = title; //전역변수
            }
        });
        return cvs;
    }

    //편의점 이름을 cvs_code로 변환(키워드로 변환)
    private String get_cvs_code(String cvs_info) {
        String code;
        if (cvs_info.trim().startsWith("세븐일레븐")) code = "seven";
        else if (cvs_info.trim().startsWith("이마트24")) code = "emart";
        else if (cvs_info.trim().startsWith("GS25")) code = "GS";
        else if (cvs_info.trim().startsWith("CU")) code = "CU";
        else code = "none";
        Log.d("CameraActivity", "cvs_info:" + cvs_info);
        Log.d("CameraActivity", "code: " + code);
        return code;
    }

    @Override
    public void onTorchOn() {

    }

    @Override
    public void onTorchOff() {

    }

    //뒤로 가면 다시 이 화면이 뜨도록
    @Override
    public void onResume() {
        super.onResume();
        this.onResume();
    }

    //어플이 꺼지거나 중단 된다면 TTS 어댑터의 ttsShutdown() 메소드 호출하기
    protected void onDestroy() {
        super.onDestroy();
        tts.ttsShutdown();
    }
}

//구현 2) 다른 Activity 연결 없이 바로 상품 정보 알리는 거 > 한 번 인식되면 멈춰야 되는데 계속 인식해서 팝업 창이 쌓이는 문제

//    private CaptureManager capture;
//    private DecoratedBarcodeView barcodeScannerView;
//    String product;
//
//    TTSAdapter tts;
//    SoundManager sManager;
//    BaseApplication base;
//
//    //GPS
//    double latitude;
//    double longitude;
//    GpsTracker gps_tracker = null;
//    LocationManager locationManager; //GPS 켰는지 확인할 것임임
//    TMapPoint tmappoint; //현재 위치 포인트
//    TMapView tMapView = null;
//    String cvs_name = "";
//    boolean cvs_found = false;
//    String cvs;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_qr);
//
//        barcodeScannerView = (DecoratedBarcodeView) findViewById(R.id.dbvBarcode);
//
//        capture = new CaptureManager(this, barcodeScannerView);
//        capture.initializeFromIntent(this.getIntent(), savedInstanceState);
//        capture.decode();
//
//        //로딩 관련
//        // base = new BaseApplication();
//
//        tts = TTSAdapter.getInstance(this);
//        tts.speak("QR 코드를 인식해보세요! 상품 정보를 알려드립니다.");
//
//
//        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
//            @Override
//            public void barcodeResult(BarcodeResult result) {
//                //startProgress();
//                product = result.toString();
//
//                //위치
//                String location = getLocation();
//
//                //서버로 productName과 편의점 위치 보내기
//
//
//                //product html에서 상품 정보 가져오기 GET
//                getRetrofit(product);
//
//            }
//
//            @Override
//            public void possibleResultPoints(List<ResultPoint> resultPoints) {
//
//            }
//        });
//    }
//
//    public void getRetrofit(String prod_name) {
//        Log.d("상황: ", "retrofitGet 메소드에 진입");
//        Retrofit retrofit2 = new Retrofit.Builder().baseUrl("http://52.14.75.37:8000/myapp/").addConverterFactory(GsonConverterFactory.create()).build();
//
//        //@GET/@POST 설정해 놓은 인터페이스와 연결
//        RetrofitService2 retrofitService = retrofit2.create(RetrofitService2.class);
//        retrofitService.getData(prod_name).enqueue(new Callback<List<Product>>() {
//            @Override
//            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
//                List<Product> data2 = response.body();
//                String result="";
//
//                Log.d("상황: ", "상품 인식에서 GET 성공");
//
//                //어차피 상품 정보 하나만 있으니까 for문 돌릴 필요 없음.
//                if(data2.get(0).getEvent_cd()==null) { //이벤트 값 없으면 (이게 되려나)
//                    result = "상품 이름 " + data2.get(0).getProd_name() + "\n가격 " + data2.get(0).getProd_price() + "원.\n일반 상품.";
//                }else{
//                    result = "상품 이름 " + data2.get(0).getProd_name() + "\n가격 " + data2.get(0).getProd_price() + "원.\n할인 행사 "+data2.get(0).getEvent_cd() +".";
//
//                }
//
//                show(result);
//                Log.d("상황: ",result);
//            }
//
//            @Override
//            public void onFailure(Call<List<Product>> call, Throwable t) {
//                Log.d("상황: ", "상품 인식에서 GET 실패");
//            }
//        });
//    }
//
////    //로딩 화면 실행하는 메소드
////    private void startProgress() {
////        base.progressON(this, "상품 정보 추출중");
////        //tts.speak("상품 정보 추출 중입니다. 잠시만 기다려 주세요.");
////    }
//
//    //결과 화면 보여주기
//    void show(String result)
//    {
//        Intent intent = new Intent(this, ResultQR.class);
//        intent.putExtra("result", result);
////        base.progressOFF();
////        tts.speak(result +"입니다.");
////        //화면 꺼짐 방지
////        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
////
////        AlertDialog.Builder builder = new AlertDialog.Builder(this);
////        builder.setTitle("상품 정보");
////        builder.setMessage(result);
////        AlertDialog alertDialot = builder.create();
////        builder.show();
////        try{
////            builder.wait(5000);
////        }catch(Exception e){
////            Log.d("상황: ","팝업 오류");
////        }
//    }



//    private String getLocation() {
//        gps_tracker = new GpsTracker(this);
//        latitude = gps_tracker.getLatitude();
//        longitude = gps_tracker.getLongitude();
//        tmappoint = new TMapPoint(latitude, longitude);
//        //"편의점" 키워드로 검색
//        TMapData tMapData = new TMapData();
//        tMapData.findAroundNamePOI(tmappoint, "편의점", new TMapData.FindAroundNamePOIListenerCallback() {
//            @Override
//            public void onFindAroundNamePOI(ArrayList poiItem) {
//                if (poiItem == null) return;
//                TMapPoint my_point = new TMapPoint(latitude, longitude); // 현재 위치
//
//                //제일 가까운 편의점 찾기
//                double min_distance = Double.POSITIVE_INFINITY;
//                int min_index = -1;
//                TMapPOIItem item;
//                for (int i = 0; i < poiItem.size(); i++) {
//                    item = (TMapPOIItem) poiItem.get(i);
//                    double distance = item.getDistance(my_point);
//                    if (distance < min_distance) {
//                        min_distance = distance;
//                        min_index = i;
//                    }
//                }
//
//                //제일 가까운 편의점에서 20m 이내에 있으면 동일 편의점으로 간주
//                if (min_index >= 0 && min_distance <= 20) { // 20 meters
//                    item = (TMapPOIItem) poiItem.get(min_index);
//                    cvs_name = item.getPOIName().toString();
//                } else
//                    cvs_name = "not_found";
//                Log.d("QRActivity", "편의점이름: " + cvs_name);
//                // String title = cvs_name + "@(" + latitude + "," + longitude + ")";
//
//                //편의점 이름을 cvs_code로 변환해서 title에 저장
//                String title = get_cvs_code(cvs_name);
//                Log.d("QRActivity", "CVS name: " + title);
//                cvs = title; //전역변수
//            }
//        });
//        return cvs;
//    }
//
//    //편의점 이름을 cvs_code로 변환(키워드로 변환)
//    private String get_cvs_code(String cvs_info) {
//        String code;
//        if (cvs_info.trim().startsWith("세븐일레븐")) code = "seven";
//        else if (cvs_info.trim().startsWith("이마트24")) code = "emart";
//        else if (cvs_info.trim().startsWith("GS25")) code = "GS";
//        else if (cvs_info.trim().startsWith("CU")) code = "CU";
//        else code = "none";
//        Log.d("CameraActivity", "cvs_info:" + cvs_info);
//        Log.d("CameraActivity", "code: " + code);
//        return code;
//    }
//
//    //뒤로가기 누르면 계속 바코드 액티비티를 띄우기 위한 것.
//    @Override
//    protected void onResume() {
//        super.onResume();
//        capture.onResume();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        capture.onPause();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        capture.onDestroy();
//    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        capture.onSaveInstanceState(outState);
//    }
//
//
//    @Override
//    public void onTorchOn() {
//
//    }
//
//    @Override
//    public void onTorchOff() {
//
//    }