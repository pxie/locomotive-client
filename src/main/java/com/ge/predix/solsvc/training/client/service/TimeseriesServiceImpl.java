package com.ge.predix.solsvc.training.client.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ge.predix.solsvc.restclient.impl.RestClient;

@Component
public class TimeseriesServiceImpl implements EnvironmentAware {

	private static Logger log = Logger.getLogger(TimeseriesServiceImpl.class);

	@PostConstruct
	private void init() {
		System.out.println("Test");
	}

	@PreDestroy
	private void destroy() {
		System.out.println("Test");
	}

	@Autowired
	@Qualifier("restClient")
	public RestClient rest;
		
	@Autowired
    private HttpServletRequest context;
	
	@Value("${predix_oauthRestHost}")
	String predix_oauthRestHost;
	
	@Value("${predix_oauthClientId}")
	String predix_oauthClientId;
	
	@Value("${predix_oauthGrantType}")
	String predix_oauthGrantType;
	
	@Value("${timeseriesZone}")
	String timeseriesZone;
	
	@Value("${accessTokenEndpointUrl}")
	String accessTokenEndpointUrl;
	
	@Value("${clientId}")
	String clientId;
	
	@Value("${clientSecret}")
	String clientSecret;

	public String timeseries(String tag, String id) {

		boolean oauthClientIdEncode = true;
		String oauthPort = "80";
		String oauthResource = "/oauth/token";
		String proxyHost = null;
		String proxyPort = null;

		List<Header> headers = this.rest.getOauthHttpHeaders(this.predix_oauthClientId, oauthClientIdEncode);
		//Request Token fro UAA
		String tokenString = this.rest.requestToken(headers, oauthResource, this.predix_oauthRestHost, oauthPort, this.predix_oauthGrantType,
				proxyHost, proxyPort);

		log.debug("TOKEN = " + tokenString);

		//Token is stored as a JSON object
		JSONObject token = new JSONObject(tokenString);

		//Store Access_token part of the token
		String authorization = "Bearer " + token.getString("access_token");

		if (tag.equalsIgnoreCase("tags")) {
			String tags = retrieveTags(authorization);
			log.info("TimeseriesServiceImpl :: retrieveTags : response - " + tags);
			return tags;

		} else if (tag.equalsIgnoreCase("data")) {
			String dataPoints = retrieveDataPoints(authorization);

			try {
				JSONObject dataPointsObject = new JSONObject(dataPoints);

				JSONArray tagsArray = dataPointsObject.getJSONArray("tags");
				log.debug("tagsArray: " + tagsArray);

				for (int i = 0; i < tagsArray.length(); i++) {
					JSONObject jsonTagsObject = tagsArray.getJSONObject(i);

					log.debug("jsonTagsObject: " + jsonTagsObject.getJSONArray("results"));

					JSONArray resultsArray = jsonTagsObject.getJSONArray("results");
					for (int j = 0; j < resultsArray.length(); j++) {
						JSONObject jsonValueObject = resultsArray.getJSONObject(j);

						log.debug("jsonValueObject: " + jsonValueObject.getJSONArray("values"));

						for (int k = 0; k < jsonValueObject.getJSONArray("values").length(); k++) {
							JSONArray jsonArray = jsonValueObject.getJSONArray("values").getJSONArray(k);

							log.debug("value - 1 : " + jsonArray.get(0) + "value - 2: " + jsonArray.get(1)
									+ "value - 3: " + jsonArray.get(2));
						}
					}
				}

				return dataPointsObject.toString();

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (tag.equalsIgnoreCase("latest")) {
			String latestPoints = retrieveLatestPoints(authorization, id);

			try {
				JSONObject latestPointsObject = new JSONObject(latestPoints);
				return latestPointsObject.toString();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return "No DATA FOUND";
	}

	private String retrieveDataPoints(String authorization) {

		String timeSeriesUri = "https://time-series-store-predix.run.aws-usw02-pr.ice.predix.io/v1/datapoints";

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		//Add required Headers for the time series query 
		//Zone-ID, Bearer Token and JSON content Type
		headers.add("Predix-Zone-Id", this.timeseriesZone);
		headers.add("Authorization", authorization);
		headers.add("Content-Type", "application/json");

		String body = "{" +

		" 'start': '1d-ago'" + "," +

		" 'tags': [ " +

		" {" +

		" 'name': ['LOCOMOTIVE_1_location', 'LOCOMOTIVE_1_rpm', 'LOCOMOTIVE_1_torque']," +

		" 'limit': 100" +

		" }]}";
		MultiValueMap<String, String> postParameters = new LinkedMultiValueMap<String, String>();
		postParameters.add("content", body);

		try {

			HttpEntity<String> requestEntity = new HttpEntity<String>(body, headers);
			HttpEntity<String> response = restTemplate.exchange(timeSeriesUri, HttpMethod.POST, requestEntity,
					String.class);

			log.info("TimeseriesServiceImpl :: retrieveDataPoints : response ==============================> "
					+ response.getBody());

			return response.getBody();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private String retrieveTags(String authorization) {
		String timeSeriesUri = "https://time-series-store-predix.run.aws-usw02-pr.ice.predix.io/v1/tags";

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.add("Predix-Zone-Id", this.timeseriesZone);
		headers.add("Authorization", authorization);
		headers.add("Content-Type", "application/json");
		log.info("TimeseriesServiceImpl :: retrieveTags : headers - " + headers);
		try {

			HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
			HttpEntity<String> response = restTemplate.exchange(timeSeriesUri, HttpMethod.GET, entity, String.class);

			log.info("TimeseriesServiceImpl :: retrieveTags : response ----------->>>>>>>>>>>>>>>>> "
					+ response.getBody());

			return response.getBody();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private String retrieveLatestPoints(String authorization, String id) {

		String timeSeriesUri = "https://time-series-store-predix.run.aws-usw02-pr.ice.predix.io/v1/datapoints/latest";

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.add("Predix-Zone-Id", this.timeseriesZone);
		headers.add("Authorization", authorization);
		headers.add("Content-Type", "application/json");
		String body = "{" +

		" 'start': '1d-ago'" + "," +

		" 'tags': [ " +

		" {" +

		" 'name': ['" + id + "_location', '" + id + "_rpm', '" + id + "_torque']" +

		" }]}";
		MultiValueMap<String, String> postParameters = new LinkedMultiValueMap<String, String>();
		postParameters.add("content", body);

		try {

			HttpEntity<String> requestEntity = new HttpEntity<String>(body, headers);
			HttpEntity<String> response = restTemplate.exchange(timeSeriesUri, HttpMethod.POST, requestEntity,
					String.class);

			log.info("TimeseriesServiceImpl :: retrieveLatestPoints : response ==============================> "
					+ response.getBody());

			return response.getBody();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public String acsretrieveLatestPoints(String username, String password, String id) throws RestClientException, URISyntaxException {
		
		String response = null;
		
		//ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault());
		//---------------------------------------------------------------------------------------------------------------------
		
		 /*ClientCredentialsResourceDetails clientDetails = new ClientCredentialsResourceDetails();
	
		 clientDetails.setClientId("client");
		 clientDetails.setClientSecret("client");
		
		 String accessUrl = "https://328ea004-f3d2-464b-bbf8-8acbd5fa4575.predix-uaa-training.run.aws-usw02-pr.ice.predix.io/oauth/token";
		
		 clientDetails.setAccessTokenUri(accessUrl);
		 clientDetails.setGrantType("client_credentials");
		
		 OAuth2RestTemplate oauth2RestTemplate = new OAuth2RestTemplate(clientDetails);
		
		 log.info("TimeseriesServiceImpl :: Access Token - " + oauth2RestTemplate.getAccessToken().getValue());
		
		 String authorization = "Bearer " + oauth2RestTemplate.getAccessToken().getValue();
		 
		 log.info("AUTH TOKEN FROM CLIENT LEVEL ::::::::" +authorization);*/
		
		 //--------------------------------------------------------------------------------------------------------
		 //Z2V1c2Vy - geuser & Z2VvcGVyYXRvcg== - geoperator
		byte[] passwordDecoded= Base64.decodeBase64(password.getBytes());
		OAuth2RestTemplate restTemplate = getRestTemplate(username, new String(passwordDecoded));
		

		
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		String latestUrl = this.context.getRequestURL().toString().replace("/locomotive/acslatest", "/validateuser");
		log.info("XXXCalling LATEST URL::::::::: " + latestUrl);
		
		try {
			String token = null;
			// 3. Once the policy is evaluated to PERMIT , then the call is passed
			// forward to get the token based on client credentials and call retrieveLatestPoints()
			
			token = restTemplate.postForObject(new URI(latestUrl), new HttpEntity<>(map), String.class);
			
			log.info("AUTH TOKEN FROM USER LEVEL >>>>>>>>>>>>" + token );

			response = retrieveLatestPoints(token, id );
			
		} // 4. If the policy evaluated condition is resolved to DENY, this
			// then raises an OAuth2AccessDeniedException and the same exception
			// is returned back as response.
		catch (OAuth2AccessDeniedException e) {
			log.error(
					"Error validating user " + username + " with following error " + e.getCause() + e.getMessage() + e);
			throw new RuntimeException(e);

		}	

		return response;

	}
	
    public String validateUser() throws Exception {
		
			// Get token based on the client_credentials 
			log.info("TimeseriesServiceImpl :: retrieveDataPoints : getting token based on the client_credentials");
			String auth = null;
			try {
				boolean oauthClientIdEncode = true;
				String oauthPort = "80";
				String oauthGrantType = this.predix_oauthGrantType;
				String oauthResource = "/oauth/token";
				String proxyHost = null;
				String proxyPort = null;
				String oauthClientId = this.predix_oauthClientId;
				String oauthHost = this.predix_oauthRestHost;

				List<Header> headers = this.rest.getOauthHttpHeaders(oauthClientId, oauthClientIdEncode);
				String tokenString = this.rest.requestToken(headers, oauthResource, oauthHost, oauthPort,
						oauthGrantType, proxyHost, proxyPort);

				log.debug("TOKEN = " + tokenString);

				JSONObject token = new JSONObject(tokenString);

				auth = "Bearer " + token.getString("access_token");

			} catch (HttpClientErrorException hce) {
				throw new Exception(hce);
			}
			return auth;
		}
	
	@Override
	public void setEnvironment(Environment env) {
		//Pull from the environment variables the UAA URL, Client ID and client secret
		this.predix_oauthRestHost = env.getProperty("predix_oauthRestHost");
		this.predix_oauthClientId = env.getProperty("predix_oauthClientId");		
		this.predix_oauthGrantType = env.getProperty("predix_oauthGrantType");
		this.accessTokenEndpointUrl = env.getProperty("accessTokenEndpointUrl");
		this.clientId = env.getProperty("clientId");
		this.clientSecret = env.getProperty("clientSecret");
	}

	private OAuth2RestTemplate getRestTemplate(String username, String password) {
		// get token here based on username password;
		ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
		resourceDetails.setUsername(username);
		resourceDetails.setPassword(password);		

		String url = this.accessTokenEndpointUrl;
		resourceDetails.setAccessTokenUri(url);		
		resourceDetails.setClientId(this.clientId);
		resourceDetails.setClientSecret(this.clientSecret);

		OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resourceDetails);

		return restTemplate;
	}

}
