package com.jeson.baiduapi.fragment;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.jeson.baiduapi.R;
import com.jeson.baiduapi.activity.NewsDetailsActivity;
import com.jeson.baiduapi.model.News;
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
public class NewsFragment extends Fragment {

    private View mFragment;
    private SwipyRefreshLayout mSwipyRefreshLayout;
    private RecyclerView mRecyclerView;
    private RequestQueue mRequestQueue;
    private RecyclerAdapter mRecyclerAdapter;
    private List<News> mNewses;
    private int mPage;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mFragment == null) {
            mPage = 0;
            mRequestQueue = Volley.newRequestQueue(getContext());
            mFragment = inflater.inflate(R.layout.fragment_news, container, false);
            mSwipyRefreshLayout = (SwipyRefreshLayout) mFragment.findViewById(R.id.swipyrefresh_newsfragment);
            mRecyclerView = (RecyclerView) mFragment.findViewById(R.id.recyclerview_newsfragment);
            mSwipyRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light);
            mSwipyRefreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh(SwipyRefreshLayoutDirection direction) {
                    getNewes();
                }
            });
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mNewses = new ArrayList<>();
            mRecyclerAdapter = new RecyclerAdapter(mNewses, getContext());
            mRecyclerView.setAdapter(mRecyclerAdapter);
            getNewes();
        }
        return mFragment;
    }

    private void getNewes(){
        mPage++;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, "http://apis.baidu.com/txapi/social/social?num=20&page=" + mPage,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                mSwipyRefreshLayout.setRefreshing(false);
                try {
                    JSONArray jsonArray = response.getJSONArray("newslist");
                    for (int i = 0; i < jsonArray.length(); i++){
                        JSONObject object = jsonArray.getJSONObject(i);
                        News news = new News();
                        news.setTitle(object.getString("title"));
                        news.setTime(object.getString("ctime"));
                        news.setPicUrl(object.getString("picUrl"));
                        news.setUrl(object.getString("url"));
                        mNewses.add(news);
                    }
                    mRecyclerAdapter.notifyItemChanged(mNewses.size() - 1);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mSwipyRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> map = new HashMap<>();
                map.put("apikey", "443cca262ab1f48a67ef1031d69e3a1a");
                return map;
            }
        };
        mRequestQueue.add(request);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mFragment != null){
            ((ViewGroup)mFragment.getParent()).removeView(mFragment);
        }
    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.myHolder>{

        private List<News> newses;
        private LayoutInflater inflater;

        public RecyclerAdapter(List<News> newses, Context context){
            this.newses = newses;
            this.inflater = LayoutInflater.from(context);
        }
        @Override
        public RecyclerAdapter.myHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_recyclerview_news, parent, false);
            return new myHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerAdapter.myHolder holder, final int position) {
            holder.newsTitle.setText(newses.get(position).getTitle());
            holder.newsTime.setText(newses.get(position).getTime());
            if (newses.get(position).getPicUrl() == "" || newses.get(position).getPicUrl().equals("")){

            }else{
                ImageRequest request = new ImageRequest(newses.get(position).getPicUrl(), new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        holder.newsPic.setImageBitmap(response);
                    }
                }, 0, 0, null, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
                mRequestQueue.add(request);
            }
            holder.newsUrl.setText(newses.get(position).getUrl());
        }

        @Override
        public int getItemCount() {
            return newses.size();
        }

        class myHolder extends RecyclerView.ViewHolder{

            private ImageView newsPic;
            private TextView newsTitle;
            private TextView newsTime;
            private TextView newsUrl;
            private CardView cardView;

            public myHolder(View itemView) {
                super(itemView);
                newsTitle = (TextView) itemView.findViewById(R.id.textview_news_title);
                newsTime = (TextView) itemView.findViewById(R.id.textview_news_time);
                newsUrl = (TextView) itemView.findViewById(R.id.textview_news_url);
                newsPic = (ImageView) itemView.findViewById(R.id.imageview_news_pic);
                cardView = (CardView) itemView.findViewById(R.id.cardview_newes);
                cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getContext(), NewsDetailsActivity.class);
                        intent.putExtra("url", newsUrl.getText().toString());
                        startActivity(intent);
                    }
                });
            }
        }

    }
}
