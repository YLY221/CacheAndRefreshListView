package com.example.testdemo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**

 * 原文地址: http://blog.csdn.net/guolin_blog/article/details/45586553

 * @author guolin

 */

public class ImageAdapter extends ArrayAdapter<String> {



    private ListView mListView;



    private Bitmap mLoadingBitmap;



    /**

     * 图片缓存技术的核心类，用于缓存所有下载好的图片，在程序内存达到设定值时会将最少最近使用的图片移除掉。

     */

    private LruCache<String, BitmapDrawable> mMemoryCache;



    public ImageAdapter(Context context, int resource, String[] objects) {

        super(context, resource, objects);

        mLoadingBitmap = BitmapFactory.decodeResource(context.getResources(),

                R.drawable.ic_launcher_foreground);

        // 获取应用程序最大可用内存

        int maxMemory = (int) Runtime.getRuntime().maxMemory();

        int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, BitmapDrawable>(cacheSize) {

            @Override

            protected int sizeOf(String key, BitmapDrawable drawable) {

                return drawable.getBitmap().getByteCount();

            }

        };

    }



    @Override

    public View getView(int position, View convertView, ViewGroup parent) {

        if (mListView == null) {

            mListView = (ListView) parent;

        }

        String url = getItem(position);

        View view;

        if (convertView == null) {

            view = LayoutInflater.from(getContext()).inflate(R.layout.image_item, null);

        } else {

            view = convertView;

        }

        ImageView image = (ImageView) view.findViewById(R.id.image);

        BitmapDrawable drawable = getBitmapFromMemoryCache(url);

        if (drawable != null) {

            image.setImageDrawable(drawable);

        } else if (cancelPotentialWork(url, image)) {

            BitmapWorkerTask task = new BitmapWorkerTask(image);

            AsyncDrawable asyncDrawable = new AsyncDrawable(getContext()

                    .getResources(), mLoadingBitmap, task);

            image.setImageDrawable(asyncDrawable);

            task.execute(url);

        }

        return view;

    }



    /**

     * 自定义的一个Drawable，让这个Drawable持有BitmapWorkerTask的弱引用。

     */

    class AsyncDrawable extends BitmapDrawable {

        private WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,

                             BitmapWorkerTask bitmapWorkerTask) {

            super(res, bitmap);

            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(

                    bitmapWorkerTask);

        }



        public BitmapWorkerTask getBitmapWorkerTask() {

            return bitmapWorkerTaskReference.get();

        }



    }



    /**

     * 获取传入的ImageView它所对应的BitmapWorkerTask。

     */

    private BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {

        if (imageView != null) {

            Drawable drawable = imageView.getDrawable();

            if (drawable instanceof AsyncDrawable) {

                AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;

                return asyncDrawable.getBitmapWorkerTask();

            }

        }

        return null;

    }



    /**

     * 取消掉后台的潜在任务，当认为当前ImageView存在着一个另外图片请求任务时

     * ，则把它取消掉并返回true，否则返回false。

     */

    public boolean cancelPotentialWork(String url, ImageView imageView) {

        BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {

            String imageUrl = bitmapWorkerTask.imageUrl;

            if (imageUrl == null || !imageUrl.equals(url)) {

                bitmapWorkerTask.cancel(true);

            } else {

                return false;

            }

        }

        return true;

    }



    /**

     * 将一张图片存储到LruCache中。

     *

     * @param key

     *            LruCache的键，这里传入图片的URL地址。

     * @param drawable

     *            LruCache的值，这里传入从网络上下载的BitmapDrawable对象。

     */

    public void addBitmapToMemoryCache(String key, BitmapDrawable drawable) {

        if (getBitmapFromMemoryCache(key) == null) {

            mMemoryCache.put(key, drawable);

        }

    }



    /**

     * 从LruCache中获取一张图片，如果不存在就返回null。

     *

     * @param key

     *            LruCache的键，这里传入图片的URL地址。

     * @return 对应传入键的BitmapDrawable对象，或者null。

     */

    public BitmapDrawable getBitmapFromMemoryCache(String key) {

        return mMemoryCache.get(key);

    }



    /**

     * 异步下载图片的任务。

     *

     * @author guolin

     */

    class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {



        String imageUrl;



        private WeakReference<ImageView> imageViewReference;



        public BitmapWorkerTask(ImageView imageView) {

            imageViewReference = new WeakReference<ImageView>(imageView);

        }



        @Override

        protected BitmapDrawable doInBackground(String... params) {

            imageUrl = params[0];

            // 在后台开始下载图片

            Bitmap bitmap = downloadBitmap(imageUrl);

            BitmapDrawable drawable = new BitmapDrawable(getContext().getResources(), bitmap);

            addBitmapToMemoryCache(imageUrl, drawable);

            return drawable;

        }



        @Override

        protected void onPostExecute(BitmapDrawable drawable) {

            ImageView imageView = getAttachedImageView();

            if (imageView != null && drawable != null) {

                imageView.setImageDrawable(drawable);

            }

        }



        /**

         * 获取当前BitmapWorkerTask所关联的ImageView。

         */

        private ImageView getAttachedImageView() {

            ImageView imageView = imageViewReference.get();

            BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask) {

                return imageView;

            }

            return null;

        }



        /**

         * 建立HTTP请求，并获取Bitmap对象。

         *

         * @param imageUrl

         *            图片的URL地址

         * @return 解析后的Bitmap对象

         */

        private Bitmap downloadBitmap(String imageUrl) {

            Bitmap bitmap = null;

            HttpURLConnection con = null;

            try {

                URL url = new URL(imageUrl);

                con = (HttpURLConnection) url.openConnection();

                con.setConnectTimeout(5 * 1000);

                con.setReadTimeout(10 * 1000);

                bitmap = BitmapFactory.decodeStream(con.getInputStream());

            } catch (Exception e) {

                e.printStackTrace();

            } finally {

                if (con != null) {

                    con.disconnect();

                }

            }

            return bitmap;

        }



    }



}


























/*
public class ImageAdapter extends ArrayAdapter<String> {

    private ListView mListView;

    private LruCache<String,BitmapDrawable> mMemoryCache;
    public ImageAdapter( Context context, int resource, String[] objects) {
        super(context, resource, objects);

        int maxMemory= (int) Runtime.getRuntime().maxMemory();
        final int cacheSize = maxMemory/8;
        mMemoryCache = new LruCache<String,BitmapDrawable>(cacheSize){
            @Override
            protected int sizeOf(String key, BitmapDrawable drawable) {
                return drawable.getBitmap().getByteCount();
            }

        };
    }



    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount();
    }

    @Override
    public View getView(int position,  View convertView, ViewGroup parent) {

        if (mListView==null){
            mListView= (ListView) parent;
        }
        String url = getItem(position);
        View view;
        if (convertView==null){
            view = LayoutInflater.from(getContext()).inflate(R.layout.image_item,null);
        }else {
            view = convertView;
        }

        ImageView image = view.findViewById(R.id.image);
        image.setTag(url);
        BitmapDrawable drawable = getBitmapFromMemoryCache(url);
        if (drawable!=null){
            image.setImageDrawable(drawable);
        }else {
            BitmapWorkerTask task = new BitmapWorkerTask(image);
            task.execute(url);
        }
        return view;
    }



    public void addBitmapToMemoryCache(String key, BitmapDrawable drawable){
        if (getBitmapFromMemoryCache(key)==null){
            mMemoryCache.put(key,drawable);

        }
    }
    private BitmapDrawable getBitmapFromMemoryCache(String url){
        return mMemoryCache.get(url);
    }

    class BitmapWorkerTask extends AsyncTask<String,Void,BitmapDrawable> {

        private ImageView imageView;
        public BitmapWorkerTask(ImageView imageView){
            this.imageView= imageView;
        }

        String imageUrl;
        @Override
        protected BitmapDrawable doInBackground(String... params) {
            imageUrl = params[0];
            Bitmap bitmap = downloadBitmap(imageUrl);
            BitmapDrawable drawable = new BitmapDrawable(getContext().getResources(),bitmap);
            addBitmapToMemoryCache(imageUrl,drawable);
            return drawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable drawable) {
            Log.e("ImageAdapter",imageView+"----"+drawable);
            ImageView imageView=mListView.findViewWithTag(imageUrl);
            if (imageView!=null&&drawable!=null){
                imageView.setImageDrawable(drawable);
            }
        }

        private Bitmap downloadBitmap(String imageUrl){
            Bitmap bitmap = null;
            HttpURLConnection con = null;
            try {
                URL url = new URL(imageUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(50000);
                con.setReadTimeout(10000);
                bitmap= BitmapFactory.decodeStream(con.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if (con!=null){
                    con.disconnect();
                }
            }
            return bitmap;
        }
    }
}
*/
