package com.jeson.baiduapi.fragment;


import android.content.Context;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
            mPage = 0;
            mFragment = inflater.inflate(R.layout.fragment_joke, container, false);
            mRequestQueue = Volley.newRequestQueue(getContext());
            mSwipyRefreshLayout = (SwipyRefreshLayout) mFragment.findViewById(R.id.swipyrefresh_jokefragment);
            mRecyclerView = (RecyclerView) mFragment.findViewById(R.id.recyclerview_jokefragment);

            mSwipyRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mJokes = new ArrayList<>();
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
                        getJokes();
                    }
                }
            });
            getJokes();
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

    private void getJokes(){
        mPage++;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, "http://apis.baidu.com/showapi_open_bus/showapi_joke/joke_text?page="+ mPage , null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONObject("showapi_res_body").getJSONArray("contentlist");
                            for (int i = 0; i < jsonArray.length(); i++){
                                JSONObject object = jsonArray.getJSONObject(i);
                                Joke joke = new Joke();
                                joke.setTitle(object.getString("title"));
                                joke.setText(object.getString("text"));
                                joke.setCt(object.getString("ct"));
                                joke.setType(object.getInt("type"));
                                mJokes.add(joke);
                            }
                            //mRecyclerAdapter = new RecyclerAdapter(jokes, getContext());
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
