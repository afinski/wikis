package com.wikia.wikis;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ImageView;

/**
 * Provides multithreaded asynchronous image loading for AdapterViews,
 * optimized. Pass in only one LoadPair or List<LoadPair> per row!
 * 
 * This program is free software. It comes without any warranty, to the extent
 * permitted by applicable law. You can redistribute it and/or modify it under
 * the terms of the Do What The Fuck You Want To Public License, Version 2, as
 * published by Sam Hocevar. See http://sam.zoy.org/wtfpl/COPYING for more
 * details.
 * 
 * @author Travis Biehn
 * 
 */
public class AdapterImageLoader {
	private ConcurrentHashMap<ImageView, LoadPair> cache = new ConcurrentHashMap<ImageView, LoadPair>();

	/**
	 * Provides callback functionality for image grabs.
	 * 
	 * @author Travis Biehn
	 * 
	 */
	public static abstract class BitmapCallback {
		public abstract void callback(Bitmap x);
	}

	/**
	 * Data structure for keeping track of images and their callbacks for worker
	 * threads.
	 * 
	 * @author Travis Biehn
	 * 
	 */

	public static class LoadPair {
		public URL url;
		public ImageView callback;

		public LoadPair(URL url, ImageView callback) {
			this.url = url;
			this.callback = callback;
		}
	}

	public static class LoadEntry {
		public List<LoadPair> list = null;
	}

	/**
	 * FILO/Stack Queue bound by the visible rows in the parent adapterView,
	 * this oldest, or last entries are trimmed on inserts to avoid buildup
	 * during fast list manipulation.
	 * 
	 * @author Travis Biehn
	 * 
	 * @param <T>
	 */
	public class FixedStack<T> extends LinkedBlockingDeque<T> {
		private static final long serialVersionUID = 1L;
		private static final int KLUDGEFACTOR = 3; // Magical kludge constant

		private int maxEntries() {
			// Visibile portion of the adapterView, might break, might not.
			return (adapterView.getLastVisiblePosition() - adapterView
					.getFirstVisiblePosition()) + KLUDGEFACTOR;
		}

		public FixedStack() {
		}

		public boolean add(T x) {
			super.addFirst(x);
			this.trimEntries();
			return true;
		}

		public void addFirst(T x) {
			super.addFirst(x);
			this.trimEntries();
		}

		public void addLast(T x) {
			throw new UnsupportedOperationException(
					"addLast not supported by this stack.");
		}

		/**
		 * Keeps the stack to a fixed size.
		 */
		private void trimEntries() {
			int toTrim = super.size() - maxEntries();
			if (toTrim > 0) {
				for (int i = 0; i < toTrim; i++) {
					super.pollLast(); // Pop off the end (oldest) entries.
				}
			}
		}

	}

	/**
	 * Low priority ThreadFactory for the image grab threads.
	 * 
	 * @author Travis Biehn
	 */
	private static ThreadFactory threadFactory = new ThreadFactory() {
		ThreadFactory tf = Executors.defaultThreadFactory();

		@Override
		public Thread newThread(final Runnable r) {
			final Thread out = this.tf.newThread(r);
			out.setPriority(Thread.NORM_PRIORITY / 2); // Low Priority

			return out;
		}
	};
	/**
	 * Number of threads to use for image grabbing.
	 */
	private static int imagePoolSize = 2;
	/**
	 * Threadpool for image grabbing.
	 */
	// It might make sense to change the implementation to static for your
	// application.
	// Note that you'll have to change this and the imageGrabber implementation
	// (to make it static.)
	private final ExecutorService imagePool = Executors.newFixedThreadPool(
			imagePoolSize, threadFactory);
	private final FixedStack<LoadEntry> workQueue;
	private final AdapterView<?> adapterView;
	private final Handler handler;

	/**
	 * Polls the blocking queue for work, executes on LoadEntries.
	 */
	private Runnable getImageGrabber() {
		return new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						tryLoadingNextImage( );
					} catch (InterruptedException e1) {
						// Skip along.
					} catch (final IOException e) {
						// Skip along.
					}

				}

			}

			
		};
	}
	/**
	 * Method removes top item from work queue, blocking until one becomes available if necessary.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void tryLoadingNextImage( ) throws InterruptedException, IOException
	{
		LoadEntry currentEntry = workQueue.takeFirst();
		for (final LoadPair pair : currentEntry.list) {
			final URL newurl = pair.url;
			if (pair.callback != null && newurl != null) {
				boolean success = true;
				HttpClient client = new DefaultHttpClient();
				HttpGet getRequest = null;
				try {
					getRequest = new HttpGet(newurl.toURI());
				} catch (URISyntaxException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
				    HttpResponse response = client.execute(getRequest);
				    final int statusCode = response.getStatusLine().getStatusCode();
				    if (statusCode != HttpStatus.SC_OK) {
				    	success = false;
				    }
				    URL url = null;
				    final HttpEntity entity = response.getEntity();
				    if (entity != null) {
				        InputStream inputStream = null;
				        try {
				            inputStream = entity.getContent();
//				            url = new URL(getParsedDataWikisDetails(inputStream));
							final URLConnection connection = newurl.openConnection();
//							final URLConnection connection = url.openConnection();
							// If you have a cache implementation, use it.
							connection.setUseCaches(true);

							final Bitmap out = BitmapFactory
									.decodeStream(connection
											.getInputStream());
							final ImageView oldcb = pair.callback;
							if (oldcb != null) {
								// Post to callback in the UI thread.
								handler.post(new Runnable() {
									@Override
									public void run() {
										final ImageView oldcb = pair.callback;
										if (oldcb != null)
											oldcb.setImageBitmap(out);
									}
								});
							}				            
				            
				            
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
				
//				final URLConnection connection = newurl.openConnection();
//				// If you have a cache implementation, use it.
//				connection.setUseCaches(true);
//
//				final Bitmap out = BitmapFactory
//						.decodeStream(connection
//								.getInputStream());
//				final ImageView oldcb = pair.callback;
//				if (oldcb != null) {
//					// Post to callback in the UI thread.
//					handler.post(new Runnable() {
//						@Override
//						public void run() {
//							final ImageView oldcb = pair.callback;
//							if (oldcb != null)
//								oldcb.setImageBitmap(out);
//						}
//					});
//				}
			}
		}
	}
	/**
	 * 
	 * @param parentView
	 */
	public AdapterImageLoader(AdapterView<?> parentView) {
		handler = new Handler(); // Affinity to the UI thread.
		adapterView = parentView;
		workQueue = new FixedStack<LoadEntry>();

		// Load up threads
		for (int i = 0; i < imagePoolSize; i++) {
			imagePool.execute(getImageGrabber());
		}
	}

	/**
	 * You must make only one call to this class per row entry. Use this for a
	 * single image per row or use the addLoadPair() functions.
	 * 
	 * @param url
	 * @param callback
	 */
	public void addImage(String url, ImageView callback) {
		LoadEntry le = new LoadEntry();
		le.list = new ArrayList<LoadPair>();
//		URI uri = null;
//		try {
//			uri = new URI("http://www.wikia.com/wikia.php?controller=WikisApi&method=getDetails&ids=" + id);
//		} catch (URISyntaxException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		URL url = null;
//		try {
//			url = uri.toURL();
//		} catch (MalformedURLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
////		URL url = createUrlFromDetail(id);
//		Connection c = new Connection(callback);
//		c.execute(id);
		
		try {
			le.list.add(new LoadPair(new URL(url), callback));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		addToWorkQueue(le);
	}

//	private URL createUrlFromDetail(String id) {
//		URI uri = null;
//		try {
//			uri = new URI("http://www.wikia.com/wikia.php?controller=WikisApi&method=getDetails&ids=" + id);
//		} catch (URISyntaxException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
////		final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
//		HttpClient client = new DefaultHttpClient();
//		final HttpGet getRequest = new HttpGet(uri);
//		try {
//		    HttpResponse response = client.execute(getRequest);
//		    final int statusCode = response.getStatusLine().getStatusCode();
//		    if (statusCode != HttpStatus.SC_OK) {
//		        return null;
//		    }
//
//		    final HttpEntity entity = response.getEntity();
//		    if (entity != null) {
//		        InputStream inputStream = null;
//		        try {
//		            inputStream = entity.getContent();
//		            final URL url = new URL(getParsedDataWikisDetails(inputStream));
//		            return url;
//		        } finally {
//		            if (inputStream != null) {
//		                inputStream.close();
//		            }
//		            entity.consumeContent();
//		        }
//		    }
//		} catch (Exception e) {
//		    // Could provide a more explicit error message for IOException or
//		    // IllegalStateException
//		    getRequest.abort();
//		    Log.w("ImageDownloader", "Error while retrieving bitmap from " 
//		    		+ "http://www.wikia.com/wikia.php?controller=WikisApi&method=getDetails&ids=" );
//		} 
//		return null;
//	}
	
//	public String getParsedDataWikisDetails(InputStream is) {
//		if(is == null){
//			return null;
//		}
//		String url = null;
//		ArrayList<Wikis_item> items = new ArrayList<Wikis_item>();
//		try{
//			JsonFactory factory = new JsonFactory();
//			JsonParser jsonParser = factory.createParser(is);
//			JsonToken token = jsonParser.nextToken();
//			if (token == JsonToken.START_OBJECT) {
//				while (token != JsonToken.END_OBJECT) {					
//					token = jsonParser.nextToken();
//					if (token == JsonToken.START_OBJECT) {
//						while (token != JsonToken.END_OBJECT) {
//							token = jsonParser.nextToken();
//							if (token == JsonToken.START_OBJECT) {
//								while (token != JsonToken.END_OBJECT) {
//									token = jsonParser.nextToken();
//									if(token == JsonToken.START_OBJECT){
//										while (token != JsonToken.END_OBJECT) {
//											token = jsonParser.nextToken();
//										}
//										token = jsonParser.nextToken();
//									}
//									if(token == JsonToken.START_ARRAY){
//										while (token != JsonToken.END_ARRAY) {
//											token = jsonParser.nextToken();
//										}
//										token = jsonParser.nextToken();
//									}
//									if (token == JsonToken.FIELD_NAME) {
//										String objectName = jsonParser.getCurrentName();
//										if (0 == objectName.compareToIgnoreCase("image")) {
//											jsonParser.nextToken();
//											url = jsonParser.getValueAsString();
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return url;
//	}
	
	
	
	private void addToWorkQueue(LoadEntry le) {
		for (LoadPair p : le.list) {
			LoadPair old = cache.put(p.callback, p);
			if (old != null) {
				// Prevent callback from firing on old view.
				old.callback = null;
			}
		}
		workQueue.add(le);
	}

	/**
	 * You must make only one call to this class per row entry! For multiple
	 * images per row use addLoadPairs()
	 * 
	 * @param loadPair
	 */
	public void addLoadPair(LoadPair loadPair) {
		LoadEntry le = new LoadEntry();
		le.list = new ArrayList<LoadPair>();
		le.list.add(loadPair);
		addToWorkQueue(le);
	}

	/**
	 * You must make only one call to this class per row entry.
	 * 
	 * @param loadPairs
	 */
	public void addLoadPairs(List<LoadPair> loadPairs) {
		LoadEntry le = new LoadEntry();
		le.list = loadPairs;
		addToWorkQueue(le);
	}
	
//	private class Connection extends AsyncTask<String, Void, URL > {
//	    private ImageView callback;
//
//		public Connection(ImageView imView) {
//			this.callback = imView;
//		}
//
//		@Override
//
//	    protected URL doInBackground(String... id) {
//			URI uri = null;
//			try {
//				uri = new URI("http://www.wikia.com/wikia.php?controller=WikisApi&method=getDetails&ids=" + id);
//			} catch (URISyntaxException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//			
////			final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
//			HttpClient client = new DefaultHttpClient();
//			final HttpGet getRequest = new HttpGet(uri);
//			try {
//			    HttpResponse response = client.execute(getRequest);
//			    final int statusCode = response.getStatusLine().getStatusCode();
//			    if (statusCode != HttpStatus.SC_OK) {
//			        return null;
//			    }
//
//			    final HttpEntity entity = response.getEntity();
//			    if (entity != null) {
//			        InputStream inputStream = null;
//			        try {
//			            inputStream = entity.getContent();
//			            final URL url = new URL(getParsedDataWikisDetails(inputStream));
//			            return url;
//			        } finally {
//			            if (inputStream != null) {
//			                inputStream.close();
//			            }
//			            entity.consumeContent();
//			        }
//			    }
//			} catch (Exception e) {
//			    // Could provide a more explicit error message for IOException or
//			    // IllegalStateException
//			    getRequest.abort();
//			    Log.w("ImageDownloader", "Error while retrieving bitmap from " 
//			    		+ "http://www.wikia.com/wikia.php?controller=WikisApi&method=getDetails&ids=" );
//			} 
//			return null;
//
//	    }
//	    
//	    protected void onPostExecute(URL url) {
//			LoadEntry le = new LoadEntry();
//			le.list = new ArrayList<LoadPair>();
//			le.list.add(new LoadPair(url, callback));
//			addToWorkQueue(le);
//	    }
//	    
//	}
	
}