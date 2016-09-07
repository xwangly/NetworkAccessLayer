/**
 * Created by xwangly on 2016/9/7.
 */
public class fds {
    # NetworkAccessLayer
    网络接入层框架，支持Http与Socket,支持同步与异步，支持取消操作。

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

        2.ENGINERESPONSE extends EngineResponse> RESPONSE parseToNetResponse(ENGINERESPONSE engine_response)
        将第一步中解析的SocketEngineResponse对象转换成我们自定义的对象,(注：这里的入参可以直接强转成SocketEngineResponse),

        3.void onPush(RESPONSE response)
        推送消息，可以选择实现，如果需要接收推送消息，实现该方法即可。

    第一步中解析SocketEngineResponse对象时，由自己判断是不是推送消息。


    备注：

    这套网络框架是我在Android项目中使用的，我们的项目有一个HTTP接入层和一个Socket接入层，通过一年多的迭代，从而实现了该框架。
    我们的HTTP接入层的引擎使用了OKHttp框架实现，也可以使用项目中的DefaultHttpEngine实现。


    有人可能会问，我们公司有一套自己的Socket协议，能否使用这套框架吗？
    需要满足一个条件：你们的协议中发送的请求中的序列号在响应流中会返回吗？如果会，就可以使用这套框架



    这套框架参考并吸收了OKHttp和Volley的一些精要东西，

        比如参数了OKHttp的Dispatcher设计，从而可以同时支持同步和异步的网络请求；

        参数了Volley框架的Request设计，从而可以让用户得到自己想要的RESPONSE对象；

        参数了Volley框架的日志设计，从而可以直观的看到一个请求的执行过程和执行时间。




    之所以写这套网络接入框架，是源于我们的项目需要

        我想要一套基于JDK的网络框架；

        我想要一套简单好用的Socket的框架

        我想要清楚的看到请求的参数，响应的结果集

        我想要一套通用的能指定响应结果类型的网络框架，

        我想要一套能取消请求的网络框架

        我想要。。。



    如果使用中遇到什么问题或对框架有建议的请与我联系：

        github:xwangly

        tencent:605134012

}
