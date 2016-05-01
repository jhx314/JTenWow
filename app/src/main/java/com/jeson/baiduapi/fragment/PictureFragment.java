package com.jeson.baiduapi.fragment;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
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
import com.jeson.baiduapi.activity.PicDetailsActivity;
import com.jeson.baiduapi.model.Picture;
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
public class PictureFragment extends Fragment {

    private View mFragment;
    private SwipyRefreshLayout mSwipyRefreshLayout;
    private RecyclerView mRecyclerView;
    private RequestQueue mRequestQueue;
    private List<Picture> mPictures;
    private RecyclerAdapter mRecyclerAdapter;
    private int mPage;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mFragment == null) {
            mPage = 0;
            mFragment = inflater.inflate(R.layout.fragment_picture, container, false);
            mRequestQueue = Volley.newRequestQueue(getContext());
            mSwipyRefreshLayout = (SwipyRefreshLayout) mFragment.findViewById(R.id.swipyrefresh_picfragment);
            mRecyclerView = (RecyclerView) mFragment.findViewById(R.id.recyclerview_picfragment);
            mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL));
            mPictures = new ArrayList<>();
            mRecyclerAdapter = new RecyclerAdapter(mPictures, getContext());
            mRecyclerView.setAdapter(mRecyclerAdapter);

            mSwipyRefreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh(SwipyRefreshLayoutDirection direction) {
                    getPictures();
                }
            });
            getPictures();
        }
        return mFragment;
    }

    public void getPictures(){
        mPage++;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, "http://www.tngou.net/tnfs/api/list?rows=10&page="+mPage, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println(response.toString());
                        mSwipyRefreshLayout.setRefreshing(false);
                        try {
                            JSONArray jsonArray = response.getJSONArray("tngou");
                            for (int i = 0; i < jsonArray.length(); i++){
                                JSONObject object = jsonArray.getJSONObject(i);
                                Picture picture = new Picture();
                                picture.setTime(object.getLong("time"));
                                picture.setTitle(object.getString("title"));
                                picture.setImg(object.getString("img"));
                                mPictures.add(picture);
                            }
                            mRecyclerAdapter.notifyItemChanged(mPictures.size() - 1);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
                mSwipyRefreshLayout.setRefreshing(false);
            }
        });

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

        private List<Picture> pictures;
        private LayoutInflater inflater;

        public  RecyclerAdapter(List<Picture> pictures, Context context){
            this.pictures = pictures;
            this.inflater = LayoutInflater.from(context);
        }
        @Override
        public myHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_recyclerview_pic, parent, false);
            return new myHolder(view);
        }

        @Override
        public void onBindViewHolder(final myHolder holder, final int position) {
            holder.pic.setTag(pictures.get(position).getImg());
            holder.pic.setImageResource(R.mipmap.picture);
            holder.title.setText(pictures.get(position).getTitle());
            //holder.time.setText(new );
            ImageRequest request = new ImageRequest("http://tnfs.tngou.net/image"+pictures.get(position).getImg(), new Response.Listener<Bitmap>() {

                @Override
                public void onResponse(Bitmap response) {
                    if (holder.pic.getTag().equals(pictures.get(position).getImg())) {
                        holder.pic.setImageBitmap(response);
                    }
                }
            }, 0, 0, null, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //Toast.makeText(getContext(), error.toString(), Toast.LENGTH_SHORT).show();
                }
            });

            mRequestQueue.add(request);
        }

        @Override
        public int getItemCount() {
            return mPictures.size();
        }

        class myHolder extends  RecyclerView.ViewHolder{

            private ImageView pic;
            private TextView title;
            private TextView time;
            private CardView cardView;

            public myHolder(View itemView) {
                super(itemView);
                pic = (ImageView) itemView.findViewById(R.id.imageview_pic_pic);
                title = (TextView) itemView.findViewById(R.id.textview_pic_title);
                //time = (TextView) itemView.findViewById(R.id.textview_pic_url);
                cardView = (CardView) itemView.findViewById(R.id.cardview_pic);
            }
        }
    }
}
