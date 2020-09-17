package com.example.capstone;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMarkerClickListener {

    private static String IP_ADDRESS = "marods.cafe24.com";
    private GoogleApiClient mGoogleApiClient = null;
    private GoogleMap mGoogleMap = null;
    private String mJsonString; // 자전거 위치 받을 제이슨 변수
    private String bike_id;  // 마커 클릭시 자전거 id 얻을 스트링
    private String bike_positive;  // 마커 클릭시 사용 가능 여부 얻을 스트링
    private String currentFee;     // 현재까지의 총 사용 금액;
    private String user_email;      // 사용자 이메일
    private boolean nowUsing;    // 사용 중일 때는 마커를 못누름

    private FragmentManager fragmentManager;   // 프래그먼트들
    private FragmentTransaction transaction;
    private FragmentMain fragmentmain;
    private FragmentUsing fragmentusing;

    private CalcMoney calcMoney;  // 요금 계산할 객체

    private static final String TAG = "Capstone";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2002;
    private static final int UPDATE_INTERVAL_MS = 1000;  // 1초
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5초

    private AppCompatActivity mActivity;
    private boolean askPermissionOnceAgain = false;
    private boolean mRequestingLocationUpdates = false;
    private  boolean mMoveMapByUser = true;
    private boolean mMoveMapByAPI = true;

    LocationRequest locationRequest = new LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_INTERVAL_MS)
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        //////////// 프래그먼트
        fragmentManager = getSupportFragmentManager();

        fragmentmain = new FragmentMain();
        fragmentusing = new FragmentUsing();

        // 액티비티 내의 프래그먼트를 관리하려면 FragmentManager를 사용해야 함.
        transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frameLayout,fragmentmain).commitAllowingStateLoss();

        // 로그인 액티비티에서 데이터 받기
        Intent intent = getIntent();
        currentFee = intent.getStringExtra("사용 금액");
        user_email = intent.getStringExtra("사용자 id");

        // 로그인에서 받은 데이터를 다시 프래그먼트로 보내준다.
        // 금액적는 부분 프래그먼트생성하고 실행되야 동작 됨
        Bundle bundle = new Bundle(1);
        bundle.putString("사용 금액",currentFee);
        fragmentmain.setArguments(bundle);

        calcMoney = new CalcMoney();    // 요금 계산기 미리 실체화해둠

        Log.d(TAG, "onCreate");
        mActivity = this;

        mGoogleApiClient = new GoogleApiClient.Builder(this)  // 현재 위치 빌더
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // MapFragment 는 맵뷰가 "com.google.android.gms.maps.MapFragment" 형태일때
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);   // 맵 호출

    }

    @Override
    public void onResume() {      // 어플 숨겼다가 다시 실행할때

        super.onResume();

        if (mGoogleApiClient.isConnected()) {

            Log.d(TAG, "onResume : call startLocationUpdates");
            if (!mRequestingLocationUpdates) startLocationUpdates();
        }

        //앱 정보에서 퍼미션을 허가했는지를 다시 검사해봐야 한다.
        if (askPermissionOnceAgain) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                askPermissionOnceAgain = false;

                checkPermissions();
            }
        }
    }


    private void startLocationUpdates() {

        if (!checkLocationServicesStatus()) {

            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();
        } else {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "startLocationUpdates : 퍼미션 안가지고 있음");
                return;
            }


            Log.d(TAG, "startLocationUpdates : call FusedLocationApi.requestLocationUpdates");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
            mRequestingLocationUpdates = true;

            mGoogleMap.setMyLocationEnabled(true);

        }

    }

    public void logOut()   // 자동로그인 정보 삭제
    {
        SharedPreferences appData;   // 자동 로그인

        // 설정값 불러오기
        appData = getSharedPreferences("appData", MODE_PRIVATE);

        SharedPreferences.Editor editor = appData.edit();
        editor.clear();
        editor.apply();

        startToast("로그 아웃");
    }


    private void stopLocationUpdates() {

        Log.d(TAG, "stopLocationUpdates : LocationServices.FusedLocationApi.removeLocationUpdates");
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mRequestingLocationUpdates = false;
    }

    private void startToast(String msg)    // 토스트 생성함수
    {
        Toast.makeText(this, msg,
                Toast.LENGTH_SHORT).show();
    }

    public void stopUsing(String id, boolean state)  // 사용 종료 버튼 true
    {
        // 강제 어플 종료 false 일때는 서버측 데이터만 갱신해준다
        calcMoney.stop();
        String totalFee = calcMoney.getTotalFee();

        // 10초안에 취소할시에 요금 없음
        if(totalFee.equals("0"))
            currentFee = Integer.parseInt(currentFee) + "";
        else
            // 사용한만큼 현재 금액에 추가
            currentFee = (Integer.parseInt(currentFee) + Integer.parseInt(totalFee)) + "";

        GetData task = new GetData();    // 서버의 자전거 사용가능 여부, 사용 요금 갱신, 마커 다시찍기;
        task.execute("http://" + IP_ADDRESS + "/KGB_capstone/update_and_read.php", bike_id, "1", user_email, currentFee);


        if(state == true) {   // 어플 정상 실행, 사용자가 종료 버튼 누름
            // 액티비티 내의 프래그먼트를 관리하려면 FragmentManager를 사용해야 함.
            transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.frameLayout, fragmentmain).commitAllowingStateLoss();
            //////////////

            Intent intent = getIntent();   // 금액적는 부분 프래그먼트생성하고 실행되야됨
            Bundle bundle = new Bundle(1);
            bundle.putString("사용 금액", currentFee);
            fragmentmain.setArguments(bundle);
            // 로그인에서 받은 데이터를 다시 프래그먼트로 보내준다.

            nowUsing = false;     // 어플측 자전거 사용 여부 바꿈
            startToast("총 사용 금액 : " +  totalFee + "원");
        }
    }

    private class GetData extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d(TAG, "response - " + result);

            if (result == null) {        // 에러났을 경우

            } else {                  // 정상 실행

                mJsonString = result;
                showResult();         // 데이터베이스 가져온값 보여줌
            }
        }

        @Override           // php 실행시키고 응답 저장, 스트링으로 변환하여 리턴
        protected String doInBackground(String... params) {

            GoWeb web = new GoWeb(params);

            if(params.length > 3)
                return web.doSameThing(3);
            else
                return web.doSameThing(2);
        }

    }

    private void showResult(){

        String TAG_JSON="webnautes";

        mGoogleMap.clear();   // 마커한번 밀어버리고 다시찍는다

        try {
            JSONObject jsonObject = new JSONObject(mJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            for(int i=0;i<jsonArray.length();i++) {

                JSONObject item = jsonArray.getJSONObject(i);

                String id = item.getString("id");   // json 에서 값 가져옴
                String lati = item.getString("lati");
                String longi = item.getString("longi");
                String positive = item.getString("positive");

                // 받은 자전거들의 모든 정보를 받아 마커찍는다
                LatLng bikelocation = new LatLng(Double.parseDouble(lati), Double.parseDouble(longi));
                setBikeMarker(bikelocation, id, positive);


            }
        } catch (JSONException e) {

            Log.d(TAG, "showResult : ", e);
        }
        mGoogleMap.setOnMarkerClickListener(this);  // 마커클릭 리스너를 메인클래스에 달음

    }

    public void setBikeMarker(LatLng temp, String id, String positive)    // 자전거 위치에 마커찍기
    {
        // 문자열은 == 으로 비교하면안되고 equals 함수 써준다.
        if(positive.equals("1")) positive = "사용 가능";   // 숫자를 적절한 스트링으로 바꿔줌
        else positive = "사용 불가능";

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(temp);
        markerOptions.title(id);
        markerOptions.snippet(positive);

        if(positive.equals("사용 가능"))   // 사용 가능할 때는 파란 자전거, 불가할 때는 빨간 자전거
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bike_2373));
        else
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.red_bicycle));

        mGoogleMap.addMarker(markerOptions);
    }

    @Override   // 마커 클릭시 동작
    public boolean onMarkerClick(Marker marker) {

        bike_id = marker.getTitle();
        bike_positive = marker.getSnippet();

        if (bike_positive.equals("사용 가능") && nowUsing == false) {   // 사용 가능할때만 눌렀을때 다이얼로그 뜸
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("전기자전거 사용");
            builder.setMessage("자전거를 사용시작 하시겠습니까?\n\n"
                    + "요금은 5분당 500원입니다.");
            builder.setCancelable(true);
            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // 여기서 카운팅 시작 & 서버로 자전거 사용가능 여부 갱신
                    calcMoney.start();  // 카운팅 시작
                    nowUsing = true;

                    GetData task = new GetData();    // 자전거 사용가능 여부 갱신해야됨
                    task.execute("http://" + IP_ADDRESS + "/KGB_capstone/update_and_read.php", bike_id, "0");

                    startToast("사용 시작");
                    transaction = fragmentManager.beginTransaction();  // 매니저안쓰면 폴트남
                    transaction.replace(R.id.frameLayout, fragmentusing).commitAllowingStateLoss();

                    Bundle bundle = new Bundle();    // 시작 시간과 자전거 id 를 보냄
                    bundle.putString("자전거 id", bike_id);
                    fragmentusing.setArguments(bundle);
                }
            });
            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            builder.create().show();

        }
        else    // 사용불가능 일때의 클릭동작
        {
                // 아무 동작 안함
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d(TAG, "onMapReady :");

        mGoogleMap = googleMap;

        GetData task = new GetData();    // 서버에서 바이크 위치 전부 불러냄
        task.execute("http://" + IP_ADDRESS + "/KGB_capstone/read_all_gps.php", "", "");
        //파라미터 개수맞출라고 공백 넣음


        //런타임 퍼미션 요청 대화상자나 GPS 활성 요청 대화상자 보이기전에
        //지도의 초기위치를 서울로 이동
        setDefaultLocation();


        //mGoogleMap.getUiSettings().setZoomControlsEnabled(false);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        mGoogleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {

            @Override
            public boolean onMyLocationButtonClick() {

                Log.d(TAG, "onMyLocationButtonClick : 위치에 따른 카메라 이동 활성화");
                mMoveMapByAPI = true;
                return true;
            }
        });
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {

                Log.d(TAG, "onMapClick :");
            }
        });

        mGoogleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {

            @Override
            public void onCameraMoveStarted(int i) {

                if (mMoveMapByUser == true && mRequestingLocationUpdates) {

                    Log.d(TAG, "onCameraMove : 위치에 따른 카메라 이동 비활성화");
                    mMoveMapByAPI = false;
                }

                mMoveMapByUser = true;

            }
        });


        mGoogleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {

            @Override
            public void onCameraMove() {


            }
        });
    }


    @Override
    public void onLocationChanged(Location location) {

        Log.d(TAG, "onLocationChanged : ");

        LatLng lat = new LatLng(location.getLatitude(),location.getLongitude());

        // 현재 위치로 카메라 이동
        setCurrentLocation(location);
    }


    @Override
    protected void onStart() {

        if(mGoogleApiClient != null && mGoogleApiClient.isConnected() == false){

            Log.d(TAG, "onStart: mGoogleApiClient connect");
            mGoogleApiClient.connect();
        }

        super.onStart();
    }

    @Override
    protected void onStop() {

        if (mRequestingLocationUpdates) {

            Log.d(TAG, "onStop : call stopLocationUpdates");
            stopLocationUpdates();
        }

        if ( mGoogleApiClient.isConnected()) {

            Log.d(TAG, "onStop : mGoogleApiClient disconnect");
            mGoogleApiClient.disconnect();
        }

        super.onStop();
    }

    protected void onDestroy()
    {

        if(nowUsing == true)    // onStop 에 넣으면 어플 숨겼을 때도 종료됨
            stopUsing(user_email,false);  // 자전거 사용중에 강제종료 할시 처리

        super.onDestroy();
    }

    protected void onPause()   // 킬러블상태에선 무조건 자전거 사용이 중지됨
    {
        stopUsing(user_email,true);  // 자전거 사용중에 강제종료 할시 처리

        super.onPause();
    }

    public void onBackPressed()    // 뒤로가기 버튼 막기
    {
       // super.onBackPressed();
    }


    @Override
    public void onConnected(Bundle connectionHint) {  // 구글 서비스에 연결 됬을때 호출되는 함수


        if ( mRequestingLocationUpdates == false ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

                if (hasFineLocationPermission == PackageManager.PERMISSION_DENIED) {

                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                } else {

                    Log.d(TAG, "onConnected : 퍼미션 가지고 있음");
                    Log.d(TAG, "onConnected : call startLocationUpdates");
                    startLocationUpdates();
                    mGoogleMap.setMyLocationEnabled(true);
                }

            }else{

                Log.d(TAG, "onConnected : call startLocationUpdates");
                startLocationUpdates();
                mGoogleMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.d(TAG, "onConnectionFailed");
        setDefaultLocation();
    }

    @Override
    public void onConnectionSuspended(int cause) {

        Log.d(TAG, "onConnectionSuspended");
        if (cause == CAUSE_NETWORK_LOST)
            Log.e(TAG, "onConnectionSuspended(): Google Play services " +
                    "connection lost.  Cause: network lost.");
        else if (cause == CAUSE_SERVICE_DISCONNECTED)
            Log.e(TAG, "onConnectionSuspended():  Google Play services " +
                    "connection lost.  Cause: service disconnected");
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    public void setCurrentLocation(Location location) {
    // 현재 내 위치로 카메라만 이동
        mMoveMapByUser = false;

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if ( mMoveMapByAPI ) {

            Log.d( TAG, "setCurrentLocation :  mGoogleMap moveCamera "
                    + location.getLatitude() + " " + location.getLongitude() ) ;
            // CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 15);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
            mGoogleMap.moveCamera(cameraUpdate);
        }
    }

    public void setDefaultLocation() {  // 초기 위치로 카메라만 이동

        mMoveMapByUser = false;

        //디폴트 위치, Seoul
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);
        mGoogleMap.moveCamera(cameraUpdate);

    }

    //여기부터는 런타임 퍼미션 처리을 위한 메소드들
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        boolean fineLocationRationale = ActivityCompat
                .shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager
                .PERMISSION_DENIED && fineLocationRationale)
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");

        else if (hasFineLocationPermission
                == PackageManager.PERMISSION_DENIED && !fineLocationRationale) {
            showDialogForPermissionSetting("퍼미션 거부 + Don't ask again(다시 묻지 않음) " +
                    "체크 박스를 설정한 경우로 설정에서 퍼미션 허가해야합니다.");
        } else if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {


            Log.d(TAG, "checkPermissions : 퍼미션 가지고 있음");

            if ( mGoogleApiClient.isConnected() == false) {

                Log.d(TAG, "checkPermissions : 퍼미션 가지고 있음");
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (permsRequestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && grantResults.length > 0) {

            boolean permissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (permissionAccepted) {


                if ( mGoogleApiClient.isConnected() == false) {

                    Log.d(TAG, "onRequestPermissionsResult : mGoogleApiClient connect");
                    mGoogleApiClient.connect();
                }



            } else {

                checkPermissions();
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        });

        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showDialogForPermissionSetting(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                askPermissionOnceAgain = true;

                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + mActivity.getPackageName()));
                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(myAppSettings);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d(TAG, "onActivityResult : 퍼미션 가지고 있음");


                        if ( mGoogleApiClient.isConnected() == false ) {

                            Log.d( TAG, "onActivityResult : mGoogleApiClient connect ");
                            mGoogleApiClient.connect();
                        }
                        return;
                    }
                }

                break;
        }
    }

}