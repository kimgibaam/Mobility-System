package com.example.capstone;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    private static String IP_ADDRESS = "marods.cafe24.com";
    private TextView mTextViewResult;  // 디버깅용 텍스트뷰

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        findViewById(R.id.sign_up_button).setOnClickListener(onClickListener);  // 클릭
        mTextViewResult = (TextView)findViewById(R.id.textView_preserved_result); // 디버깅용
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.sign_up_button:   // 버튼눌렀을때 로직
                    signUp();
                    break;
            }
        }
    };


    private void signUp() {    // 회원가입 기능

        String email = ((EditText) findViewById(R.id.emailEditText)).getText().toString();   // email,password 값 찾기
        String password = ((EditText) findViewById(R.id.passwordEditText)).getText().toString();
        String passwordCheck = ((EditText) findViewById(R.id.passwordCheckEditText)).getText().toString();
        String name = ((EditText) findViewById(R.id.nameEditText)).getText().toString();
        String phone = ((EditText) findViewById(R.id.phoneEditText)).getText().toString();


        if (email.length() > 0 && password.length() > 0 && passwordCheck.length() > 0 && name.length() > 0 && phone.length() > 0)   // 입력이 되어있어야 실행
        {
            if (password.equals(passwordCheck)) {
                Toserver task = new Toserver();   // HTTP 통신 테스크 시작
                task.execute("http://" + IP_ADDRESS + "/KGB_capstone/sign_up.php", email, password, name, phone);

            } else {
                startToast("비밀번호를 재확인 해주세요");
            }
        } else {
            startToast("모든 내용을 입력 해주세요");
        }
    }


    private void startToast(String msg)    // 토스트 생성함수
    {
        Toast.makeText(this, msg,
                Toast.LENGTH_SHORT).show();
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);  // 레이아웃 넘기기
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  // 액티비티 넘기고 다시못돌아가게함
        startActivity(intent);
    }


    class Toserver extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(SignUpActivity.this,
                    "Please Wait", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();   // 작업이 끝났으니 다이얼로그 종료
            Log.d("TAG", "response - " + result);    // result 가 php에서 받은값!

            if(result.contains("회원가입 완료"))
            {  // mTextViewResult.setText(result);  // 디버깅용
                startToast("회원가입 성공");
                startLoginActivity();
            }
            else
            {
                mTextViewResult.setText(result);  // 디버깅용
                startToast("회원가입 실패");
            }
        }

        @Override
        protected String doInBackground(String... params) {

            GoWeb web = new GoWeb(params);

            return web.doSameThing(1);    // 모드1 : 회원가입
        }
    }
}

