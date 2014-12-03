package com.amra.backend;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.cloudfoundry.runtime.env.CloudEnvironment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

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
			String vcap = System.getenv("VCAP_SERVICES");
			if (vcap!=null){
				JSONObject vcapServices = new JSONObject(vcap);
				if (vcapServices.has("mongodb-2.4")) {
					JSONObject credentials = vcapServices.getJSONArray("mongodb-2.4").getJSONObject(0).getJSONObject("credentials");
					String connURL = credentials.getString("url");
			        mongo = new MongoClient(new MongoClientURI(connURL));

				}
			} else {
			   mongo = new MongoClient( "localhost" , 27017 );
			}
			 DB db = mongo.getDB(DB_NAME);
			 PATHS_COLLECTION = db.getCollection(COLLECTION_NAME);
			 
		} catch (UnknownHostException e) {
			LOGGER.error("Connection to MongoDB failed");
		} catch (Exception e) {
			LOGGER.error("Failed: " + e.getMessage());
	        e.printStackTrace();
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
	 * Get a path from the DB, based on its id
	 * @param pathId
	 * @return
	 */
	@RequestMapping(method=RequestMethod.GET)
	public String getPathsByLocation(@RequestParam("lat") float latitude, @RequestParam("long") float longitude, 
			@RequestParam(value = "dist") float distance) {
		LOGGER.info("Get request on path from location : [latitude:" + latitude + ", longitude:" + longitude + "]");	
		 
		// variation of .001 in lat or long is a 100m variation on earth's surface
		float dist = distance * 0.00001f;
		BasicDBObject searchQuery = new BasicDBObject();
		List<BasicDBObject> searchArguments = new ArrayList<BasicDBObject>();
		searchArguments.add(new BasicDBObject("checkpoints.0.latitude", new BasicDBObject("$lte", latitude + dist)
			.append("$gte", latitude - dist)));
		searchArguments.add(new BasicDBObject("checkpoints.0.longitude", new BasicDBObject("$lte", longitude + dist)
		.append("$gte", longitude - dist)));
		searchQuery.put("$and", searchArguments);
	 
		DBCursor cursor = PATHS_COLLECTION.find(searchQuery);
//		 
		JSONObject result = new JSONObject() ;
		JSONArray pathsArray = new JSONArray();
		while (cursor.hasNext()) {
			JSONObject path = new JSONObject(cursor.next().toString());
			 pathsArray.put(path);
			LOGGER.info("Found path: " + path.toString() + " in DB");
		}
		
		result.put("paths", pathsArray);
		return result.toString();
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

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getServiceURI() throws Exception {
	    CloudEnvironment environment = new CloudEnvironment();
	    if ( environment.getServiceDataByLabels("mongodb-2.4").size() == 0 ) {
	        throw new Exception( "No MongoDB service is bund to this app!!" );
	    } 

	    Map credential = (Map)((Map)environment.getServiceDataByLabels("mongodb-2.4").get(0)).get( "credentials" );
	 
	    return (String)credential.get( "url" );
	  }
}