package com.example.capstone;

public class CalcMoney {    // 타이머 시작, 종료, 총 사용 금액 반환

    private long startTime;
    private long stopTime;
    private long runTime;
    private int minute;
    private int coin;
    private int totalFee;
    private int sec;

    public CalcMoney()
    {
        coin = 1;
        totalFee = 0;
    }

    public void start()
    {
        startTime = System.currentTimeMillis();
    }

    public void stop()
    {
        stopTime = System.currentTimeMillis();
    }

    public String getTotalFee()
    {
        runTime = stopTime - startTime;

        sec = (int)(runTime/1000.0);
        if(sec < 5)    // 5초 이내에 사용 중지시 가격 0원
            return "0";

        minute = (int)((runTime/1000.0)/60);

        while(minute >= 5)
        {
            coin += 1;
            minute -= 5;
        }
        totalFee = coin * 500;   // 5분당 5백원

        return totalFee + "";
    }


}
