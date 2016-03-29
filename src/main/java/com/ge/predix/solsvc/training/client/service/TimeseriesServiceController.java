package com.ge.predix.solsvc.training.client.service;

import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.ge.predix.solsvc.restclient.impl.RestClient;

@RestController
public class TimeseriesServiceController {
	
	private static Logger log = Logger.getLogger(TimeseriesServiceController.class);
	
	@Autowired
	public TimeseriesServiceImpl tsimpl;
	
	@Autowired
	@Qualifier("restClient")
	public  RestClient rest;
		
	static String authToken ;
	
	@RequestMapping(value = "/locomotive/tags", method = RequestMethod.GET)
	public String retrieveTags() {

		log.info("TimeseriesServiceController: retrieveTags ");

		String timeSeriesTags = tsimpl.timeseries("tags", null);

		return timeSeriesTags;
	}
	

	@RequestMapping(value = "/locomotive/datapoints", method = RequestMethod.GET)
	public String retrieveDatapoints() {

		log.info("TimeseriesServiceController: retrievedatapoints ");

		String timeSeriesDataPoints = tsimpl.timeseries("data", null);

		return timeSeriesDataPoints;
	}
	
	
	@RequestMapping(value = "/locomotive/acslatest", method = { RequestMethod.GET, RequestMethod.POST })
	public String retrieveLatest(@RequestParam("id") String id,
			@RequestParam(value = "username", required = true) String username,
			@RequestParam(value = "password", required = true) String password
			) throws RestClientException, URISyntaxException {
			
		log.info("TimeseriesServiceController: retrieveACSLatest Inside Locomotive/latest >>>>>>>>> ");	
	
		
		
		String timeSeriesLatestDataPoints = tsimpl.acsretrieveLatestPoints(username, password, id );

		return timeSeriesLatestDataPoints;
	}
	
	@RequestMapping(value = "/locomotive/latest", method = { RequestMethod.GET, RequestMethod.POST })
	public String retrieveLatest(@RequestParam("id") String id,
			@RequestParam(value = "username", required = true) String username
			
			)
			throws RestClientException, URISyntaxException {

		log.info("TimeseriesServiceController: <<<<<<<retrieveONLYLatest Inside Locomotive/latest >>>>>>>>> ");	
	
		
		String timeSeriesLatestDataPoints = tsimpl.timeseries("latest", id );

		return timeSeriesLatestDataPoints;
	}
	
	
	//------------------------------------------ACS -----------------------------------------
	
	@SuppressWarnings("nls")
    @RequestMapping(value = "/validateuser", method = { RequestMethod.GET, RequestMethod.POST })
    public @ResponseBody String validateUser() throws Exception {
		
			// Get token based on the client_credentials
			log.info("TimeseriesServiceController: validateUser :: getting token based on the client_credentials");
			String auth = tsimpl.validateUser();
			return auth;
	}

}
