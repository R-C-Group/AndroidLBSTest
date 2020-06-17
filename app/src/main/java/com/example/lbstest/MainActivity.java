package com.example.lbstest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView positionText;

    public LocationClient mLocationClient;

    private MapView mapView;

    private BaiduMap baiduMap;
    private boolean isFirstLocate=true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SDKInitializer.initialize(getApplicationContext());//先通过initialize方法来进行初始化操作
        //initialize() 方法接收一个Context 参数
        // 这里我们调用getApplicationContext() 方法来获取一个全局的Context 参数并传入
        //注意初始化操作一定要在setContentView() 方法前调用，不然的话就会出错

        setContentView(R.layout.activity_main);

        //构建一个LocationClient的实例
        mLocationClient=new LocationClient(getApplicationContext());//LocationClient的构建函数接收一个context参数
        //调用getApplicationContext() 方法来获取一个全局的Context 参数并传入

        //调用registerLocationListener方法来注册一个定位的监听器。当获取到位置信息的时候，就会回调这个定位监听器。
        mLocationClient.registerLocationListener(new MyLocationListener());

        positionText=(TextView) findViewById(R.id.position_text_view);

        mapView=(MapView) findViewById(R.id.bmapView);//找到MapView实例

        //百度LBS SDK的API中提供了一个BaiduMap 类，它是地图的总控制器，
        // 调用MapView的getMap()方法就能获取到BaiduMap 的实例
        baiduMap = mapView.getMap();

        //如果要在地图上显示自己的位置，需要先调用下面方法来开启这个功能
        baiduMap.setMyLocationEnabled(true);



        //////////***************权限********************
        //如果没有启动下面权限，就询问用户让用户打开
        List<String> permissionList=new ArrayList<>();
        //创建一个空的list集合，然后依次判断这3个权限有没有被授权
        //如果没被授权就添加到List集合中
        //最后将List转换成数组

        //采用运行时权限。对于危险权限是需要进行运行时权限处理的
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            //由于ACCESS_FINE_LOCATION和ACCESS_COARSE_LOCATION属于同一个权限组，只需申请一个即可
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED)
        {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);//将List转换成数组
            //调用ActivityCompat.requestPermissions() 方法一次性申请
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }
        else {
            requestLocation();
        }

    }



    //监听线程，获得当前的经纬度，并显示
    public class MyLocationListener implements BDLocationListener{

        @Override
        public void onReceiveLocation(final BDLocation bdLocation) {

            if(bdLocation.getLocType()==BDLocation.TypeNetWorkLocation||bdLocation.getLocType()==BDLocation.TypeGpsLocation){
                navigateTo(bdLocation);
            }

            runOnUiThread(new Runnable() {//不能在主线程(UI线程)以外操作UI，android中把耗时的操作都放到子线程里面
                @Override
                public void run() {
                    StringBuilder currentPosition = new StringBuilder();//一个可变的字符序列
                    currentPosition.append("纬度：").append(bdLocation.getLatitude()).append("\n");
                    currentPosition.append("经线：").append(bdLocation.getLongitude()).append("\n");

                    //获取地址信息一定需要用到网络
                    // 因此即使我们将定位模式指定成了Device_Sensors，也会自动开启网络定位功能。
                    currentPosition.append("国家：").append(bdLocation.getCountry()).append("\n");
                    currentPosition.append("省：").append(bdLocation.getProvince()).append("\n");
                    currentPosition.append("市：").append(bdLocation.getCity()).append("\n");
                    currentPosition.append("区：").append(bdLocation.getDistrict()).append("\n");
                    currentPosition.append("街道：").append(bdLocation.getStreet()).append("\n");
                    currentPosition.append("定位方式：");
                    if (bdLocation.getLocType()==BDLocation.TypeGpsLocation){
                        currentPosition.append("GPS");
                    }else if (bdLocation.getLocType()==BDLocation.TypeNetWorkLocation)
                    {
                        currentPosition.append("网络");
                    }
                    positionText.setText(currentPosition);

                }
            });
        }
    }

    private void navigateTo(BDLocation location){
        //先是将BDLocation 对象中的地理位置信息取出并封装到LatLng 对象中
        if(isFirstLocate){
            //************将地图调到当前位置**********
            LatLng ll=new LatLng(location.getLatitude(),location.getLongitude());//用于存放经纬度
            //调用MapStatusUpdateFactory的newLatLng() 方法将LatLng 对象传入，
            // newLatLng() 方法返回的也是一个MapStatusUpdate 对象
            MapStatusUpdate update= MapStatusUpdateFactory.newLatLng(ll);
            // 再把这个对象传入BaiduMap的animateMapStatus() 方法当中，就可以将地图移动到指定的经纬度上了
            baiduMap.animateMapStatus(update);

            //**************将地图缩放*******
            //MapStatusUpdateFactory的zoomTo() 方法接收一个float 型的参数，就是用于设置缩放级别的
            update=MapStatusUpdateFactory.zoomTo(19f);
            //zoomTo() 方法返回一个MapStatusUpdate 对象，我们把这个对象传入BaiduMap的animateMapStatus() 方法当中即可完成缩放功能。
            baiduMap.animateMapStatus(update);

            isFirstLocate=false;//只有首次需要做初始化设置
        }

        //***********将自己当前的位置显示出来**********
        //MyLocationData.Builder 类，这个类是用来封装设备当前所在位置的，只需将经纬度信息传入到这个类的相应方法当中就可以了
        MyLocationData.Builder locationBuilder=new MyLocationData.Builder();
        locationBuilder.latitude(location.getLatitude());
        locationBuilder.longitude(location.getLongitude());
        //调用MyLocationData.Builder 类所提供的build() 方法，
        // 就会生成一个MyLocationData的实例
        MyLocationData locationData=locationBuilder.build();
        //再将这个实例传入到BaiduMap的setMyLocationData() 方法当中，就可以让设备当前的位置显示在地图上了
        baiduMap.setMyLocationData(locationData);


    }

    //*初始化函数，并启动位置客户端LocationClient*/
    private void requestLocation() {
        initLocation();
        //默认情况下，调用LocationClient的start() 方法只会定位一次
        mLocationClient.start();//调用方法来启动定位。定位的结果会反馈到上面所注册的监听器MyLocationListener中
    }

    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(5000);//调用setScanSpan方法设置更新的间隔（每5秒会更新一下当前的位置）
        //调用了setLocationMode() 方法来将定位模式指定成传感器模式，也就是说只能使用GPS进行定位
        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        option.setIsNeedAddress(true);//表示我们需要获取当前位置详细的地址信息。
        mLocationClient.setLocOption(option);
    }

    //只有同意打开相关权限才可以开启本程序
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults){
        //通过一个循环将申请的每个权限都进行了判断，如果有任何一个权限被拒绝
        // 那么就直接调用finish() 方法关闭当前程序，
        // 只有当所有权限都被用户同意了，才会调用requestLocation() 方法开始地理位置定位
        switch(requestCode){
            case 1:
                if (grantResults.length>0){
                    for (int result:grantResults){
                        if (result !=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"必须同意所有权限才能使用本程序",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else{
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }



    //这个方法在活动准备好和用户进行交互的时候调用。
    // 此时的活动一定位于返回栈的栈顶，并且处于运行状态。
    @Override
    protected void onResume(){
        super.onResume();
        mapView.onResume();
    }

    //这个方法在系统准备去启动或者恢复另一个活动的时候调用。
    // 通常会在这个方法中将一些消耗CPU的资源释放掉，以及保存一些关键数据，
    // 但这个方法的执行速度一定要快，不然会影响到新的栈顶活动的使用。
    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
    }

    //在活动被销毁的时候一定要调用LocationClient 的stop() 方法来停止定位，
    //不然程序会持续在后台不停地进行定位，从而严重消耗手机的电量。
    @Override
    protected void onDestroy(){
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);//程序退出时，关闭在地图上显示自己位置的功能
    }


}
