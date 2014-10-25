package com.amra.backend;

import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

//TODO: move the DB relation into a service ! If we want to do proper MVC...
@RestController
@RequestMapping("/paths")
public class PathsController {
	
	private static Logger LOGGER = Logger.getLogger(PathsController.class);
	private static DBCollection PATHS_COLLECTION ;
	
	/**
	 * MongoDB informations 
	 */
	private static String DB_NAME = "AMRA-DB";
	private static String COLLECTION_NAME = "paths";
	
	/**
	 * Constructor. Connect to the DB
	 */
	public PathsController() {
		MongoClient mongo;
		try {
			 mongo = new MongoClient( "localhost" , 27017 );
			 DB db = mongo.getDB(DB_NAME);
			 PATHS_COLLECTION = db.getCollection(COLLECTION_NAME);
			 
		} catch (UnknownHostException e) {
			LOGGER.error("Connection to MongoDB failed");
		}		
	}

	/**
	 * Get a path from the DB, based on its id
	 * @param pathId
	 * @return
	 */
	@RequestMapping(value="/{id}", method=RequestMethod.GET)
	public String getPathById(@PathVariable("id") UUID pathId) {
		LOGGER.info("Get request on path [" + pathId + "]");
		JSONObject pathJSON = new JSONObject();
		pathJSON.put("id", pathId);
		pathJSON.put("title", "path1");
		pathJSON.put("description", "test path");		
		 
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("id", pathId.toString());
	 
		DBCursor cursor = PATHS_COLLECTION.find(searchQuery);
		 
		String pathResult = null ;
		if (cursor.hasNext()) {
			 pathResult = cursor.next().toString();
			LOGGER.info("Found path : " + pathResult + " in DB");
		}
		
		return pathResult;
	}
	
	/**
	 * Create a path in the DB from the request body
	 * @param path
	 */
	@RequestMapping(method=RequestMethod.POST)
	public void createPath(@RequestBody String path) {
		LOGGER.info("POST request received with body [" + path + "]");
		try {
			JSONObject pathJSON = new JSONObject(path);
			UUID id = UUID.randomUUID();
			pathJSON.put("id", id);
			BasicDBObject pathDB = (BasicDBObject) com.mongodb.util.JSON.parse(pathJSON.toString());
			PATHS_COLLECTION.insert(pathDB);	
			LOGGER.info("Path [" + pathJSON.toString() + "] added to DB");
		}
		catch(JSONException e) {
			LOGGER.error("Path format in request body is not a valid JSON Object");
		}		
	}

}