package com.example.renewedproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//편의점 'seven'으로 강제 return 못하도록 변경해야 함
//event_cd 가 null 값으로 되는 것에서 또 다시 문제가 발생했음. 그래서 DB쪽에서 강제로evnet_cd 값을 주는
//억지를 쓰셨다고 했음 ... 나중에 고칠 수 있으면 고칠 것.

public class QRActivity extends AppCompatActivity{
    private TTSAdapter tts=null;

    //GPS
    double latitude;
    double longitude;
    GpsTracker gps_tracker = null;
    LocationManager locationManager; //GPS 켰는지 확인할 것임
    TMapPoint tmappoint; //현재 위치 포인트
    TMapView tMapView = null;
    String cvs_name = "";
    boolean cvs_found = false;
    String cvs; //현재 위치하고 있는 편의점 이름
    String TAG = "QRActivity";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        tts = TTSAdapter.getInstance(this);

        tMapView = new TMapView(this);
        tMapView.setHttpsMode(true);

        //지도 초기 설정
        tMapView.setSKTMapApiKey("l7xx8af54a909a6e4bb8a498c7628aae0720");

        //화면 꺼짐 방지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        scanCode(); //스캔 화면을 키기 위한 메소드 호출
    }

    //상품인식 버튼과 관련하여 스캔 화면을 켜기 위한 메소드
    private void scanCode(){
        IntentIntegrator integrator = new IntentIntegrator (this);
        integrator.setCaptureActivity(CaptureAct.class);
        integrator.setOrientationLocked(false);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("QR 코드를 인식해 보세요! 상품 정보를 알려드립니다.");
        tts.speak("상품의 QR 코드를 인식해 보세요.");
        integrator.initiateScan();
    }


    //상품 인식이 되었을 경우
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

//        //민아 언니 휴대폰 결과 확인 위해서
//        Toast.makeText(this, "결과 코드: "+result.getContents()+"\ndata.getStringExtra: "+data.getStringExtra("SCAN_RESULT"), Toast.LENGTH_LONG).show();

        if (result != null) {
            if (result.getContents() != null) { //상품 정보가 있다면

                final String productName = result.getContents();

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
                        //cvs = title; //전역변수

                        // 서버로 productName과 편의점 위치 보내기
                        final String BASE_URL = "http://52.14.75.37:8000";  // aws

                        RequestBody requestBody = new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("title", title)
                                .addFormDataPart("prod_name", productName)
                                .build();

                        Request request = new Request.Builder()
                                .url(BASE_URL + "/myapp/qrview/")
                                .post(requestBody)
                                .build();

                        OkHttpClient client = new OkHttpClient.Builder()
                                .connectTimeout(10, TimeUnit.SECONDS)
                                .writeTimeout(10, TimeUnit.SECONDS)
                                .readTimeout(30, TimeUnit.SECONDS)
                                .retryOnConnectionFailure(true)
                                .build();

                        client.newCall(request).enqueue(new okhttp3.Callback() {
                            @Override
                            public void onFailure(okhttp3.Call call, IOException e) {
                                Log.d(TAG, "POST: Connection error " + e.toString());
                                final String error_msg = e.toString();
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(QRActivity.this, error_msg, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                                final String response_body = response.body().string();
                                if (response.isSuccessful()) {
                                    Log.d(TAG, "등록 완료");
                                    //Log.d(TAG, "onResponse: " + response.body().string());
                                    //인식된 text를 tts로 말하기
                                    speak_prod_info(response_body);
                                    //tts.speak(response_body);
                                    //getRetrofit(productName); //무엇을 인식시켜도 한라봉에이드만 계속 나옴
                                } else {
                                    Log.d(TAG, "Server Response Code : " + response.code());
                                    Log.d(TAG, response.toString());
                                    //Log.d(TAG, call.request().body().toString());
                                    //오류 발생시에 재촬영 요청
                                    tts.speak("오류입니다. 다시 한 번 촬영해 주세요.");
                                }
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        //Toast.makeText(QRActivity.this, response_body, Toast.LENGTH_LONG).show();
                                    }
                                });
                                System.out.println(response_body);
                                response.body().close();
                            }
                        });
                    }
                });

                //getRetrofit(productName); //무엇을 인식시켜도 한라봉에이드만 계속 나옴

            } else {
                Toast.makeText(this, "결과 없음.", Toast.LENGTH_LONG).show();
                tts.speak("추출된 상품 정보가 없습니다. 다시 돌아가서 QR 코드를 인식해 주세요.");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void getRetrofit(String prod_name) {
        Retrofit retrofit2 = new Retrofit.Builder().baseUrl("http://52.14.75.37:8000/myapp/").
                addConverterFactory(GsonConverterFactory.create()).build();

        //@GET 설정해 놓은 인터페이스와 연결
        RetrofitService2 retrofitService = retrofit2.create(RetrofitService2.class);
        retrofitService.getData(prod_name).enqueue(new Callback<List<Product>>(){

            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response){
                List<Product> data2 = response.body();
                String prod_info = "";

                Log.d("상황: ", "상품 인식에서 GET 성공");

                //어차피 상품 정보 하나만 있으니까 for문 돌릴 필요 없음.
                if (data2.get(0).getEvent_cd() == null) { //이벤트 값 없으면
                    prod_info = "상품 이름 " + data2.get(0).getProd_name() + "\n가격 " + data2.get(0).getProd_price() + "원.\n일반 상품.";
                } else {
                    prod_info = "상품 이름 " + data2.get(0).getProd_name() + "\n가격 " + data2.get(0).getProd_price() + "원.\n할인 행사 " + data2.get(0).getEvent_cd() + ".";
                }

                show(prod_info);
                Log.d("상황: ", prod_info);
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                tts.speak("상품 정보 값이 불러들여지지 않았습니다. 다시 시도해 주세요.");
            }
        });


    }

    private void speak_prod_info(String response) {

        String before = null;
        String prod_name = null;
        int prod_price = -1;
        String event_cd = null;

        String prod_info = "";
        String response1 = response.substring(1, response.length()-1);  //어차피 상품 정보 하나만 있으니까 for문 돌릴 필요 없음.
        System.out.println("Response1 = " + response1);
        try {
            JSONObject jsonObject = new JSONObject(response1);
            // get a String from the JSON object
            prod_name = (String) jsonObject.get("prod_name");
            prod_price = (int) jsonObject.get("prod_price");
            event_cd = (String) jsonObject.get("event_cd");

//            if(((String)jsonObject.get("event_cd")).contains("1+1") || ((String)jsonObject.get("event_cd")).contains("2+1")){
//                event_cd = (String) jsonObject.get("event_cd");
//            }else event_cd = null;
            Log.d("상품가격1: ", event_cd);
            before = "prod_name은 "+prod_name+"/prod_price는 "+prod_price+"/event_cd는 "+event_cd;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Log.d("상품가격: ",event_cd);

        if (prod_name != null && prod_price != -1) {
//            if (event_cd == null) { //이벤트 값 없으면
//                prod_info = "상품 이름 " + prod_name + "\n가격 " + prod_price + "원.\n일반 상품.";
//            } else {
//                prod_info = "상품 이름 " + prod_name + "\n가격 " + prod_price + "원.\n할인 행사 " + event_cd + ".";
//            }
            if(event_cd.equals("1+1")||event_cd.equals("2+1")){
                prod_info = "상품 이름 " + prod_name + "\n가격 " + prod_price + "원.\n할인 행사 " + event_cd + ".";
            }else{
                prod_info = "상품 이름 " + prod_name + "\n가격 " + prod_price + "원.\n일반 상품.";
            }
        }

        System.out.println("prod_info = " + prod_info);
        Handler mHandler = new Handler(Looper.getMainLooper());
        final String stmt = prod_info;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                show(stmt);
            }
        }, 0);
        Log.d("상황: ","before 정보 딩당동"+before);
        Log.d("상황: ", prod_info);
    }

    //팝업창과 음성 알림 메소드
    private void show(String prod_info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false); //다이얼로그 뒷 화면을 눌렀을 때 화면이 꺼지지 않도록 함
        builder.setTitle("상품 정보");
        builder.setMessage(prod_info); //출력 결과
        builder.setPositiveButton("상품 인식으로 돌아가기", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                scanCode(); //바코드 화면으로 다시 돌아간다.
            }
        }).setNegativeButton("메인으로 돌아가기", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish(); //액티비티 화면 종료
            }
        });

        AlertDialog dialog = builder.create();
        tts.speak(prod_info+"입니다. 팝업 창의 왼쪽 버튼은 메인 화면으로 돌아가기. 오른쪽 버튼은 상품 인식으로 돌아가기 입니다.");
        //팝업창 띄우기
        dialog.show();
    }

    //편의점 이름을 cvs_code로 변환(키워드로 변환)
    private String get_cvs_code(String cvs_info) {
//        String code;
////        if (cvs_info.trim().startsWith("세븐일레븐")) code = "seven";
////        else if (cvs_info.trim().startsWith("이마트24")) code = "emart";
////        else if (cvs_info.trim().startsWith("GS25")) code = "GS";
////        else if (cvs_info.trim().startsWith("CU")) code = "CU";
////        else code = "none";
////        Log.d("CameraActivity", "cvs_info:" + cvs_info);
////        Log.d("CameraActivity", "code: " + code);
////        return code;
        return "seven"; //테스트 중 > seven으로 명시함.
    }

    //어플이 꺼지거나 중단 된다면 TTS 어댑터의 stop()호출
    protected void onDestroy() {
        super.onDestroy();
        tts.stop();
    }
}