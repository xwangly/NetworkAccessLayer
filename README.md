# NetworkAccessLayer
网络接入层框架，支持Http and Socket

NetExecutor: 网络层接口，提供如下方法
    RESPONSE sendRequest(REQUEST request) throws NetException;

    void sendRequest(REQUEST request, NetCallback<RESPONSE> listener);

    void cancelRequest(Object tag);

    void shutDown();

Engine:网络引擎，真正做网络请求的，可以自定义网络引擎
    ENGINERESPONSE performRequest(EngineRequest<REQUEST> request) throws NetException;

    void cancelRequest(EngineRequest<REQUEST> request) throws NetException;

    void shutDown();

NetResponseParser：结果解析接口，网络引擎将网络响应的数据解析成一个通用的对象，用户实现自己的parser方法，将通用对象解析成用户需要的对象。



网络接入层基于JDK实现的，因此可以脱离Android环境运行。


HTTP：
1.DefalutHttpExecutor  

网络层接口，提供同步或异步执行HttpRequest，也可以自己实现一个Http执行器。

2.DefaultHttpEngine

默认的Http引擎，也可以自己实现引擎

3.DefaultHttpRequest 
默认的HttpRequest，需要自己实现parser接口，将HttpEngineResponse解析成自己想要的RESPONSE对象

HTTP示例可参照MainActivity的用法

Socket:
可以通过扩展SocketExecutor接口实现Socket操作
需要实现三个方法
1.SocketEngineResponse parseResponse(Socket socket, InputStream is)
解析Socket输入流，读取流中数据，并解析成SocketEngineResponse对象

2.<ENGINERESPONSE extends EngineResponse> RESPONSE parseToNetResponse(ENGINERESPONSE engine_response)
将第一步中解析的SocketEngineResponse对象转换成我们自定义的对象(注：这里的入参可以直接强转成SocketEngineResponse)

3.void onPush(RESPONSE response) 
推送消息，可以选择实现，如果需要接收推送消息，实现该方法即可。
第一步中解析SocketEngineResponse对象时，由自己判断是不是推送消息。
