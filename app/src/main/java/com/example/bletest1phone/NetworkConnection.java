package com.example.bletest1phone;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)    //LOLLIPOP 버전 이상에서 동작
public class NetworkConnection extends ConnectivityManager.NetworkCallback {   // 네트워크 변경에 대한 알림에 사용되는 Callback Class

    private Context context;
    private NetworkRequest networkRequest;
    private ConnectivityManager connectivityManager;
    private Boolean connect_flag;

    public static final int TYPE_WIFI = 1;
    public static final int TYPE_MOBILE = 2;
    public static final int TYPE_NOT_CONNECTED = 3;

    public NetworkConnection(Context context){
        this.context=context;
        networkRequest =
                new NetworkRequest.Builder()                                        // addTransportType : 주어진 전송 요구 사항을 빌더에 추가
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)   // TRANSPORT_CELLULAR : 이 네트워크가 셀룰러 전송을 사용함을 나타냅니다.
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)       // TRANSPORT_WIFI : 이 네트워크가 Wi-Fi 전송을 사용함을 나타냅니다.
                        .build();
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE); // CONNECTIVITY_SERVICE : 네트워크 연결 관리 처리를 검색
    }

    public void register() {
        this.connectivityManager.registerNetworkCallback(networkRequest, this);
//        connect_flag = Boolean.FALSE;
        int tmp;
        tmp = currentNet();
        Log.d("network_connection","state : " + tmp);
    }

    public void unregister() {
        this.connectivityManager.unregisterNetworkCallback(this);
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        super.onAvailable(network);

        // 네트워크가 연결되었을 때 할 동작
        connect_flag = Boolean.TRUE;
        Toast.makeText(this.context, "network available", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLost(@NonNull Network network) {
        super.onLost(network);
        connect_flag = Boolean.FALSE;
        // 네트워크 연결이 끊겼을 때 할 동작
        Toast.makeText(this.context, "network lost", Toast.LENGTH_SHORT).show();
    }

    public boolean isConnected(){
        Log.d("network_connection","isConnected state : "+connect_flag);
        return connect_flag;
    }


    public int currentNet(){
//        Network currentNetwork = connectivityManager.getActiveNetwork();
//        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);
//        LinkProperties linkProperties = connectivityManager.getLinkProperties(currentNetwork);
//        caps.hasCapability();
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo != null){
            int type = networkInfo.getType();
            if(type == ConnectivityManager.TYPE_MOBILE){//쓰리지나 LTE로 연결된것(모바일을 뜻한다.)
                connect_flag = Boolean.TRUE;
                return TYPE_MOBILE;
            }else if(type == ConnectivityManager.TYPE_WIFI){//와이파이 연결된것
                connect_flag = Boolean.TRUE;
                return TYPE_WIFI;
            }
        }
        connect_flag = Boolean.FALSE;
        return TYPE_NOT_CONNECTED;  //연결이 되지않은 상태
    }


}