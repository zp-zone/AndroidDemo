package com.zp.aidlcallbackservice;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.zp.aidlcb.aidl.ICallbackService;
import com.zp.aidlcb.aidl.IJoinCallback;

/**
 * 远程服务类，提供回调注册方法，以及远程连接非正常断开的处理 
 *
 */
public class CallbackService extends Service {
	private static final String TAG = "CallbackService";

	// 回调函数数组，每个连接的Client注册一个IJoinCallback类型的回调接口
	private RemoteCallbackList<IJoinCallback> iCallbacks = new RemoteCallbackList<IJoinCallback>();
	// 连接的Client数组
	private List<Client> mClients = new ArrayList<Client>();

	private final ICallbackService.Stub mBinder = new ICallbackService.Stub() {

		// Client取消注册的接口
		@Override
		public void unregisterJoinCallback(IJoinCallback callback)
				throws RemoteException {
			iCallbacks.unregister(callback);
			Log.i(TAG, "unregisterJoinCallback()");
		}

		// Client注册回调函数的接口
		@Override
		public void registerJoinCallback(IJoinCallback callback)
				throws RemoteException {
			iCallbacks.register(callback);
			Log.i(TAG, "registerJoinCallback()");
		}

		// 获取所有已经连接的客户端的名字
		@Override
		public List<String> getJoin() throws RemoteException {
			ArrayList<String> names = new ArrayList<String>();
			for (Client client : mClients) {
				names.add(client.mName);
				Log.i(TAG, client.mName + " : getJoin()");
			}
			return names;
		}

		// 一个简单的远程计算功能接口函数
		@Override
		public int calculate(int a, int b) throws RemoteException {
			Log.i(TAG, "calculate() called");
			return a + b;
		}

		// Client加入，将其加入到mClients并通过notifyParticipate()执行注册的回调函数
		@Override
		public void join(IBinder token, String name) throws RemoteException {
			int idx = findClient(token);
			if (idx >= 0) {
				Log.i(TAG, "already joined");
				return;
			}

			Client client = new Client(token, name);
			// // 注册客户端断开连接的的通知；在RemoteCallbackList<E extends IInterface>类的源码中有等价实现
			// // 可以参考下源码的实现
			token.linkToDeath(client, 0);
			mClients.add(client);
			// 发出通知事件；效果同 token.linkToDeath
			notifyParticipate(client.mName, true);
		}

		// Client正常离开，从mClients列表中将其移除，然后回调其注册的回调函数
		@Override
		public void leave(IBinder token) throws RemoteException {
			int idx = findClient(token);
			if (idx < 0) {
				Log.i(TAG, "already left");
				return;
			}

			Client client = mClients.get(idx);
			mClients.remove(client);
			// // 取消注册
			client.mToken.unlinkToDeath(client, 0);
			// 发出断开连接的通知通知事件
			notifyParticipate(client.mName, false);
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy()");
		// Service销毁的时候清除掉mCallbacks中的回调对象
		iCallbacks.kill();
	}

	/**
	 * 查找已经连接的Client所在下标
	 * 
	 * @param token
	 * @return
	 */
	private int findClient(IBinder token) {
		for (int i = 0; i < mClients.size(); i++) {
			if (mClients.get(i).mToken == token) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 发出通知事件
	 * 
	 * 该函数还存在一个小bug，就是mCallbacks.beginBroadcast()返回的总是0
	 * 但是mCallbacks.register()又是显示注册成功了的。后续还是看看源码了解下细节
	 * 
	 * BUG原因：client需要先register注册函数之后才可以调用join()方法
	 * 因为join方法中的notifyParticipate()会使用到mCallbacks
     * 而mCallbacks是在register中添加变量unregister中移除变量的
     * 
	 * if the target IBinder's process has already died
	 * 
	 * 源码回调注册的的实现：RemoteCallbackList的register(E callback, Object cookie)方法
	 * 
	 * @param name
	 * @param joinOrLeave
	 *            Client是建立连接还是断开连接
	 */
	private void notifyParticipate(String name, boolean joinOrLeave) {
		int len = iCallbacks.beginBroadcast();
		for (int i = 0; i < len; i++) {
			try {
				// 通知回调
				iCallbacks.getBroadcastItem(i).onJoin(name, joinOrLeave);
				Log.i(TAG, " callback client : " + name + " onJoin()");
			} catch (RemoteException e) {
				Log.i(TAG, "notifyParticipate() RemoteException" + joinOrLeave);
				e.printStackTrace();
			}
		}
		iCallbacks.finishBroadcast();
	}

	/**
	 * Client类，主要是用其IBinder的mToken对象实现意外断开连接时候的通知
	 * 
	 */
	private final class Client implements IBinder.DeathRecipient {
		public final IBinder mToken;
		public final String mName;

		public Client(IBinder token, String name) {
			mToken = token;
			mName = name;
		}

		@Override
		public void binderDied() {
			// 客户端意外断开连接，执行此回调
			int index = mClients.indexOf(this);
			if (index < 0) {
				return;
			}

			Log.i(TAG, "client died: " + mName);
			mClients.remove(this);
			notifyParticipate(mName, false);
		}
	}
	
}
