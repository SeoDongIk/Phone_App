/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bletest1phone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Controller {
    // Debugging
    private static final String TAG = "BLE_Controller";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothMulti";
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    private ArrayList<String> mDeviceAddresses; // 연결된 장치들의 mac 주소 list
    private ArrayList<ConnectedThread> mConnThreads;
    private ArrayList<BluetoothSocket> mSockets; // 연결된 socket list
    /**
     * A bluetooth piconet can support up to 7 connections. This array holds 7 unique UUIDs.
     * When attempting to make a connection, the UUID on the client must match one that the server
     * is listening for. When accepting incoming connections server listens for all 7 UUIDs. 
     * When trying to form an outgoing connection, the client tries each UUID one at a time. 
     */
//    private ArrayList<UUID> mUuids; // 연결 고유 식별을 위한 uuid list
//    private int uuid_number;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public Controller(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE; // 초기 상태
        mHandler = handler;
        mDeviceAddresses = new ArrayList<String>();
        mConnThreads = new ArrayList<ConnectedThread>();
        mSockets = new ArrayList<BluetoothSocket>();

    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state; // 현재 상태를 새 상태로 업데이트

        // Give the new state to the Handler so the UI Activity can update
        // state가 바꼈다고 main activity에 알림
        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState; // 현재 상태 얻기
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() { // controller 생성후, 처음에 시작할때 호출됨
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        // 연결 시도중인 thread 삭제
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

//        // Cancel any thread currently running a connection
//        // 연결되어 있는 thread 삭제
//        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    /*
    * MainActivity에서 controller 객체 생성후, start method 호출하여 3개의 thread 초기화 (connect, connected, accept)
    * 이후, 리스트에서 선택한 장치의 mac 주소를 활용해 해당 장치를 얻어 이 connect method를 호출해 전달. (해당 장치와 연결을 위해)
    * */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        // 현재 연결중인 connection이 있다면 & Connect thread가 null이 아니라면 -> Connect thread 초기화
        // 연결중인 connection이 있으면 그걸 취소하고 지금껄 진행시킴.
        // -> 이때 uuid 등 기존에 할당된 것들은 어떻게 할지 찾아봐야 함.
        // -> socket만 close할게 아니라, mac 주소 list, connthread list 그리고 socket list는 어떻게 되는지 알아봐야함.
        // -> 타임라인 제작 필요.
        if (mState == STATE_CONNECTING) { // 연결중
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

//        // Cancel any thread currently running a connection
//        // 이미 connected thread list에 들어가서 삭제돼도 괜찮음.
//        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        // 얘도 삭제되어야 함.

        // Create a new thread and attempt to connect to each UUID one-by-one.
        // 연결은 전달된 장치와 단 하나만.
        // uuid 번호를 전역 변수로 설정하여 관리 필요.
        try {
            mConnectThread = new ConnectThread(device);
            mConnectThread.start();
            setState(STATE_CONNECTING);
        } catch (Exception e) {

        }

    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        //Commented out all the cancellations of existing threads, since we want multiple connections.

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
//        // Cancel any thread currently running a connection
//        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        // 얘 지워야함


        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        // Add each connected thread to an array
        mConnThreads.add(mConnectedThread); // 여기에 넣었으므로, connectedThread = null 처리해도 됨.
        // ********************************************************************** //
//        mConnThreads.get(mConnThreads.size()-1).start(); // 방금 넣은 thread start

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_OBJECT);
        Bundle bundle = new Bundle();
        bundle.putParcelable(MainActivity.DEVICE_OBJECT, device);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    // 연결된 모든 기기들에 전송
    // 딱히 문제는 없는듯함.
    public void write(byte[] out) {
        // When writing, try to write out to all connected threads

        if(mConnThreads.size() == 0) { // 연결된 장치가 없을경우
            // Send a failure message back to the Activity
            Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(MainActivity.TOAST, "Can't write.\nConnected threads : 0");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
        else { // // 연결된 장치가 있을경우
            for (int i = 0; i < mConnThreads.size(); i++) {
                try {
                    // Create temporary object
                    ConnectedThread r;
                    // Synchronize a copy of the ConnectedThread
                    synchronized (this) {
//                        if (mState != STATE_CONNECTED) return;
                        r = mConnThreads.get(i);
                    }
                    // Perform the write unsynchronized
                    r.write(out);
                } catch (Exception e) {
                }
            }
        }


    }





    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */

    /*
     * 연결을 수락하는 원격 기기와의 연결을 시작하려면 원격 기기를 나타내는 BluetoothDevice 객체가 필요
     * BluetoothDevice를 사용해 BluetoothSocket을 가져와 연결을 시작함.
     * BluetoothDevice를 통해 createRFcommSocketToServiceRecord(UUID)를 호출하여 BluetoothSocket을 가져옴
     * -> BluetoothSocket 객체를 초기화.
     * -> BluetoothSocket : BluetoothDevice에 연결하도록 허용하는 객체
     * UUID는 서버 기기가 listenUsingRFcommWithServiceRecord(String, UUID)를 호출하여
     * BluetoothServerSocket을 열 때 사용한 UUID와 일치해야 함.
     *
     * */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                // bluetooth device를 통해 bluetooth socket을 가져옴
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            // conect() 호출 전에 항상 cancelDiscovery() 호출 필요
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                // 연결 시작
                // 시스템에 SDP 조회 실행 -> 일치하는 UUID를 포함한 원격 기기 찾음
                // 조회 성공 & 원격 기기가 연결을 수락 -> 원격 기기가 연결하는 동안 사용할 RFCOMM 채널 공유 및 connect() 반환
                // 연결 실패 || method 시간 초과 -> IOException
                mmSocket.connect();

            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            // 이건 왜 하늘걸까
            // connected method를 먼저 실행하고 null을 설정해야 하는거 아닌가?
            synchronized (Controller.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    /*
    * connected thread는 장치와 연결중일 때 작동하는 thread.
    * 들어오는 transmissions(read)과 나가는 transmissions(write) 관리
    * */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;
        private final InputStream mmInStream;
        private final String mmNameAddress;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");

            mmSocket = socket;
            mmNameAddress = socket.getRemoteDevice().getName()+"\n"+socket.getRemoteDevice().getAddress();
            OutputStream tmpOut = null;
            InputStream tmpIn = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpOut = socket.getOutputStream();
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmOutStream = tmpOut;
            mmInStream = tmpIn;
        }

        public void run() { // 읽을 필요는 없지만, connection lost는 알아차려야 함.
            Log.i(TAG, "BEGIN mConnectedThread");

            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost(mmNameAddress);
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        
        public String getNameAddress() {
            return mmNameAddress;
        }

        public void cancel() {
            try {
                Log.d(TAG,mmSocket.getRemoteDevice().getName()+" - socket close");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        mConnThreads.clear();
        // mac 주소 list, multi connected thread list 모두 제거해야 함.
        setState(STATE_NONE);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     * 연결을 잃었을때 대처 어떻게 해야할지? -> 여러 연결들 중 하나의 연결을 lost
     */
    private void connectionLost(String nameAddress) {
//        setState(STATE_LISTEN);

        Log.d(TAG,"connectionLost");
//
        // Main에 끊긴 device 전달
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DISCONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME_ADDRESS, nameAddress);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // mConnThread에서 연결이 끊긴 thread 찾아 삭제
        // connectionLost 상태에서 되나?
        for (int i=0;i<mConnThreads.size();i++){
            // 현재 index에 해당하는 connected thread의 socket이 지금 연결이 끊긴 socket과 같다면
            if(mConnThreads.get(i).getNameAddress() == nameAddress) {
                Log.d(TAG,"connectionLost : find index about mConnThreads");
//                mConnThreads.get(i).cancel(); // socket 닫기 -> 앱 종료될때 이미 socket close 된듯
                mConnThreads.remove(i); // connected thread list에서 제거
                break;

            }
        }

        if (mConnThreads.size()==0){
            setState(STATE_LISTEN);
        }
        else {
            setState(STATE_CONNECTED);
        }

    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     * 연결에 실패했을때 대처 어떻게 해야할지? -> 여러 연결들 중 하나의 연결을 failed
     */
    private void connectionFailed() {
        if (mConnThreads.size()==0){
            setState(STATE_LISTEN);
        }
        else {
            setState(STATE_CONNECTED);
        }
//        setState(STATE_LISTEN);
        // Commented out, because when trying to connect to all 7 UUIDs, failures will occur
        // for each that was tried and unsuccessful, resulting in multiple failure toasts.

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "ConnectionFailed\nUnable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Multi 연결이라 controller를 다시 시작하면 안되는 줄 알았는데,
        // start() method는 단순히 현재 존재하는 connect thread와 connected thread를
        // cancel하고 null로 초기화 하는 것이라 start 다시 해도 될 것 같다.
        // 이미 연결중인 애들은 mConnThread에 저장되어 있기 때문에!
        Controller.this.start();

    }

}