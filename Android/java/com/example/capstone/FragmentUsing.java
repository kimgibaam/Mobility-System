package com.example.capstone;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Timer;
import java.util.TimerTask;

public class FragmentUsing extends Fragment {  // 사용 시작시 나오는 프래그먼트

    private TextView textViewMoney;
    private TextView textViewAlarm;
    private Button button;

    private String bike_id;

    private Timer t;
    private CalcMoney calcMoney;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_fragment_using, container, false);

        bike_id = getArguments().getString("자전거 id");

        // 프래그먼트는 view.findView 해줘야된다
        textViewMoney = (TextView)view.findViewById(R.id.textView_money);  // 현재까지 사용 금액 적을 텍스트뷰
        textViewAlarm = (TextView)view.findViewById(R.id.textView_alarm);   // 주의 사항 적을 단순 텍스트뷰
        textViewMoney.setText("현재 사용 금액 : 0원");    // 최초 요금 상태
        textViewAlarm.setText("※5초 이내에 취소할 시 요금 면제 됩니다");

        t = new Timer();     // 타이머 설정
        t.schedule(new CustomTimer(),5000,300000);
        // 최초 요금 면제시간 5초 후에 5분 간격으로 요금이 늘어나게하는 메소드가 호출됨

        calcMoney = new CalcMoney();
        calcMoney.start();    // 요금 계산기 시작


        button = (Button)view.findViewById(R.id.stop_button);  // 사용 정지 버튼
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("사용 종료");
                builder.setMessage("사용 종료 하시겠습니까?\n");
                builder.setCancelable(true);
                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        ((MainActivity)getActivity()).stopUsing(bike_id,true);  // 메인클래스의 함수를 실행
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

        return view;
    }

    class CustomTimer extends TimerTask    // 타이머 클래스시간 지날때마다 사용금액 올려줌
    {
        public void run()
        {
            calcMoney.stop();
            String fee = calcMoney.getTotalFee();

            textViewMoney.setText("현재 사용 금액 : " + fee + "원");
        }
    }

}
