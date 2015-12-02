package com.zp.aidlcbc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zp.aidlcallbackclient.R;
import com.zp.aidlcb.aidl.ICallbackService;
import com.zp.aidlcb.aidl.IJoinCallback;

public class MainActivity extends Activity implements OnClickListener {
	protected static final String TAG = "MainActivity";
	// 开启服务的Action
	private static final String CALLBACK_SERVICE_ACTION = "com.zp.aidlcb.aidl.ICallbackService.CALLBACKSERVICE";
	// remote service
	private ICallbackService callbackService = null;
	private boolean isBinded = false;
	private boolean isRegistered = false;
	private IBinder mToken = new Binder();
	private Random mRand = new Random();

	private Button bt_bind_togger;
	private Button bt_register_togger;
	private Button bt_join;
	private Button bt_leave;
	private Button bt_callremote;
	private Button bt_getclients;
	private TextView tv_output;

	private Intent intent;
	private ArrayList<String> mNames = new ArrayList<String>();

	// Client的一个回调函数
	private IJoinCallback.Stub mJoinCallback = new IJoinCallback.Stub() {
		@Override
		public void onJoin(String name, boolean joinOrLeave)
				throws RemoteException {
			// Client的回调函数在Service端可以在任意的时刻调用
			// 此处仅仅以加入或者离开的时候进行回调
			if (joinOrLeave) {
				handleMsg("Client "+name +"callback is called"+joinOrLeave);
				mNames.add(name);
			} else {
				handleMsg("Client "+name +"callback is called"+joinOrLeave);
				mNames.remove(name);
			}
		}
	};

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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		bt_bind_togger = (Button) findViewById(R.id.bt_bind_togger);
		bt_register_togger = (Button) findViewById(R.id.bt_register_togger);
		bt_join = (Button) findViewById(R.id.bt_join);
		bt_leave = (Button) findViewById(R.id.bt_leave);
		bt_callremote = (Button) findViewById(R.id.bt_callremote);
		bt_getclients = (Button) findViewById(R.id.bt_getclients);
		tv_output = (TextView) findViewById(R.id.tv_output);
		
		intent = new Intent();
		intent.setAction(CALLBACK_SERVICE_ACTION);
		
		bt_bind_togger.setOnClickListener(this);
		bt_register_togger.setOnClickListener(this);
		bt_join.setOnClickListener(this);
		bt_leave.setOnClickListener(this);
		bt_callremote.setOnClickListener(this);
		bt_getclients.setOnClickListener(this);
	}

	// 远程连接，获取远程服务
	private ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			callbackService = null;
		}
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			callbackService = ICallbackService.Stub.asInterface(service);
		}
	};
	
	// Activity销毁之后解除服务的绑定
	@Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBinded) {
            unbindService(conn);
            callbackService = null;
        }
    }
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bt_bind_togger:
			// 服务的绑定和解绑
			if (isBinded) {
				unbindService(conn);
				bt_bind_togger.setText("bind");
				isBinded = false;
				callbackService = null;
				handleMsg("Service unbind");
			}else{
				startService(intent);
				bindService(intent, conn, BIND_AUTO_CREATE);
				bt_bind_togger.setText("unbind");
				isBinded = true;
				handleMsg("Service binded");
			}
			break;
		case R.id.bt_register_togger:
			// 为当前的Client注册一个回调函数，该回调函数会在CallbackService的join和leave函数中被调用
			// 其他调用时机可以在CallbackService中自行添加
			try {
				if (isServiceReady()) {
					if (isRegistered) {
						callbackService.unregisterJoinCallback(mJoinCallback);
						bt_register_togger.setText("register");
						isRegistered = false;
					} else {
						callbackService.registerJoinCallback(mJoinCallback);
						bt_register_togger.setText("unregister");
						isRegistered = true;
					}
				}
			} catch (RemoteException e) {
				handleMsg("bt_register_togger RemoteException");
				e.printStackTrace();
			}
			break;
		case R.id.bt_join:
			// Client加入
			try {
				if (isServiceReady()) {
					String tempClient = "Client " + mRand.nextInt(100);
					callbackService.join(mToken, tempClient);
					handleMsg("Client " + tempClient + " join");
				}
			} catch (RemoteException e) {
				handleMsg("bt_join RemoteException");
				e.printStackTrace();
			}
			break;
		case R.id.bt_leave:
			// Client离开，但是Service并没有设置为null，只是将其从client列表中移除
			// 实现见CallbackService的leave函数
			try {
				if (isServiceReady()) {
					callbackService.leave(mToken);
					handleMsg("Client leave");
				}
			} catch (RemoteException e) {
				handleMsg("bt_leave RemoteException");
				e.printStackTrace();
			}
			break;
		case R.id.bt_callremote:
			// 获取远程的一个简单的方法调用，此处我一个二元运算
			try {
				if (isServiceReady()) {
					int result = callbackService.calculate(1, 2);
					handleMsg("1 + 2 = " + result);
				}
			} catch (RemoteException e) {
				handleMsg("bt_callremote RemoteException");
				e.printStackTrace();
			}
			break;
		case R.id.bt_getclients:
			// 获取所有连接的Client名字
			try {
				if (isServiceReady()) {
					List<String> names = callbackService.getJoin();
					handleMsg("connected clients :");
					for(int i=0;i<names.size();i++)
						handleMsg("client "+(i+1)+ names.get(i));
				}
			} catch (RemoteException e) {
				handleMsg("bt_getclients RemoteException");
				e.printStackTrace();
			}
			break;
		}
	
	}
	
	/**
	 * 判断当前远程服务是否可用
	 * 
	 * @return true可用，否则不可用
	 */
	private boolean isServiceReady() {
        if (callbackService != null) {
            return true;
        } else {
            handleMsg("Service is not available yet! please bind it first");
            return false;
        }
    }

}
