package com.wikia.wikis;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Wikis_fragment extends Fragment {
//	http://www.wikia.com/wikia.php?controller=WikisApi&method=getList&hub=Gaming&lang=en
//	wiki title, wiki url, wiki thumbnail image
	Context context;
	private View progressBarLinearLayout;
	private View listViewLinearLayout;
	private ListView listView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container,
				false);
		
		context = getActivity();
		
		progressBarLinearLayout = rootView.findViewById(R.id.progressBarLinearLayout);
		
        listView = ( ListView ) rootView.findViewById(R.id.lv_list);
//        listView.setOnItemLongClickListener(this);
		
		listViewLinearLayout = rootView.findViewById(R.id.listViewLinearLayout);
		listViewLinearLayout.setVisibility(View.GONE);
		
        ListViewLoaderTask listViewLoaderTask = new ListViewLoaderTask();
        
        listViewLoaderTask.execute();
		
		return rootView;
	}
	
    private class ListViewLoaderTask extends AsyncTask<Void, Void, ItemsArrayAdapter>{

    	ArrayList<Wikis_item> itemList;
		@Override
		protected ItemsArrayAdapter doInBackground(Void... params ) {
			try {
				itemList = getItemsfromURL();
			}catch(Exception e){
				e.printStackTrace();
			}

	        ItemsArrayAdapter adapter = new ItemsArrayAdapter(context, itemList);  
	        
			return adapter;
		}
		
		@Override
		protected void onPostExecute(ItemsArrayAdapter adapter) {
			progressBarLinearLayout.setVisibility(View.GONE);
			listViewLinearLayout.setVisibility(View.VISIBLE);

	        listView.setAdapter(adapter);
		}
		
    }
    
    public ArrayList<Wikis_item> getItemsfromURL() {
        InputStream is = null;
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://www.wikia.com/wikia.php?controller=WikisApi&method=getList&hub=Gaming&lang=en");
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
        	e.printStackTrace();
        	
//        	TODO It is for DEBUG 
//        	load JSON from assets
			return getItemsfromAssets();
        }
        return getParsedData(is);
    }
    
	public ArrayList<Wikis_item> getItemsfromAssets() {
        InputStream is = null;
        try {
			is = context.getAssets().open("list_of_wikis.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
        return getParsedData(is);
	}
    
	public ArrayList<Wikis_item> getParsedData(InputStream is) {
		if(is == null){
			return null;
		}
		ArrayList<Wikis_item> items = new ArrayList<Wikis_item>();
		try{
			JsonFactory factory = new JsonFactory();
			JsonParser jsonParser = factory.createParser(is);
			JsonToken token = jsonParser.nextToken();
			// Expected JSON is an array so if current token is "[" then while
			// we don't get
			// "]" we will keep parsing
//			if (token == JsonToken.START_ARRAY) {
//				while (token != JsonToken.END_ARRAY) {
			if (token == JsonToken.START_OBJECT) {
				while (token != JsonToken.END_OBJECT) {					
					// Inside array there are many objects, so it has to start
					// with "{" and end with "}"
//					token = jsonParser.nextToken();
//					if (token == JsonToken.START_OBJECT) {
////						Wikis_item item = null;
//						while (token != JsonToken.END_OBJECT) {
							// Each object has a name which we will use to
							// identify the type.
							token = jsonParser.nextToken();
							if (token == JsonToken.START_ARRAY) {
								while (token != JsonToken.END_ARRAY) {
									token = jsonParser.nextToken();
									if (token == JsonToken.START_OBJECT) {
										Wikis_item item = null;
										while (token != JsonToken.END_OBJECT) {
											token = jsonParser.nextToken();
//											Wikis_item item = null;
											if (token == JsonToken.FIELD_NAME) {
												String objectName = jsonParser.getCurrentName();
												if (0 == objectName.compareToIgnoreCase("id")) {
													item = new Wikis_item();
													jsonParser.nextToken();
													item.setId(jsonParser.getValueAsString());
												}else if (0 == objectName.compareToIgnoreCase("name")) {
													jsonParser.nextToken();
													item.setTitle(jsonParser.getValueAsString());
												} else if (0 == objectName.compareToIgnoreCase("domain")) {
													jsonParser.nextToken();
													item.setWikis_url(jsonParser.getValueAsString());
												} 
											}
										}
										items.add(item);
									}
									
								}
							}
//						}
////						items.add(item);
//					}
				}
			}
			System.out.println(items);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return items;
	}
    
	public class ItemsArrayAdapter extends ArrayAdapter<Wikis_item> {
		private final Context context;
		private final ArrayList<Wikis_item> items;
		
		public ItemsArrayAdapter(Context context, ArrayList<Wikis_item> itemList) {
		    super(context, R.layout.lv_layout, itemList);
		    this.context = context;
		    this.items = itemList;
		}
			
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.lv_layout, parent, false);
			TextView tv_title = (TextView) rowView.findViewById(R.id.tv_wiki_title);
			TextView tv_wiki_url = (TextView) rowView.findViewById(R.id.tv_wiki_url);
			ImageView iv_wiki_thumbnail = (ImageView) rowView.findViewById(R.id.iv_wiki_thumbnail);
			
			Wikis_item item = items.get(position);
			
			tv_title.setText(item.getTitle());
			tv_wiki_url.setText(item.getWikis_url());
			iv_wiki_thumbnail.setBackgroundResource(R.drawable.ic_launcher);
			
			return rowView;
		}
	} 
    
	
}
