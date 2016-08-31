package com.tsb.newspaper.management.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tsb.newspaper.management.R;
import com.tsb.newspaper.management.infoSaved.MyInternalStorage;
import com.tsb.newspaper.management.internet.InternetUtil;
import com.tsb.newspaper.management.newspaper.GettingNewspaper;
import com.tsb.newspaper.management.newspaper.Newspaper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by dusz2 on 2016/7/20 0020.
 */
public class NewspaperInfoActivity extends AppCompatActivity {

    private  Newspaper myNewspaper;

    private String decodeResult;
    private TextView nameText;
    private TextView dateText;
    private TextView issueText;
    private TextView totalIssueText;

    protected LocationManager locationManager;
    private String provider;
    private Location location;
    private LocationListener locationListener;

    private String myLatitude;
    private String myLongitude;
    private String myLocation;

    SimpleDateFormat formatter;
    private String myTime;

    private boolean isLogin = false;

    private String myPhone;

    private boolean isGet = false;
    private boolean isContinue = false;

    private String gettingResut;

    final int REQUEST_CODE = 1;
    final int RESULT_CODE = 11;

    private int gettingHistory;

    private GettingNewspaper gettingNewspaper;
    private GettingNewspaper lastGetting = new GettingNewspaper();


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_newspaper_information);

        nameText = (TextView)findViewById(R.id.name_text);
        dateText = (TextView)findViewById(R.id.date_text);
        issueText = (TextView)findViewById(R.id.issue_text);
        totalIssueText = (TextView)findViewById(R.id.totalIssue_text);


        MyInternalStorage myInternalStorage = new MyInternalStorage(NewspaperInfoActivity.this);
        String filename = "myNewspaper";
        try{
            decodeResult = myInternalStorage.get(filename);
        }catch (IOException e){
            e.printStackTrace();
        }

        myNewspaper = new Newspaper(decodeResult);

        nameText.setText(myNewspaper.getName());
        dateText.setText(myNewspaper.getDate());
        issueText.setText(myNewspaper.getIssue());
        totalIssueText.setText(myNewspaper.getTotalIssue());

        formatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

//        if(!isGpsAble(locationManager)){
//            Toast.makeText(NewspaperInfoActivity.this, "定位服务未打开", Toast.LENGTH_SHORT).show();
//            openGPS2();
//        }

        // 获取所有可用的位置提供器
        List<String> providerList = locationManager.getProviders(true);
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        }
        else if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        }
        else {
            // 没有可用的位置提供器
            Toast.makeText(NewspaperInfoActivity.this, "定位不可用", Toast.LENGTH_SHORT).show();
            openGPS2();
            return;
        }
        Log.i("provider",provider);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // 当GPS定位信息发生改变时，更新定位
                updateLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

                // 当GPS LocationProvider可用时，更新定位
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(NewspaperInfoActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    updateLocation(locationManager.getLastKnownLocation(provider));
                }
            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //设置间隔两秒获得一次GPS定位信息
        locationManager.requestLocationUpdates(provider, 0, 0, locationListener);

        location = locationManager.getLastKnownLocation(provider);

        if(location != null){
            Log.i("location",String.valueOf(location.getLongitude()));
            updateLocation(location);
        }

    }


    @Override
    public void onResume(){
        super.onResume();
        if(provider != null){
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(NewspaperInfoActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            }
            locationManager.requestLocationUpdates(provider, 2000, 8, locationListener);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(locationManager != null){
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(NewspaperInfoActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            }
            locationManager.removeUpdates(locationListener);
        }
    }

    public void confirm_getting_onClick(View v){

        if (isLogin){

            Thread gettingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    //线程执行内容
                    //发送手机号、报纸内容、地理位置
                    //得到返回值：是否已领取，领取历史记录

                    Date curDate =  new Date(System.currentTimeMillis());
                    myTime = formatter.format(curDate);

                    gettingNewspaper = new GettingNewspaper(myNewspaper,myPhone,myLocation,myTime);

                    Log.i("领取情况",gettingNewspaper.toString());

                    String url = getResources().getString(R.string.network_url) + "record/"+myPhone+"/?name="+myNewspaper.getName()+"&jou_id="+myNewspaper.getTotalIssue();
                    Log.i("test",url);

                    InternetUtil internetUtil = new InternetUtil(url);
                    String getResult = internetUtil.getRecordMethod();
                    if(getResult != null && getResult != ""){
                        isContinue = true;
                        try {
                            JSONObject jsonObject = new JSONObject(getResult);
                            gettingHistory = jsonObject.getInt("news_num");
                            isGet = jsonObject.getBoolean("receive_state");

                            Log.i("isGet",String.valueOf(isGet));
                            if(isGet){
                                lastGetting = new GettingNewspaper(myNewspaper,myPhone,jsonObject.getString("station"),jsonObject.getString("date"));
                            }

                        }catch (JSONException e){
                            e.printStackTrace();
                        }
                    }else {
                        isContinue = false;
                        Looper.prepare();
                        Toast.makeText(NewspaperInfoActivity.this,"访问服务器异常",Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }

                }
            });

            if(new InternetUtil().isNetworkConnected(NewspaperInfoActivity.this)){
                //开启线程
                gettingThread.start();
                try
                {
                    gettingThread.join();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }else {
                isContinue = false;
                Toast.makeText(NewspaperInfoActivity.this,"网络不可用",Toast.LENGTH_SHORT).show();
            }

            Intent intent = new Intent(NewspaperInfoActivity.this,GettingResultActivity.class);

            Log.i("isGet",String.valueOf(isGet));

            if(!isGet){
                gettingResut = "领取成功";

                Thread addRecordThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String url = getResources().getString(R.string.network_url)+"record/"+myPhone+"/";
                        InternetUtil internetUtil = new InternetUtil(url);

                        String gettingResult  = internetUtil.putRecordMethod(gettingNewspaper.getGettingInformation());

                        if(gettingResult == "OK"){
                            isContinue = true;
                            Looper.prepare();
                            Toast.makeText(NewspaperInfoActivity.this,"领取成功",Toast.LENGTH_SHORT).show();
                            Looper.loop();
                        }else {
                            isContinue = false;
                            Looper.prepare();
                            Toast.makeText(NewspaperInfoActivity.this,"访问服务器异常，领取失败",Toast.LENGTH_SHORT).show();
                            Looper.loop();
                        }
                    }
                });
                if(new InternetUtil().isNetworkConnected(NewspaperInfoActivity.this)){
                    addRecordThread.start();
//                    try {
//                        addRecordThread.join();
//                    } catch (InterruptedException e)
//                    {
//                        e.printStackTrace();
//                    }
                }else {
                    isContinue = false;
                    Toast.makeText(NewspaperInfoActivity.this,"网络不可用",Toast.LENGTH_SHORT);
                }

            }else {
                gettingResut = "该用户已领取";
                intent.putExtra("gettingInformation",lastGetting.getLastGetting());
                isContinue = true;
            }

            Log.i("isContinue",String.valueOf(isContinue));

            if(isContinue){
                intent.putExtra("isGet",isGet);
                intent.putExtra("gettingResult",gettingResut);
                intent.putExtra("gettingHistory",gettingHistory);

                startActivity(intent);
            }

        }
        else{
            Toast.makeText(NewspaperInfoActivity.this,"尚未登录，请登录",Toast.LENGTH_SHORT).show();
//            myNewspaper.saveNewspaperInformation();
            Intent intent = new Intent(NewspaperInfoActivity.this,GetNewspaperActivity.class);
            startActivityForResult(intent,REQUEST_CODE);
        }

    }

//    private boolean isGpsAble(LocationManager lm){
//        return lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)?true:false;
//    }

    private void openGPS2(){
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent,0);
    }

    private void updateLocation(Location location) {
        if (location != null) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("当前的位置信息：\n");
//            sb.append("经度：" + location.getLongitude() + "\n");
//            sb.append("纬度：" + location.getLatitude() + "\n");
//            sb.append("高度：" + location.getAltitude() + "\n");
//            sb.append("速度：" + location.getSpeed() + "\n");
//            sb.append("方向：" + location.getBearing() + "\n");
//            sb.append("定位精度：" + location.getAccuracy() + "\n");

            myLatitude = String.valueOf(location.getLatitude());
            myLongitude = String.valueOf(location.getLongitude());
            myLocation = "("+myLatitude+","+myLongitude+")";

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode == RESULT_CODE && requestCode == REQUEST_CODE){
            Toast.makeText(NewspaperInfoActivity.this,"登录成功",Toast.LENGTH_SHORT).show();
            myPhone = data.getStringExtra("phone");
            isLogin = data.getBooleanExtra("isLogin",false);

            if(isLogin){

                Thread gettingThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //线程执行内容
                        //发送手机号、报纸内容、地理位置
                        //得到返回值：是否已领取，领取历史记录

                        Date curDate =  new Date(System.currentTimeMillis());
                        myTime = formatter.format(curDate);

                        gettingNewspaper = new GettingNewspaper(myNewspaper,myPhone,myLocation,myTime);

                        Log.i("领取情况",gettingNewspaper.toString());

                        String url = getResources().getString(R.string.network_url) + "record/"+myPhone+"/?name="+myNewspaper.getName()+"&jou_id="+myNewspaper.getTotalIssue();
                        Log.i("test",url);

                        InternetUtil internetUtil = new InternetUtil(url);
                        String getResult = internetUtil.getRecordMethod();
                        if(getResult != null && getResult != ""){
                            isContinue = true;
                            try {
                                JSONObject jsonObject = new JSONObject(getResult);
                                gettingHistory = jsonObject.getInt("news_num");
                                isGet = jsonObject.getBoolean("receive_state");

                                Log.i("isGet",String.valueOf(isGet));
                                if(isGet){
                                    lastGetting = new GettingNewspaper(myNewspaper,myPhone,jsonObject.getString("station"),jsonObject.getString("date"));
                                }

                            }catch (JSONException e){
                                e.printStackTrace();
                            }
                        }else {
                            isContinue = false;
                            Looper.prepare();
                            Toast.makeText(NewspaperInfoActivity.this,"访问服务器异常",Toast.LENGTH_SHORT).show();
                            Looper.loop();
                        }

                    }
                });

                if(new InternetUtil().isNetworkConnected(NewspaperInfoActivity.this)){
                    //开启线程
                    gettingThread.start();
                    try
                    {
                        gettingThread.join();
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }else {
                    isContinue = false;
                    Toast.makeText(NewspaperInfoActivity.this,"网络不可用",Toast.LENGTH_SHORT).show();
                }

                Intent intent = new Intent(NewspaperInfoActivity.this,GettingResultActivity.class);

                Log.i("isGet",String.valueOf(isGet));

                if(!isGet){
                    gettingResut = "领取成功";

                    Thread addRecordThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String url = getResources().getString(R.string.network_url)+"record/"+myPhone+"/";
                            InternetUtil internetUtil = new InternetUtil(url);

                            String gettingResult  = internetUtil.putRecordMethod(gettingNewspaper.getGettingInformation());

                            if(gettingResult == "OK"){
                                isContinue = true;
                                Looper.prepare();
                                Toast.makeText(NewspaperInfoActivity.this,"领取成功",Toast.LENGTH_SHORT).show();
                                Looper.loop();
                            }else {
                                isContinue = false;
                                Looper.prepare();
                                Toast.makeText(NewspaperInfoActivity.this,"访问服务器异常，领取失败",Toast.LENGTH_SHORT).show();
                                Looper.loop();
                            }
                        }
                    });
                    if(new InternetUtil().isNetworkConnected(NewspaperInfoActivity.this)){
                        addRecordThread.start();
//                    try {
//                        addRecordThread.join();
//                    } catch (InterruptedException e)
//                    {
//                        e.printStackTrace();
//                    }
                    }else {
                        isContinue = false;
                        Toast.makeText(NewspaperInfoActivity.this,"网络不可用",Toast.LENGTH_SHORT);
                    }

                }else {
                    gettingResut = "该用户已领取";
                    intent.putExtra("gettingInformation",lastGetting.getLastGetting());
                    isContinue = true;
                }

                Log.i("isContinue",String.valueOf(isContinue));

                if(isContinue){
                    intent.putExtra("isGet",isGet);
                    intent.putExtra("gettingResult",gettingResut);
                    intent.putExtra("gettingHistory",gettingHistory);

                    startActivity(intent);
                    this.finish();
                }
            }
        }
    }


}