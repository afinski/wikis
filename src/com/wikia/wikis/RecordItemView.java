package com.wikia.wikis;


import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RecordItemView extends LinearLayout implements OnClickListener {

	private ImageView iv_wiki_thumbnail;
	private ProgressBar pb_load_indicator;

	public RecordItemView(Context context, Wikis_item item) {
		super(context); 
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.lv_layout, this, true);
		
		TextView tv_title = (TextView) findViewById(R.id.tv_wiki_title);
		TextView tv_wiki_url = (TextView) findViewById(R.id.tv_wiki_url);
		tv_title.setText(item.getTitle());
		tv_wiki_url.setText(item.getWikis_url());
		
		iv_wiki_thumbnail = (ImageView) findViewById(R.id.iv_wiki_thumbnail);
		iv_wiki_thumbnail.setVisibility(View.GONE);
		
		pb_load_indicator = (ProgressBar) findViewById(R.id.pb_load_indicator);
		pb_load_indicator.setVisibility(View.VISIBLE);

		ImageLoader imageLoader = new ImageLoader();
		imageLoader.execute(item);
//		imageLoader.
//		Drawable d = ;
//		iv_wiki_thumbnail.setBackgroundDrawable(d );
//		iv_wiki_thumbnail.setBackgroundResource(R.drawable.ic_launcher);
		
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
	
	private class ImageLoader extends AsyncTask<Wikis_item, Void, Drawable> {
		
		@Override
		protected void onPostExecute(Drawable result) {
			super.onPostExecute(result);
			displayImage(result);
		}
		
		@Override
		protected Drawable doInBackground(Wikis_item... params) {
			try {
				Wikis_item item = params[0];
		        HttpClient httpclient = new DefaultHttpClient();
		        HttpPost httppost = new HttpPost(item .getWikis_url());
		        Log.d("wikis", "item .getWikis_url():" + item .getWikis_url());
		        HttpResponse response = null;
				response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		
		}
	}
	public void displayImage(Drawable d) {
		if(d != null){
			pb_load_indicator.setVisibility(View.GONE);
			iv_wiki_thumbnail.setVisibility(View.VISIBLE);
			iv_wiki_thumbnail.setBackgroundDrawable(d);
		}

	}

	
}
