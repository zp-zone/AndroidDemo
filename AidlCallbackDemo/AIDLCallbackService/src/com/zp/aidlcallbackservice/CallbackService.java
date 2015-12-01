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
 * Զ�̷����࣬�ṩ�ص�ע�᷽�����Լ�Զ�����ӷ������Ͽ��Ĵ��� 
 *
 */
public class CallbackService extends Service {
	private static final String TAG = "CallbackService";

	// �ص��������飬ÿ�����ӵ�Clientע��һ��IJoinCallback���͵Ļص��ӿ�
	private RemoteCallbackList<IJoinCallback> mCallbacks = new RemoteCallbackList<IJoinCallback>();
	// ���ӵ�Client����
	private List<Client> mClients = new ArrayList<Client>();

	private final ICallbackService.Stub mBinder = new ICallbackService.Stub() {

		@Override
		public void unregisterJoinCallback(IJoinCallback callback)
				throws RemoteException {
			mCallbacks.unregister(callback);
			Log.i(TAG, "unregisterJoinCallback()");
		}

		@Override
		public void registerJoinCallback(IJoinCallback callback)
				throws RemoteException {
			mCallbacks.register(callback);
			Log.i(TAG, "registerJoinCallback()");
		}

		// ��ȡ�����Ѿ����ӵĿͻ��˵�����
		@Override
		public List<String> getJoin() throws RemoteException {
			ArrayList<String> names = new ArrayList<String>();
			for (Client client : mClients) {
				names.add(client.mName);
			}
			Log.i(TAG, "getJoin()");
			return names;
		}

		// һ���򵥵�Զ�̼��㹦�ܽӿں���
		@Override
		public int calculate(int a, int b) throws RemoteException {
			Log.i(TAG, "calculate()");
			return a + b;
		}

		// ���ӽ���
		@Override
		public void join(IBinder token, String name) throws RemoteException {
			Log.i(TAG, "join()");
			int idx = findClient(token);
			if (idx >= 0) {
				Log.i(TAG, "already joined");
				return;
			}

			Client client = new Client(token, name);
			// // ע��ͻ��˶Ͽ����ӵĵ�֪ͨ����RemoteCallbackList<E extends IInterface>���Դ�����еȼ�ʵ��
			// // ���Բο���Դ���ʵ��
			// token.linkToDeath(client, 0);
			mClients.add(client);
			// ����֪ͨ�¼���Ч��ͬ token.linkToDeath
			notifyParticipate(client.mName, true);
		}
		
		// ��������������Client�뿪
		@Override
		public void leave(IBinder token) throws RemoteException {
			Log.i(TAG, "leave()");
			int idx = findClient(token);
			if (idx < 0) {
				Log.i(TAG, "already left");
				return;
			}

			Client client = mClients.get(idx);
			mClients.remove(client);
			// // ȡ��ע��
			// client.mToken.unlinkToDeath(client, 0);
			// �����Ͽ����ӵ�֪֪ͨͨ�¼�
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
		// Service���ٵ�ʱ�������mCallbacks�еĻص�����
		mCallbacks.kill();
	}

	/**
	 *  �������ӵĿͻ��ˣ�����ֻ�ܲ���һ���ͻ��ˣ������ͻ��˵����Ӽ���ѯ��������
	 *  
	 * @param token
	 * @return
	 */
	private int findClient(IBinder token) {
		Log.i(TAG, "findClient()");

		for (int i = 0; i < mClients.size(); i++) {
			if (mClients.get(i).mToken == token) {
				return i;
			}
		}
		return -1;
	}

	/**
	 *  ����֪ͨ�¼�
	 *  
	 *  �ú���������һ��Сbug������mCallbacks.beginBroadcast()���ص�����0
	 *  ����mCallbacks.register()������ʾע��ɹ��˵ġ��������ǿ���Դ���˽���ϸ��
	 *  
	 * @param name
	 * @param joinOrLeave Client�ǽ������ӻ��ǶϿ�����
	 */
	private void notifyParticipate(String name, boolean joinOrLeave) {
		int len = 0;
		try {
			len = mCallbacks.beginBroadcast();
		} catch (Exception e) {
			Log.i(TAG, " mCallbacks.beginBroadcast() exception");
			e.printStackTrace();
		}
		if (len > 0) {
			for (int i = 0; i < len; i++) {
				try {
					// ֪ͨ�ص�
					mCallbacks.getBroadcastItem(i).onJoin(name, joinOrLeave);
					Log.i(TAG, "mCallbacks.getBroadcastItem(i).onJoin(name, joinOrLeave)");
				} catch (RemoteException e) {
					Log.i(TAG, "notifyParticipate() RemoteException");
					e.printStackTrace();
				}
			}
			mCallbacks.finishBroadcast();
			Log.i(TAG, "notifyParticipate()");
		} else {
			Log.i(TAG, "notifyParticipate() has not executed, mCallbacks.beginBroadcast() == 0");
		}
	}

	/**
	 *  Client�࣬��Ҫ������IBinder��mToken����ʵ������Ͽ�����ʱ���֪ͨ
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
			// �ͻ�������Ͽ����ӣ�ִ�д˻ص�
			int index = mClients.indexOf(this);
			if (index < 0) {
				return;
			}

			Log.i(TAG, "client died: " + mName);
			mClients.remove(this);
		}
	}
}
