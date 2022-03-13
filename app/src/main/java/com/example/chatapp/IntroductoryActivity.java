package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class IntroductoryActivity extends AppCompatActivity {
    ImageView logo, splashImg;
    LottieAnimationView lottieAnimationView;
    EditText username, password;
    TextView viewSignin, forgotpass, signup, others;
    LinearLayout linearLayout;
    Button buttonLogin;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introductory);
        int time = 1200;
        logo = findViewById(R.id.logo);
        lottieAnimationView = findViewById(R.id.lottie);
        splashImg = findViewById(R.id.img);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        viewSignin = findViewById(R.id.viewSignin);
        forgotpass = findViewById(R.id.forgotpass);
        linearLayout = findViewById(R.id.layoutSignin);
        others = findViewById(R.id.others);
        buttonLogin = findViewById(R.id.buttonLogin);
        signup = findViewById(R.id.signup);
        splashImg.animate().translationY(-700).setDuration(1000).setStartDelay(time);
        viewSignin.animate().translationY(-1600).setDuration(1000).setStartDelay(time);
        logo.animate().translationY(-100).setDuration(1000).setStartDelay(time);
        username.animate().translationY(-1500).setDuration(1000).setStartDelay(time);
        password.animate().translationY(-1250).setDuration(1000).setStartDelay(time);
        lottieAnimationView.animate().translationY(1400).setDuration(1000).setStartDelay(time);
        signup.animate().translationY(-1100).setDuration(1000).setStartDelay(time);
        linearLayout.animate().translationY(-600).setDuration(1000).setStartDelay(time);
        others.animate().translationY(-840).setDuration(1000).setStartDelay(time);
        buttonLogin.animate().translationY(-1200).setDuration(1000).setStartDelay(time);
        forgotpass.animate().translationY(-1450).setDuration(1000).setStartDelay(time);


        auth = FirebaseAuth.getInstance();
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String txt_email = username.getText().toString();
                String txt_password = password.getText().toString();
                if (TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password)) {
//                    Toast.makeText(IntroductoryActivity.this, Const.TEXT_REQUIRED_ALL_FIELDS, Toast.LENGTH_SHORT).show();
//                    username.setError("First name is required!");
                } else {
                    auth.signInWithEmailAndPassword(txt_email, txt_password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(IntroductoryActivity.this, Const.LOGIN_SUCCESS, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(IntroductoryActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(IntroductoryActivity.this, Const.WARNING_LOGIN_FAIL, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });


    }
}