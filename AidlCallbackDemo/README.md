# AIDLCallbackDemo  

Service和Client之间互相调用函数的demo，演示基本的回调机制  
演示流程：  
1. bind：绑定远程服务  
2. register：注册Client回调函数  
3. join：Client加入到Service的列表中，同时Service回调Client注册的回调函数  
4. call remote func：Client调用Service提供的一个简单计算函数并显示计算结果  
5. get connected clients：输出Service端所有连接的Client
6. leave：当前Client从Service的列表移除，但是Service并有断开连接   

当上述基本流程执行完之后，点击unbind，再点击call remote func的时候
由于连接已经断开，所以提示需要先建立与远程服务的连接  

Client的执行结果：  

![](http://i.imgur.com/hpYMHkJ.png)  


![](http://i.imgur.com/T4z3NjW.png)  

Service端的Log输出：  

![](http://i.imgur.com/aN16roH.png)
