package com.example.capstone;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static String IP_ADDRESS = "marods.cafe24.com";
    private TextView mTextViewResult;  // 디버깅

    private EditText mEditTextEmail;
    private EditText mEditTextPassword;
    private CheckBox mCheckBox;
    private String current_id;
    private Button loginButton;

    private boolean saveLoginData;
    private String email;
    private String password;

    private SharedPreferences appData;   // 자동 로그인에 쓰일 공유 객체

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mTextViewResult = (TextView) findViewById(R.id.debug); // 디버깅
        mEditTextEmail = (EditText) findViewById(R.id.EditText_email);
        mEditTextPassword = (EditText) findViewById(R.id.EditText_password);
        mCheckBox = (CheckBox) findViewById(R.id.auto_login);

        loginButton = (Button)findViewById(R.id.button_login);  // 로그인 버튼
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();     // 로그인
            }
        });

        findViewById(R.id.button_sign_up).setOnClickListener(onClickListener);

        // 설정값 불러오기
        appData = getSharedPreferences("appData", MODE_PRIVATE);
        load();

        // 이전에 로그인 정보를 저장시킨 기록이 있다면
        if (saveLoginData) {
            mEditTextEmail.setText(email);          // 값 세팅하고
            mEditTextPassword.setText(password);
            mCheckBox.setChecked(saveLoginData);
            loginButton.performClick();    // 자동으로 로그인 버튼 눌러줌
        }

    }

    // 설정값을 불러오는 함수
    private void load() {
        // SharedPreferences 객체.get타입( 저장된 이름, 기본값 )
        // 저장된 이름이 존재하지 않을 시 기본값
        saveLoginData = appData.getBoolean("SAVE_LOGIN_DATA", false);
        email = appData.getString("ID", "");
        password = appData.getString("PWD", "");
    }

    // 설정값을 저장하는 함수
    private void save() {
        // SharedPreferences 객체만으론 저장 불가능 Editor 사용
        SharedPreferences.Editor editor = appData.edit();

        // 에디터객체.put타입( 저장시킬 이름, 저장시킬 값 )
        // 저장시킬 이름이 이미 존재하면 덮어씌움
        editor.putBoolean("SAVE_LOGIN_DATA", mCheckBox.isChecked());
        editor.putString("ID", mEditTextEmail.getText().toString().trim());
        editor.putString("PWD", mEditTextPassword.getText().toString().trim());

        // apply, commit 을 안하면 변경된 내용이 저장되지 않음
        editor.apply();
    }


    View.OnClickListener onClickListener = new View.OnClickListener() { // 버튼에 따른 로직
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.button_sign_up:
                    startSignupActivity();
                    break;
            }
        }
    };

    private void login() {           // 일반 회원 로그인

        email = mEditTextEmail.getText().toString();   // email,password 값 얻기
        password = mEditTextPassword.getText().toString();

        if (email.length() > 0 && password.length() > 0) {

            Toserver task = new Toserver();
            task.execute("http://" + IP_ADDRESS + "/KGB_capstone/log_in.php", email, password);

            save();

            current_id = mEditTextEmail.getText().toString();   // 사용자 이메일 저장해둠
            mEditTextEmail.setText("");   // 아이디 비밀번호 다시 지우기
            mEditTextPassword.setText("");

        } else {
            startToast("이메일 또는 비밀번호를 입력 해주세요");
        }

    }

    private void startToast(String msg)    // 토스트 생성함수
    {
        Toast.makeText(this, msg,
                Toast.LENGTH_SHORT).show();
    }

    private void startSignupActivity()   // 회원가입 액티비티 시작
    {
        Intent intent = new Intent(this,SignUpActivity.class);
        startActivity(intent);
    }

    private void startMainActivity(String havetopay)   // 어플 메인으로 이동
    {
        Intent intent = new Intent(this,MainActivity.class);
        intent.putExtra("사용 금액",havetopay);      // 필요한 데이터 메인액티비티로
        intent.putExtra("사용자 id",current_id);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  // 다시못돌아가게함
        startActivity(intent);
    }

    class Toserver extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(LoginActivity.this,
                    "Please Wait", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();   // 작업이 끝났으니 다이얼로그 종료
            Log.d("TAG", "response - " + result);    // result 가 php에서 받은값!

            if (CheckNumber(result)) {      // php 리턴값이 단순 숫자이면 인증성공
               // mTextViewResult.setText(result);  // 디버깅
                startToast("로그인 성공");
                startMainActivity(result);     // 로그인성공하면 레이아웃에 값주면서 넘어감
            }
            else {                  // 실패
                mTextViewResult.setText(result);  // 디버깅
                startToast("로그인 실패");

            }
        }

        @Override
        protected String doInBackground(String... params) {

            GoWeb web = new GoWeb(params);

            return web.doSameThing(0);  // 모드0 : 로그인
        }
    }

    public boolean CheckNumber(String str){
        char check;
        if(str.equals(""))
        {
            //문자열이 공백인지 확인
            return false;
        }
        for(int i = 0; i<str.length(); i++){
            check = str.charAt(i);
            if( check < 48 || check > 58)
            {
                //해당 char값이 숫자가 아닐 경우
                return false;
            }
        }
        return true;
    }


    @Override
    public void onBackPressed()      // 뒤로가기버튼 기능 막음
    {
      //  super.onBackPressed();
    }


}
