package com.amra.backend;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/paths")
public class PathsController {
	
	private static Logger LOGGER = Logger.getLogger(PathsController.class);

	@RequestMapping(value="/{id}", method=RequestMethod.GET)
	public String getPathById(@PathVariable("id") UUID pathId) {
		LOGGER.info("Get request on path [" + pathId + "]");
		return new JSONObject(
				"{"
					+ "id:" + pathId + ","
					+ "title:Path1,"
					+ "description: Test path"
				+ "}").toString();
	}

}