package com.zp.aidlcbc;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.zp.aidlcallbackclient.R;
import com.zp.aidlcb.aidl.ICallbackService;
import com.zp.aidlcb.aidl.IJoinCallback;

public class MainActivity extends Activity {
	protected static final String TAG = "MainActivity";

	private static final String CALLBACK_SERVICE_ACTION = "com.zp.aidlcb.aidl.ICallbackService.CALLBACKSERVICE";
	// remote service
	private ICallbackService callbackService = null;

	private Button bt_callback;
	private Button bt_showresult;
	private TextView tv_output;

	private Intent intent;
	private ArrayList<String> mNames = new ArrayList<String>();
	
	/*
	 * 回调对象;由于CallbackService的一个bug，该对象的onJoin()并没有被执行
	 */
	private IJoinCallback.Stub mJoinCallback = new IJoinCallback.Stub() {

		@Override
		public void onJoin(String name, boolean joinOrLeave)
				throws RemoteException {
			if (joinOrLeave) {
				Log.i(TAG, "onJoin():" + joinOrLeave);
				mNames.add(name);
			} else {
				Log.i(TAG, "onJoin(): " + joinOrLeave);
				mNames.remove(name);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		bt_callback = (Button) findViewById(R.id.bt_callback);
		bt_showresult = (Button) findViewById(R.id.bt_showresult);
		tv_output = (TextView) findViewById(R.id.tv_output);
		intent = new Intent();
		intent.setAction(CALLBACK_SERVICE_ACTION);
		// 点击建立远程服务的连接
		bt_callback.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startService(intent);
				bindService(intent, conn, BIND_AUTO_CREATE);
				Log.i(TAG, "startService-->bindService");
			}
		});
		// 点击调用远程服务中的一些方法，需要在bt_callback点击之后
		bt_showresult.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					// 获取连接的客户端数
					List<String> result = callbackService.getJoin();
					int i = 0;
					for (String str : result)
						handleMsg(result.get(i++));
					// 调用远程服务的计算功能
					handleMsg("1 + 1 = " + callbackService.calculate(1, 1));
					// 获取回调函数中添加到mAdapter中的String变量数目
					handleMsg("callback echo :" + mNames.size());
					
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			tv_output.append(msg.getData().getString("msg"));
		}

	};

	public void handleMsg(String str) {
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("msg", str + "\n");
		msg.setData(b);
		handler.sendMessage(msg);
	}

	private ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "onServiceDisconnected()");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "onServiceConnected()");
			callbackService = ICallbackService.Stub.asInterface(service);
			try {
				callbackService.join(service, "first client");
				if(mJoinCallback == null)
					Log.i(TAG, "mJoinCallback == null");
				else{
					callbackService.registerJoinCallback(mJoinCallback);
					Log.i(TAG, "mJoinCallback ！= null and registerJoinCallback success");
				}
				
				Log.i(TAG, "callbackService.registerJoinCallback(mJoinCallback)");
			} catch (RemoteException e) {
				handleMsg("RemoteException onServiceConnected()");
				e.printStackTrace();
			}
		}
	};

}
