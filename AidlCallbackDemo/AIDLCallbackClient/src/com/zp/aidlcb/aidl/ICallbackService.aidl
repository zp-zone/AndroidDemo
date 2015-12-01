package com.zp.aidlcb.aidl;

import com.zp.aidlcb.aidl.IJoinCallback;

interface ICallbackService{
	int calculate(int a, int b);

    void join(IBinder token, String name);
    void leave(IBinder token);
    List<String> getJoin();
    
	void registerJoinCallback(IJoinCallback callback);
	void unregisterJoinCallback(IJoinCallback callback);
}