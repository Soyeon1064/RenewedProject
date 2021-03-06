package com.example.renewedproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.renewedproject.R;
import com.example.renewedproject.SplashActivity;
import com.example.renewedproject.TTSAdapter;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity {

    private String introduce;
    private TTSAdapter tts = null; //TTS 사용하고자 한다면 1) 클래스 객체 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //스플래쉬 화면 실행
        startActivity(new Intent(this, SplashActivity.class));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        tts = TTSAdapter.getInstance(this);

        //권한 요청 메소드 호출
        if(checkPermission()==false){
            tts.speak("어플을 이용하기 위한 권한을 모두 허용해 주세요.");
        }
    }

    //권한이 허용되어 있는지 확인한다.
    public boolean checkPermission() {

        String tmp = "";

        //카메라 권한 확인 > 권한 승인되지 않으면 tmp에 권한 내용 저장
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            tmp += Manifest.permission.CAMERA+" ";
        }

        //카메라 저장 권한 확인
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            tmp += Manifest.permission.WRITE_EXTERNAL_STORAGE+" ";
        }

        //위치 권한1 확인
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            tmp += Manifest.permission.ACCESS_FINE_LOCATION+" ";
        }

        //위치 권한2 확인
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            tmp += Manifest.permission.ACCESS_COARSE_LOCATION;
        }

        //tmp에 내용물이 있다면, 즉 권한 승인받지 못한 권한이 있다면
        if(TextUtils.isEmpty(tmp) == false) {
            //권한 요청하기
            tts.speak("어플을 이용하기 위해 화면에 뜨는 모든 권한을 허용해 주세요.");
            ActivityCompat.requestPermissions(this, tmp.trim().split(" "), 1);
            return false;
        }else{
            //허용 되어 있으면 그냥 두기
            Log.d("상황: ", "권한 모두 허용");
            return true;
        }

    }

    //권한에 대한 응답이 있을때 자동 작동하는 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //권한 허용했을 경우
        if(requestCode == 1){
            int length = permissions.length;
            for(int i=0; i<length; i++){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    //동의
                    Log.d("상황: ","권한 허용 "+permissions[i]);
                }
            }
        }
    }


    //Button0-> 앱 도움말 버튼 클릭
    public void onTTSButtonClicked(View view) {
        introduce = "시각 장애인 및 저시력자가 편의점의 편의 서비스를 쉽게 이용할 수 있도록 도와주는 어플 니편 내편 입니다."+
                "메뉴는 지금 실행 중인 음성 사용 설명서부터 상품인식. 멤버십 정보. 가까운 편의점 순서로 배치되어 있습니다."+
        "상품 인식 메뉴는 진열대 옆의 QR 코드를 사용자 휴대폰 카메라로 인식하고 인식된 상품의 이름, 가격, 할인 여부를 음성 및 화면으로 제공합니다."+
        "멤버십 정보는 지에스 25, 씨유, 세븐 일레븐, 이마트 24 순서로 멤버십 정보를 제공합니다."+
        "가까운 편의점 메뉴는 현재 사용자의 위치에서 가장 가까운 편의점을 알려줍니다. 위치와, 카메라 권한을 반드시 허용해 주세요.";

        tts.speak(introduce);
    }

    //Button1-> 사물인식_카메라 촬영 버튼 클릭
    public void onButtonCameraClicked(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }
    //Button1-> 사물인식_카메라 촬영 버튼 클릭
//    public void onButtonCameraClicked(View view) {
//        Intent intent = new Intent(this, QRActivity.class);
//        startActivity(intent);
//    }


    //Button2-> 멤버십 안내 버튼 클릭
    public void onMembershipButtonClicked(View view) {
        Intent intent = new Intent(this, MembershipActivity.class);
        startActivity(intent);

    }

    //Button3-> 근처 편의점 찾기 버튼 클릭
    public void onLocationButtonClicked(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);

    }

    //어플이 꺼지거나 중단 된다면 TTS 어댑터의 ttsShutdown() 메소드 호출하기
    protected void onDestroy() {
        super.onDestroy();
        tts.ttsShutdown();
    }

}