package com.cjsc.networkaccesslayer;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.xwang.net.NetCallback;
import com.xwang.net.NetException;
import com.xwang.net.NetLog;
import com.xwang.net.http.DefalutHttpExecutor;
import com.xwang.net.http.DefaultHttpEngine;
import com.xwang.net.http.HttpExecutor;
import com.xwang.net.http.HttpRequest;
import com.xwang.net.http.StringRequest;

import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        NetLog.setLogPrinter(new NetLog.LogPrinter() {
            @Override
            public boolean logable() {
                return true;
            }

            @Override
            public void d(String tag, String msg) {
                Log.d(tag, msg);
            }

            @Override
            public void e(String tag, String msg) {
                Log.e(tag, msg);
            }
        });


        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addBtn(layout);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.WHITE);
        sv.addView(layout);

        setContentView(sv);
    }

    public void cancel() {
        getHttpExecutor().cancelRequest(this);
    }

    private void addBtn(LinearLayout layout) {
        Method[] methods = this.getClass().getDeclaredMethods();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = 30;

        try {
            Button btn = createBtn(this.getClass().getMethod("cancel"));
            layout.addView(btn, params);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        for (Method method : methods) {
            if (method.getName().startsWith("test")) {
                Button btn = createBtn(method);
                layout.addView(btn, params);
            }
        }
    }

    private Button createBtn(final Method method) {
        Button button = new Button(this);
        button.setText(method.getName());
        button.setTextColor(Color.BLACK);
        int padding = 20;
        button.setPadding(padding, padding, padding, padding);
        final Object obj = this;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    method.invoke(obj);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        });
        return button;
    }

    private Executor executor = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };
    HttpExecutor httpExecutor;

    protected HttpExecutor getHttpExecutor() {
        if (httpExecutor == null) {
            httpExecutor = new DefalutHttpExecutor(executor);
            //Use default http engine.
            //You can write yourself Engine extends HurlEngine.
            //You can also write it only implements Engine interface
            httpExecutor.setEngine(new DefaultHttpEngine());
        }
        return httpExecutor;
    }

    public void sendRequest(HttpRequest request, NetCallback callback) {
        request.setTag(this);
        getHttpExecutor().sendRequest(request, callback);

    }

    public void testHttp() {
        //String request,it return String result.

        HttpRequest httpRequest = new StringRequest();
        httpRequest.setMethod("get");
        httpRequest.setUrl("https://github.com/xwangly/picasso/blob/master/settings.gradle");
        sendRequest(httpRequest, new NetCallback<String>() {
            @Override
            public void onResponse(String response) {
                System.out.println("onResponse:" + response);
            }

            @Override
            public void onFailure(NetException e) {
                e.printStackTrace();
            }
        });
    }

    public void testJsonObject() {
        //String request,it return String result.

        HttpRequest httpRequest = new JsonObjectRequest();
        httpRequest.setMethod("get");
        httpRequest.setUrl("http://query.yahooapis.com/v1/public/yql?q=show%20tables&format=json&callback=");
        sendRequest(httpRequest, new NetCallback<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                System.out.println("onResponse:" + response);
            }

            @Override
            public void onFailure(NetException e) {
                e.printStackTrace();
            }
        });
    }
}
