package com.example.renewedproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
//2) 관련 지금은 안 쓰고 있음.
public class ResultQR extends AppCompatActivity {

    TTSAdapter tts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_qr);


        Intent intent = getIntent();
        TextView tv = (TextView) findViewById(R.id.textView);
        String result = intent.getStringExtra("result");
        tv.setText(result);

        tts.speak(result+"입니다. 화면을 터치하면 상품 인식 화면으로 다시 돌아갑니다.");

    }

    //다시 상품인식 화면으로 돌아가기
    public void returnbuttonclieked(View view) {
        finish();
    }
}
