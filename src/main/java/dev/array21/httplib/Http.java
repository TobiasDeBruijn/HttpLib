/**
 * HTTP Library for sending HTTP requests
 * @author Tobias de Bruijn
 */

package dev.array21.httplib;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Http {

	private boolean debugMode = false;

	public Http() {}
	
	public Http(boolean debugMode) {
		this.debugMode = debugMode;
	}

	/**
	 * Send a HTTP Request
	 * @param method Method to use (e.g POST)
	 * @param targetUrl URL to send the request to (e.g https://example.com)
	 * @param params URL Parameters to be used
	 * @return Returns a ResponseObject
	 * @throws MalformedURLException Thrown when an invalid URL is given
	 * @throws IOException Thrown when an IOException occurs
	 */
	public ResponseObject makeRequest(RequestMethod method, String targetUrl, HashMap<String, String> params, MediaFormat requestBodyFormat, String requestBody, HashMap<String, String> customHeaders) throws MalformedURLException, IOException {
		//Turn the HashMap of parameters into a String
		String sParams = "";
		if(params != null) {
			sParams = "?" + hashMapToString(params);
		}
		
		//Determine the request method
		String sMethod = "";
		switch(method) {
		case GET: 
			sMethod = "GET"; 
			break;
		case POST: 
			sMethod = "POST"; 
			break;
		case PUT:
			sMethod = "PUT";
			break;
		case DELETE:
			sMethod = "DELETE";
			break;
		}

		//Create the URL, open a connection and connect.
    	final URL url = new URL(targetUrl + sParams);
    	final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    	conn.setRequestMethod(sMethod);
    	conn.setDoOutput(true);

		if(customHeaders != null) {
			for(Map.Entry<String, String> entry : customHeaders.entrySet()) {
				conn.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}
    	
       	if(requestBody != null) {
    		byte[] postData = requestBody.getBytes(StandardCharsets.UTF_8);    		
       		
    		conn.setRequestProperty("Content-Type", requestBodyFormat.getApplicationType());
    		conn.setRequestProperty("Charset", "utf-8");
    		conn.setUseCaches(false);
    		conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
    		
    		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
    		wr.write(postData);
    		wr.flush();
    	} else {
    		if(method == RequestMethod.POST) {
        		requestBody = " ";
        		conn.setRequestProperty("Content-Length", Integer.toString(requestBody.getBytes(StandardCharsets.UTF_8).length));
        		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        		wr.write(requestBody.getBytes(StandardCharsets.UTF_8));
        		wr.flush();
    		}
    	}
       	       	
    	//Get the response message from the server
    	String result = "";
        try {
        	BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            result = reader.lines().collect(Collectors.joining("\n"));

        } catch(IOException e) {
            logDebug("HttpRequest: " + method + "::" + targetUrl);
            logDebug("HttpRequest: " + conn.getResponseCode() + "::" + conn.getResponseMessage());
            logDebug("HttpRequest ErrorStream: \n" + new BufferedReader(new InputStreamReader(conn.getErrorStream())).lines().collect(Collectors.joining("\n")));
            
        	BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        	result = reader.lines().collect(Collectors.joining("\n"));
        }
                
        conn.disconnect();
        
        Http http = new Http();
		return http.new ResponseObject(result, conn.getResponseCode(), conn.getResponseMessage());
	}
	
	private String hashMapToString(HashMap<String, String> input) {
		if(input == null) {
			return "";
		}
		
		final StringBuilder result = new StringBuilder();
		int index = 1;
		
		for(Map.Entry<String, String> entry : input.entrySet()) {			
			try {
				result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
				result.append("=");
				result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
			} catch(Exception e) {}

			
			//Check if we're not yet on the last iteration
			//If not, add the '&' character.
			if(index != input.size()) {
				result.append("&");
			}
			
			index++;
		}
		
		return result.toString();
	}
	
	public enum RequestMethod {
		GET,
		POST, 
		PUT,
		DELETE
	}
	
	public enum MediaFormat {
		JSON("application/json"),
		JPEG("application/jpeg"),
		PNG("application/png"),
		XML("application/xml"),
		X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded");
		
		private String applicationType;
		private MediaFormat(String applicationType) {
			this.applicationType = applicationType;
		}
		
		public String getApplicationType() {
			return this.applicationType;
		}
	}
	
	public class ResponseObject {
		
		private String content, connectionMessage;
		private int responseCode;
		
		public ResponseObject(String content, int responseCode, String connectionMessage) {
			this.content = content;
			this.responseCode = responseCode;
			this.connectionMessage = connectionMessage;
		}
		
		public String getMessage() {
			return this.content;
		}
		
		public int getResponseCode() {
			return this.responseCode;
		}
		
		public String getConnectionMessage() {
			return this.connectionMessage;
		}
	}

	private void logDebug(Object log) {
		if(!this.debugMode) return;

		//kk:mm:ss --> hour:minute:seconds, without hours going 0-24
		final DateTimeFormatter f = DateTimeFormatter.ofPattern("kk:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		System.out.println("[" + now.format(f) + "][DEBUG][HttpLib] " + log.toString());
	}
}