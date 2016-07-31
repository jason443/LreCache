package com.example.asus.lrecache;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends Activity implements View.OnClickListener{

    private LruCache<String, Bitmap> mLruCache;
    private Button mSetImageBt;
    private Button mChangeImageBt;
    private ImageView mShowImageIv;
    private ExecutorService mExecutorPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initLruCache();
        initPool();
    }
    // 初始化控件
    private void initView() {
        mSetImageBt = (Button) findViewById(R.id.main_bt_set_image);
        mSetImageBt.setOnClickListener(this);
        mChangeImageBt = (Button) findViewById(R.id.main_bt_change_image);
        mChangeImageBt.setOnClickListener(this);
        mShowImageIv = (ImageView) findViewById(R.id.main_iv_show_image);
    }
    //初始化LruCache
    private void initLruCache() {
        final int maxMemory = (int)(Runtime.getRuntime().maxMemory()/1024);
        int cacheSize = maxMemory/8;
        mLruCache = new LruCache<>(cacheSize);
    }
    //初始化线程池
    private void initPool() {
        mExecutorPool = Executors.newCachedThreadPool();
    }
    //从缓冲中取图片
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }
    //将图片装入缓存
    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if(getBitmapFromLruCache(key) == null) {
            mLruCache.put(key, bitmap);
        }
    }
    //模拟从网上下载图片
    private Bitmap downloadBitmapFromNet(String key) {
        Future<Bitmap> future = mExecutorPool.submit(new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                Bitmap bitmap;
                try {
                    Thread.sleep(3000); //模拟从网络下载过程
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.photo1);
                    return bitmap;
                } catch (InterruptedException e) {
                    return null;
                }
            }
        });

        Bitmap bitmap = null;
        try {
           bitmap = future.get(4, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Toast.makeText(MainActivity.this, "网络有误请重试", Toast.LENGTH_SHORT).show();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(bitmap != null) {
            addBitmapToMemoryCache(key, bitmap);
        }

        return bitmap;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.main_bt_set_image:
                setImageBtOnclick();
                break;
            case R.id.main_bt_change_image:
                changeImageBtOnclick();
                break;
            default:
                break;
        }
    }

    private void setImageBtOnclick() {
        Bitmap bitmap = getBitmapFromLruCache("photo1");
        if (bitmap != null) {
            mShowImageIv.setImageBitmap(bitmap);
            Toast.makeText(MainActivity.this, "从缓冲中获取图片", Toast.LENGTH_SHORT).show();
            Log.d("Main", "从缓冲中获取图片");
        } else {
            bitmap = downloadBitmapFromNet("photo1");
            mShowImageIv.setImageBitmap(bitmap);
            Toast.makeText(MainActivity.this, "从网络中获取图片",Toast.LENGTH_SHORT).show();
            Log.d("Main", "从网络中获取图片");
        }
    }

    private void changeImageBtOnclick() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.photo2);
        mShowImageIv.setImageBitmap(bitmap);
    }
}
