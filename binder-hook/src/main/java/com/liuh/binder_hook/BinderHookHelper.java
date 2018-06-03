package com.liuh.binder_hook;

import android.os.IBinder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * Created by huan on 2018/6/3.
 */

public class BinderHookHelper {

    public static void hookClipboardService() throws Exception {

        final String CLIPBOARD_SERVICE = "clipboard";

        //这一段代码的意思是：ServiceManager.getService("clipboard");
        //只不过ServiceManager这个类是@hide的
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        Method getService = serviceManager.getDeclaredMethod("getService", String.class);

        //ServiceManager里面管理的原始的Clipboard Binder对象
        //一般来说这是一个Binder代理对象
        IBinder rawBinder = (IBinder) getService.invoke(null, CLIPBOARD_SERVICE);

        //Hook掉这个Binder代理对象的 queryLocalInterface 方法
        //然后在 queryLocalInterface 返回一个IInterface对象，hook掉我们感兴趣的方法
        IBinder hookedBinder = (IBinder) Proxy.newProxyInstance(serviceManager.getClassLoader(),
                new Class<?>[]{IBinder.class},
                new BinderProxyHookHandler(rawBinder));

        //把这个hook过的Binder代理对象放进ServiceManager的cache里面
        //以后查询的时候，会优先查询缓存里面的Binder，这样就会使用被我们修改过的Binder了
        Field cacheField = serviceManager.getDeclaredField("sCache");
        cacheField.setAccessible(true);
        Map<String, IBinder> cache = (Map<String, IBinder>) cacheField.get(null);
        cache.put(CLIPBOARD_SERVICE, hookedBinder);

    }

}
