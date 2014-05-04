package com.wikia.wikis;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
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
//            HttpPost httppost = new HttpPost("http://www.wikia.com/wikia.php?controller=WikisApi&method=getList&hub=Gaming&lang=en");
            HttpGet httpget = new HttpGet("http://www.wikia.com/wikia.php?controller=WikisApi&method=getList&hub=Gaming&lang=en");
            
//            HttpResponse response = httpclient.execute(httppost);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
        	e.printStackTrace();
        	
//        	TODO It is for DEBUG 
//        	load JSON from assets
//			return getItemsfromAssets();
        }
        return getParsedDataWikis(is);
    }
    
	public ArrayList<Wikis_item> getItemsfromAssets() {
        InputStream is = null;
        try {
			is = context.getAssets().open("list_of_wikis.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
        return getParsedDataWikis(is);
	}
    
	public ArrayList<Wikis_item> getParsedDataWikis(InputStream is) {
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
//			parent.addView(child, params)
			Wikis_item item = items.get(position);
//			View rowView = new RecordItemView(context, item);
			TextView tv_title = (TextView) rowView.findViewById(R.id.tv_wiki_title);
			TextView tv_wiki_url = (TextView) rowView.findViewById(R.id.tv_wiki_url);
			ImageView iv_wiki_thumbnail = (ImageView) rowView.findViewById(R.id.iv_wiki_thumbnail);
			
			tv_title.setText(item.getTitle());
			tv_wiki_url.setText(item.getWikis_url());

	        if (iv_wiki_thumbnail != null) {
	            new ImageDownloaderTask(iv_wiki_thumbnail).execute(item);
	        }
	        
//			ImageLoader imageLoader = new ImageLoader();
//			imageLoader.
//			Drawable d = ;
//			iv_wiki_thumbnail.setBackgroundDrawable(d );
//			iv_wiki_thumbnail.setBackgroundResource(R.drawable.ic_launcher);
			
			return rowView;
		}
	} 
	
	private class ImageDownloaderTask extends AsyncTask<Wikis_item, Void, Bitmap > {
		
		private final WeakReference<ImageView> imageViewReference;

		public ImageDownloaderTask(View rowView) {
			imageViewReference = new WeakReference<ImageView>((ImageView) rowView);
		}
		
		@Override
		protected void onPostExecute(Bitmap  bitmap) {
//			super.onPostExecute(bitmap);
	        if (isCancelled()) {
	            bitmap = null;
	        }
	 
	        if (imageViewReference != null) {
	            ImageView imageView = (ImageView) imageViewReference.get();
	            if (imageView != null) {
	 
	                if (bitmap != null) {
	                    imageView.setImageBitmap(bitmap);
	                } else {
	                    imageView.setImageDrawable(imageView.getContext().getResources()
	                            .getDrawable(R.drawable.ic_launcher));
	                }
	            }
	 
	        }
		}
			
		
		
		@Override
		protected Bitmap doInBackground(Wikis_item... params) {
			Wikis_item item = params[0];
			URI uri = null;
			try {
				uri = new URI("http://www.wikia.com/wikia.php?controller=WikisApi&method=getDetails&ids=" + item .getId());
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Log.d("wikis", "item .getWikis_url():" + item .getWikis_url());
			
//			final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
			HttpClient client = new DefaultHttpClient();
			final HttpGet getRequest = new HttpGet(uri);
			try {
			    HttpResponse response = client.execute(getRequest);
			    final int statusCode = response.getStatusLine().getStatusCode();
			    if (statusCode != HttpStatus.SC_OK) {
			        Log.w("ImageDownloader", "Error " + statusCode
			                + " while retrieving bitmap from " + item .getWikis_url());
			        return null;
			    }
 
			    final HttpEntity entity = response.getEntity();
			    if (entity != null) {
			        InputStream inputStream = null;
			        try {
			            inputStream = entity.getContent();
			            final Bitmap bitmap = BitmapFactory.decodeStream(getParsedDataWikisDetailsThubnail(inputStream/*, client*/));
			            return bitmap;
			        } finally {
			            if (inputStream != null) {
			                inputStream.close();
			            }
			            entity.consumeContent();
			        }
			    }
			} catch (Exception e) {
			    // Could provide a more explicit error message for IOException or
			    // IllegalStateException
			    getRequest.abort();
			    Log.w("ImageDownloader", "Error while retrieving bitmap from " 
			    		+ "http://www.wikia.com/wikia.php?controller=WikisApi&method=getDetails&ids=" );
			} 
			/*finally {
			    if (client != null) {
			        client.close();
			    }
			}*/
			return null;
		
		}

		private InputStream getParsedDataWikisDetailsThubnail(InputStream inputStream/*, AndroidHttpClient client*/) {
			String url = getParsedDataWikisDetails(inputStream);
			InputStream is = null;
			if(url == null){
				return null;
			}else{
				final HttpGet getRequest = new HttpGet(url);
				try {
					HttpClient client = new DefaultHttpClient();
				    HttpResponse response = client.execute(getRequest);
				    final int statusCode = response.getStatusLine().getStatusCode();
				    if (statusCode != HttpStatus.SC_OK) {
				        return null;
				    }
	 
				    final HttpEntity entity = response.getEntity();
				    if (entity != null) {
				        is = entity.getContent();
				    }
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				return is;
			}
		}
		
		public String getParsedDataWikisDetails(InputStream is) {
			if(is == null){
				return null;
			}
			String url = null;
			ArrayList<Wikis_item> items = new ArrayList<Wikis_item>();
			try{
				JsonFactory factory = new JsonFactory();
				JsonParser jsonParser = factory.createParser(is);
				JsonToken token = jsonParser.nextToken();
				if (token == JsonToken.START_OBJECT) {
					while (token != JsonToken.END_OBJECT) {					
						token = jsonParser.nextToken();
						if (token == JsonToken.START_OBJECT) {
							while (token != JsonToken.END_OBJECT) {
								token = jsonParser.nextToken();
								if (token == JsonToken.START_OBJECT) {
									while (token != JsonToken.END_OBJECT) {
										token = jsonParser.nextToken();
										if(token == JsonToken.START_OBJECT){
											while (token != JsonToken.END_OBJECT) {
												token = jsonParser.nextToken();
											}
											token = jsonParser.nextToken();
										}
										if(token == JsonToken.START_ARRAY){
											while (token != JsonToken.END_ARRAY) {
												token = jsonParser.nextToken();
											}
											token = jsonParser.nextToken();
										}
										if (token == JsonToken.FIELD_NAME) {
											String objectName = jsonParser.getCurrentName();
											if (0 == objectName.compareToIgnoreCase("image")) {
												jsonParser.nextToken();
												url = jsonParser.getValueAsString();
											}
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return url;
		}
		
		
	}
    

	
	
}
