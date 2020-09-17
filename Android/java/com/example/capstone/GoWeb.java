package com.example.capstone;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GoWeb {

    private String[] params;

    public GoWeb(String... params) {
        this.params = params;
    }


    public String doSameThing(int mode) {

        String id;
        String positive;
        String havetopay;

        String email = (String) params[1];   // 기본 : 로그인
        String password = (String) params[2];

        // 1. PHP 파일을 실행시킬 수 있는 주소와 전송할 데이터를 준비합니다.
        // POST 방식으로 데이터 전달시에는 데이터가 주소에 직접 입력되지 않습니다.
        String serverURL = (String) params[0];

        // HTTP 메시지 본문에 포함되어 전송되기 때문에 따로 데이터를 준비해야 합니다.
        // 전송할 데이터는 “이름=값” 형식이며 여러 개를 보내야 할 경우에는 항목 사이에 &를 추가합니다.
        // 여기에 적어준 이름을 나중에 PHP에서 사용하여 값을 얻게 됩니다.
        String postParameters = "email=" + email + "&password=" + password;

        if (mode == 1) {          // 회원가입
            email = (String) params[1];
            password = (String) params[2];
            String name = (String) params[3];
            String phone = (String) params[4];

            serverURL = (String) params[0];

            postParameters = "email=" + email + "&password=" + password + "&name=" + name + "&phone=" + phone;
        }

        else if (mode == 2)   // gps 전부 읽기 , 자전거 사용 여부 바꾸기
        {
            // 사용 시작
            serverURL = params[0];
            id = params[1];
            positive = params[2];
            postParameters = "id=" + id + "&positive=" + positive;

        }
        else if(mode == 3)
        {
            // 사용 종료
            serverURL = params[0];
            id = params[1];
            positive = params[2];
            email = params[3];
            havetopay = params[4];
            postParameters = "id=" + id + "&positive=" + positive + "&email=" + email + "&havetopay=" + havetopay;

        }


        try {
            // 2. HttpURLConnection 클래스를 사용하여 POST 방식으로 데이터를 전송합니다.
            URL url = new URL(serverURL);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


            httpURLConnection.setReadTimeout(5000);  //5초안에 응답이 오지 않으면 예외가 발생합니다.
            httpURLConnection.setConnectTimeout(5000);  //5초안에 연결이 안되면 예외가 발생합니다.
            httpURLConnection.setRequestMethod("POST");  //요청 방식을 POST로 합니다.
            httpURLConnection.connect();


            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(postParameters.getBytes("UTF-8"));
            //전송할 데이터가 저장된 변수를 이곳에 입력합니다. 인코딩을 고려해줘야 합니다.
            outputStream.flush();
            outputStream.close();

            // 3. 응답을 읽습니다.
            int responseStatusCode = httpURLConnection.getResponseCode();
            Log.d("TAG", "POST response code - " + responseStatusCode);

            InputStream inputStream;
            if (responseStatusCode == HttpURLConnection.HTTP_OK) {   // 정상적인 응답 데이터
                inputStream = httpURLConnection.getInputStream();
            } else {     // 에러 발생
                inputStream = httpURLConnection.getErrorStream();
            }

            // 4. StringBuilder를 사용하여 수신되는 데이터를 저장합니다.
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuilder sb = new StringBuilder();
            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }


            bufferedReader.close();

            // 5. 저장된 데이터를 스트링으로 변환하여 리턴합니다.
            return sb.toString();

        } catch (Exception e) {

            Log.d("TAG", "InsertData: Error ", e);

            return new String("Error: " + e.getMessage());
        }

    }
}
