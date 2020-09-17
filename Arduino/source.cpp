/*
 * =====================아두이노의 작동 흐름=====================
 * 아두이노에 사용된 센서는 gps센서와 서보모터, 두가지입니다.
 * 아두이노의 작동 흐름은 다음과 같습니다.
 * 우선 gps 값을 수신하면 서버와의 통신을 시작해, 측정된 gps값을 알맞게 변환하여 전송을 해줍니다.
 * 또한 서버로부터 데이터를 입력받으면 해당 값에 따라 작동을 달리하게 됩니다.
 * 
 * =====================gps에 대한 설명========================
 * gps센서로 측정된 값은 NMEA라고 하는 프로토콜의 형태로 값을 측정합니다. 
 * 이는 우리가 평소에 익히 보고 사용을 하는 위도와 경도의 형태꼴이 아닌
 * $GPGGA,015442.00, 3458.17997 ,N, 12728.74791 ,E,1,04,6.67,39.9,M,21.1,M,,*61
 * 다음과 같은 형태로 값을 측정 및 출력해주게 됩니다.
 * 해당 값을 사용하기 위해서는 우리가 평소 알고있는 형태로 적절히 변환하는 과정이 필요합니다.
 * 해당 값에서 위도는 N 앞에 있는 값, 경도는 E앞에 있는 값을 의미합니다. 
 * 다만 이 값들을 그대로 파싱을 하는게 아닌 따로 계산을 해주어야 제대로 된 값이 나오게 됩니다.
 * 위도의 경우에는 앞 두 자리 값을, 경도의 경우에는 앞 세 자리 값을 따로 떼어내어 각각 60으로 나눗셈을 한 뒤, 
 * 덧셈을 해주면 위도와 경도꼴의 형태가 나오게 됩니다.
 * 
 * ====================서보모터에 대한 설명=======================
 * 서보모터는 자전거를 잠글때 사용하는 자물쇠와 같은 역할을 하며, 서버로부터 입력받은 키값에 따라 서보모터가 작동됩니다.
 * 서보모터는 90도와 0도, 두가지로 움직이게 설정하였습니다. 90도는 잠금해제, 0도는 잠금설정을 의미합니다.
 * 서버로부터 key값 #1#을 수신하면, 킥보드를 사용하겠다는 의미로, 서보모터를 90도 회전시켜 잠금을 해제합니다.
 * 서버로부터 key값 #0#을 수신하면, 킥보드의 사용을 종료하겠다는 의미로, 서보모터를 다시 0도로 회전시켜 잠금을 설정합니다.
 */




#include <ESP8266WiFi.h>
#include <SoftwareSerial.h>
#include <Servo.h>
SoftwareSerial gpsSerial(13, 15);

//========================서보세팅================================
#define SERVO_PIN 05
Servo myservo;//서보 라이브러리
bool is_using = false;//현재 사용중인지 구분, 초기값은 false로 사용중이 아님을 의미

//gps 변수 세팅===================================================
char sensor = 0; // Wn 인지 구분 및 str에 저장.
String str = ""; // \n 전까지 sensor값을 저장.
String is_GPGGA = "GPGGA"; // str의 값이 NMEA의 GPGGA 값인지 확인을 위한 변수

//wifi 변수 세팅==================================================
const char* ssid = "IN";//인터넷 연결을 위한 변수
const char* password = "0316950316";//인터넷 연결을 위한 변수
const char* host = "marods.cafe24.com";//접속할 서버의 주소
String id = "CS001";//id값
String Send_Data1 = "";//서버로 전송할 위도값 저장
String Send_Data2 = "";//서버로 전송할 경도값 저장

void setup() {

  Serial.begin(9600);

  //wifi 연결 세팅
  Serial.println("WIFI Setting...");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
  }

  //서보 세팅
  Serial.println("Servo Setting...");
  myservo.attach(SERVO_PIN);//서보 시작
  myservo.write(0);//초기 서보 모터를 0도로 위치 시킴

  //gps 세팅
  Serial.println("GPS Setting...");
  gpsSerial.begin(9600);

}

void loop() {
  //=======================WIFI, GPS SENSING=========================
  if (gpsSerial.available()) // gps 센서의 사용가능 여부
  {
    sensor = gpsSerial.read(); // 센서의 값 읽기
    if (sensor == '\n')// \n 값인지 구분
    {
      // \n 일시, 즉 아무것도 측정되지 않았을시, 지금까지 저장된 str(str의 첫번째값부터 6번째값)값이 targetStr과 맞는지 구분
      if (is_GPGGA.equals(str.substring(1, 6)))
      {
        // NMEA 의 GPGGA 값일시
        Serial.println(str);

        // ,단위로 파싱 indexOf를 이용하여 파싱함
        int first = str.indexOf(",");
        int two = str.indexOf(",", first + 1);
        int three = str.indexOf(",", two + 1);
        int four = str.indexOf(",", three + 1);
        int five = str.indexOf(",", four + 1);

        // Lat과 Long 위치에 있는 값들을 index로 추출
        String Lat = str.substring(two + 1, three);
        String Long = str.substring(four + 1, five);

        // Lat의 앞값과 뒷값을 구분
        String Lat1 = Lat.substring(0, 2);
        String Lat2 = Lat.substring(2);

        // Long의 앞값과 뒷값을 구분
        String Long1 = Long.substring(0, 3);
        String Long2 = Long.substring(3);

        // 좌표 계산
        double lati = Lat1.toDouble() + Lat2.toDouble() / 60;
        float longi = Long1.toFloat() + Long2.toFloat() / 60;

        // 좌표 출력
        Serial.print("Lat : ");
        Serial.println(lati, 15);
        Serial.print("Long : ");
        Serial.println(longi, 15);
        delay(3000);

        //좌표값이 0이면 해당 좌표값은 전송하지 않음.
        if (lati != 0 && longi != 0)
        {
          Send_Data1 += String(lati, 6);//소숫점 여섯자리까지 끊어서 Send_Data1에 저장
          Send_Data2 += String(longi, 6);//소숫점 여섯자리까지 끊어서 Send_Data1에 저장
          
          //서버와의 통신을 위한 호출
          WiFiClient client;
          const int httpPort = 80;
          if (!client.connect(host, httpPort))
          {
            return;
          }

          //서버로 해당 데이터를 전송할 값들을 지정함, 전송 데이터는 id, 위도 및 경도
          String url = "/KGB_capstone/with_bike.php";
          url += "?id=";
          url += id;
          url += "&lati=";
          url += Send_Data1;
          url += "&longi=";
          url += Send_Data2;

          //서버에게 데이터 전송을 위한 요청 실행
          client.print(String("GET ") + url + " HTTP/1.1\r\n" +
            "Host: " + host + "\r\n" +
            "Connection: close\r\n\r\n");

          //서버로부터 값을 전송받기 위한 예외처리, 서버로부터 응답이 없다면, 서버와의 연결 종료
          int timeout = millis() + 5000;
          while (client.available() == 0)
          {
            if (timeout - millis() < 0)
            {
              client.stop();
              return;
            }
          }
          
          //서버로부터의 전송된 모든 데이터를 받음
          while (client.available())//서버로부터 데이터가 있다면
          {
            Serial.println("Receiving Data...");
            String line = client.readStringUntil('\r');
            while (client.available())
            {
              char server_data;//서버로부터 받은 데이터들을 저장하기 위한 변수
              //서버로부터의 데이터는 불필요한 부분도 있기 때문에 필요한 부분만 따로 떼내어 저장함
              char key[3] = { 0 };//server_data로 부터 받은 데이터 중, 필요한 부분만을 이용하기 위한 변수 설정
              
               /*
               * 서버로부터 받은 데이터는 실질적인 데이터를 전송해준뒤, 현 서버의 상태를 뒤이어 출력해주므로
               * 필요한 부분인 앞 부분만 따로 저장할 필요가 있음
               */
              for (int i = 0; i < 3; i++)//필요한 데이터를 key에 저장
              {
                server_data = client.read();
                key[i] = server_data;
              }
              //다음 두개의 if문은 각각 사용을 시작할 때와 사용을 중지할때의 if문이다.
              if (key[0] == '#' && key[1] == '1' && key[2] == '#')//key값이 #1#이면 사용을 시작한다는 의미로, 잠금장치(서보모터)를 90도 돌려 잠금을 해제함
              {
                if (is_using == false)
                {
                  Serial.println("Welcome");
                  myservo.write(90);
                  is_using = true;
                  delay(2000);
                }
              }

              if (key[0] == '#' && key[1] == '0' && key[2] == '#')//key값이 #0#이면 사용을 중지한다는 의미로, 풀려있던 잠금장치를 다시 되돌려 잠금을 설정함
              {
                if (is_using == true)
                {
                  Serial.println("Hope you Enjoyed");
                  myservo.write(0);
                  is_using = false;
                  delay(2000);
                }
              }

            }
            //Send_Data 초기화, gps값은 상시 서버에 전송되어야 하므로 이를 위해 초기화를 해주어야 한다.
            Send_Data1 = "";
            Send_Data2 = "";
          }
        }
      }
      // 새로운 gps값을 받기 위해, str 값 초기화 
      str = "";
    }
    else // \n 아닐시, 즉 측정된 값이 끝이 아닐시, str에 문자를 계속 더함
    {
      str += sensor;
    }
  }
}