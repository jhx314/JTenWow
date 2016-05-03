package com.jeson.baiduapi.fragment;


import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.jeson.baiduapi.R;
import com.jeson.baiduapi.model.Joke;
import com.jeson.baiduapi.sqlite.JokeReaderContract;
import com.jeson.baiduapi.sqlite.JokeReaderDbHelper;
import com.jeson.baiduapi.util.NetUtil;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class JokeFragment extends Fragment {
    private final static int LOADJOKES_SUCCESS = 0;
    private final static int LOADJOKES_FAILURE = 1;

    private View mFragment;
    private SwipyRefreshLayout mSwipyRefreshLayout;
    private RecyclerView mRecyclerView;
    private RequestQueue mRequestQueue;
    private RecyclerAdapter mRecyclerAdapter;
    private List<Joke> mJokes;
    private int mPage;
    private JokeReaderDbHelper mDbHelper;
    private ProgressBar mProgressBar;
    private TextView mTextViewCheckNet;
    private boolean mIsNetEnable;
    private boolean mIsFirstLoadJokes = true; //是否是第一次从网络加载数据

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                if (NetUtil.checkNetworkConnection(context)){
                    mIsNetEnable = true;
                    mTextViewCheckNet.setVisibility(View.GONE);
                    if (mIsFirstLoadJokes) {
                        mIsFirstLoadJokes = false;
                        mProgressBar.setVisibility(View.VISIBLE);
                        getJokes();
                    }
                    Log.d("net","当前网络可用");
                }else{
                    mIsNetEnable = false;
                    mTextViewCheckNet.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.GONE);
                    Log.d("net","当前网络不可用");
                }
            }
        }
    };

    private Handler mHandler = new Handler(){
        @Override
        public void dispatchMessage(Message msg) {
            mSwipyRefreshLayout.setRefreshing(false);
            switch (msg.what){
                case LOADJOKES_SUCCESS:
                    //mRecyclerView.setAdapter(mRecyclerAdapter);
                    break;
                case LOADJOKES_FAILURE:
                    Toast.makeText(getContext(),msg.getData().getString("error"),Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            super.dispatchMessage(msg);
        }
    };
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if( mFragment == null){

            mDbHelper = new JokeReaderDbHelper(getContext());
            mPage = 0;
            mFragment = inflater.inflate(R.layout.fragment_joke, container, false);
            mRequestQueue = Volley.newRequestQueue(getContext());
            mSwipyRefreshLayout = (SwipyRefreshLayout) mFragment.findViewById(R.id.swipyrefresh_jokefragment);
            mRecyclerView = (RecyclerView) mFragment.findViewById(R.id.recyclerview_jokefragment);
            mProgressBar = (ProgressBar) mFragment.findViewById(R.id.progressbar_joke);
            mTextViewCheckNet = (TextView) mFragment.findViewById(R.id.textview_joke_checknet);

            mSwipyRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mJokes = getJokesFromDB();
            mRecyclerAdapter = new RecyclerAdapter(mJokes, getContext());
            mRecyclerView.setAdapter(mRecyclerAdapter);
            mSwipyRefreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh(SwipyRefreshLayoutDirection direction) {
                    if (direction == SwipyRefreshLayoutDirection.BOTTOM) {
                        getJokes();
                    }else{
                        mPage = 0;
                        mJokes = new ArrayList<Joke>();
                        mRecyclerAdapter = new RecyclerAdapter(mJokes, getContext());
                        mRecyclerView.setAdapter(mRecyclerAdapter);
                        if (mIsNetEnable) {
                            getJokes();
                        }else{
                            Toast.makeText(getContext(),"请连接网络再进行刷新操作！",Toast.LENGTH_SHORT).show();
                            mSwipyRefreshLayout.setRefreshing(false);
                        }
                    }
                }
            });

            //由于要改变界面，所以在控件初始化后开始广播接收
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getContext().registerReceiver(mBroadcastReceiver, intentFilter);
            if (mIsNetEnable){
                getJokes();
            }
        }
        return mFragment;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mFragment != null){
            ((ViewGroup)mFragment.getParent()).removeView(mFragment);
        }
    }

    public void insertJokeToDB(Joke joke){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(JokeReaderContract.JokeEntry.COLUMN_NAME_JOKE_ID, joke.getType());
        values.put(JokeReaderContract.JokeEntry.COLUMN_NAME_JOKE_TITLE, joke.getTitle());
        values.put(JokeReaderContract.JokeEntry.COLUMN_NAME_JOKE_TEXT, joke.getText());
        values.put(JokeReaderContract.JokeEntry.COLUMN_NAME_JOKE_CREATIME, joke.getCt());

        db.insert(JokeReaderContract.JokeEntry.TABLE_NAME,
                null, values);
        db.close();
    }

    public List<Joke> getJokesFromDB(){
        List<Joke> jokes = new ArrayList<>();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                JokeReaderContract.JokeEntry._ID,
                JokeReaderContract.JokeEntry.COLUMN_NAME_JOKE_ID,
                JokeReaderContract.JokeEntry.COLUMN_NAME_JOKE_TITLE,
                JokeReaderContract.JokeEntry.COLUMN_NAME_JOKE_TEXT,
                JokeReaderContract.JokeEntry.COLUMN_NAME_JOKE_CREATIME
        };

        String sortOrder =
                JokeReaderContract.JokeEntry._ID + " DESC";

        Cursor c = db.query(
                JokeReaderContract.JokeEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null);
        c.moveToFirst();
        if (c.getCount() == 0){
            return jokes;
        }
        do {
            Joke joke = new Joke();
            joke.setType(c.getInt(1));
            joke.setTitle(c.getString(2));
            joke.setText(c.getString(3));
            joke.setCt(c.getString(4));
            jokes.add(joke);
        }while(c.moveToNext());
        db.close();
        return jokes;
    }

    private void getJokes(){
        mPage++;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, "http://apis.baidu.com/showapi_open_bus/showapi_joke/joke_text?page="+ mPage , null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONObject("showapi_res_body").getJSONArray("contentlist");
                            if (mProgressBar.getVisibility() == View.VISIBLE){
                                mDbHelper.getWritableDatabase().execSQL("delete from " + JokeReaderContract.JokeEntry.TABLE_NAME);
                                mProgressBar.setVisibility(View.GONE);
                                mJokes = new ArrayList<>();
                                mRecyclerAdapter = new RecyclerAdapter(mJokes, getContext());
                                mRecyclerView.setAdapter(mRecyclerAdapter);
                            }
                            for (int i = 0; i < jsonArray.length(); i++){
                                JSONObject object = jsonArray.getJSONObject(i);
                                Joke joke = new Joke();
                                joke.setTitle(object.getString("title"));
                                joke.setText(object.getString("text"));
                                joke.setCt(object.getString("ct"));
                                joke.setType(object.getInt("type"));
                                mJokes.add(joke);
                                insertJokeToDB(joke);
                            }
                            mRecyclerAdapter.notifyItemChanged(mJokes.size());
                            Message msg = new Message();
                            msg.what = LOADJOKES_SUCCESS;
                            mHandler.sendMessage(msg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Message msg = new Message();
                            msg.what = LOADJOKES_FAILURE;
                            Bundle bundle = new Bundle();
                            bundle.putString("error", e.getMessage().toString());
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Message msg = new Message();
                msg.what = LOADJOKES_FAILURE;
                Bundle bundle = new Bundle();
                bundle.putString("error", error.toString());
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> map = new HashMap<>();
                map.put("apikey","443cca262ab1f48a67ef1031d69e3a1a");
                return map;
            }
        };
        mRequestQueue.add(request);
    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.myHolder>{

        private List<Joke> jokes;
        private LayoutInflater inflater;

        public RecyclerAdapter(List<Joke> jokes, Context context){
            this.jokes = jokes;
            this.inflater = LayoutInflater.from(context);
        }
        @Override
        public myHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_recyclerview_joke, parent, false);
            return new myHolder(view);
        }

        @Override
        public void onBindViewHolder(myHolder holder, int position) {
            holder.jokeTitle.setText(jokes.get(position).getTitle());
            holder.jokeText.setText(Html.fromHtml(jokes.get(position).getText()));
            holder.jokeCt.setText(jokes.get(position).getCt());
        }

        @Override
        public int getItemCount() {
            return jokes.size();
        }

        class myHolder extends RecyclerView.ViewHolder{
            private TextView jokeTitle;
            private TextView jokeText;
            private TextView jokeCt;
            public myHolder(View itemView) {
                super(itemView);
                jokeTitle = (TextView) itemView.findViewById(R.id.textview_joke_title);
                jokeText = (TextView) itemView.findViewById(R.id.textview_joke_text);
                jokeCt = (TextView) itemView.findViewById(R.id.textview_joke_ct);
            }
        }
    }
}
