package com.example.capstone;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentMain extends Fragment {   // 기본 프래그먼트

    private TextView textView;

    private Button logoutButton;
    private Button payButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_fragment_main, container, false);


        logoutButton = (Button)view.findViewById(R.id.button_log_out);  // 로그인 버튼
        payButton = (Button)view.findViewById(R.id.button_pay_system);   // 결제하러 가기 버튼

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("로그 아웃");
                builder.setMessage("로그 아웃 하시겠습니까?\n");
                builder.setCancelable(true);
                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        ((MainActivity)getActivity()).logOut();  // 메인클래스의 함수, 자동로그인 정보삭제

                        Intent intent = new Intent(getContext(),LoginActivity.class);  // 로그인 액티비티로 넘어감
                        startActivity(intent);   // 로그인 액티비티 시작
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
        });

        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("결제 하기");
                builder.setMessage("결제 하러 가시겠습니까?\n");
                builder.setCancelable(true);
                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        Intent intent = new Intent(getContext(),PaySystem.class);  // 로그인 액티비티로 넘어감
                        startActivity(intent);    // 결제 액티비티 시작

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
        });

        String havetopay = getArguments().getString("사용 금액");

        // 프래그먼트는 view.findView 해줘야된다
        textView = (TextView) view.findViewById(R.id.textView_total_momey);  // 현재까지 사용 금액 적을 텍스트뷰
        textView.setText("결제해야 할 금액 : " + havetopay + "원");    // 받은 금액으로 설정


        return view;
    }

}
