package com.example.testdemo;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnRefreshListener{
    private RefreshListView rListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rListView=findViewById(R.id.list_view);
        ImageAdapter imageAdapter=new ImageAdapter(this,0,Images.imageUrls);
        rListView.setAdapter(imageAdapter);

        //可以用这种注册监听的方式,这种方式需要实现MainActivity  implements OnRefreshListener
        rListView.setOnRefreshListener(this);

        //也可以用这种注册监听的方式
       /* rListView.setOnRefreshListener(new OnRefreshListener() {

            @Override
            public void onDownPullRefresh() {
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        SystemClock.sleep(2000);
                        for (int i = 0; i < 2; i++) {
                            textList.add(0, "这是下拉刷新出来的数据" + i);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        adapter.notifyDataSetChanged();
                        rListView.hideHeaderView();
                    }
                }.execute(new Void[]{});
            }

            @Override
            public void onLoadingMore() {
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        SystemClock.sleep(5000);

                        textList.add("这是加载更多出来的数据1");
                        textList.add("这是加载更多出来的数据2");
                        textList.add("这是加载更多出来的数据3");
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        adapter.notifyDataSetChanged();

                        // 控制脚布局隐藏
                        rListView.hideFooterView();
                    }
                }.execute(new Void[]{});
            }
        });*/
    }

    @Override
    public void onDownPullRefresh() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                SystemClock.sleep(2000);

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                //adapter.notifyDataSetChanged();
                rListView.hideHeaderView();
            }
        }.execute(new Void[]{});
    }

    @Override
    public void onLoadingMore() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                SystemClock.sleep(5000);

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                //adapter.notifyDataSetChanged();

                // 控制脚布局隐藏
                rListView.hideFooterView();
            }
        }.execute(new Void[]{});
    }

}
