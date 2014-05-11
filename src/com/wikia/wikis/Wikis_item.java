package com.wikia.wikis;

import java.io.IOException;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class Wikis_item {

//	wiki title, wiki url, wiki thumbnail image
		
		private String id;
		private String title;
		private String wiki_url;
		private String wiki_url_thumbnail_image;


		public Wikis_item (){
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		/**
		 * 
		 * @return wiki title
		 */
		public String getTitle() {
			return title;
		}

		/**
		 * wiki title
		 * @param title
		 */
		public void setTitle(String title) {
			this.title = title;
		}

		/**
		 * 
		 * @return wiki url
		 */
		public String getWikis_url() {
			return wiki_url;
		}

		/**
		 * wiki url
		 * @param wiki_url
		 */
		public void setWikis_url(String wiki_url) {
			this.wiki_url = wiki_url;
		}	
		
		/**
		 * 
		 * @return wiki url thumbnail image
		 */
		public String getImage_url() {
			return wiki_url_thumbnail_image;
		}

		/**
		 * wiki url thumbnail image
		 * @param url_thumbnail_image
		 */
		public void setImage_url(String url_thumbnail_image) {
			this.wiki_url_thumbnail_image = url_thumbnail_image;
		}

	}

