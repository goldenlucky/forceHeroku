/**
 * 
 */
package agrex.sforce;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author e9900141
 * 
 */
public class SMain {
	public static String SETTING_PATH = "output/setting.properties";
    private static Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
    private static String objectId ;
	private static String baseUri;
	private static Header oauthHeader;
	private static String sObject;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
		String xmlLink = args[0];
		String csvLink = args[1];
		SResource res = new SResource();
		List<Map<String, String>> lstMapCSV = null;
		/**
		 * Read setting.properties files
		 */
		// Get Salesforce Username
		res.setStr_sf_Username(SUtil.readSetting(SConst.SETTING_USERNAME));
		// Get Salesforce Password
		res.setStr_sf_Password(SUtil.readSetting(SConst.SETTING_PASSWORD));
		// Get Salesforce LoginURL
		res.setStr_sf_LoginUrl(SUtil.readSetting(SConst.SETTING_LOGINURL));
		// Get Salesforce GrantServices
		res.setStr_sf_GrantServices(SUtil.readSetting(SConst.SETTING_GRANTSERVICE));
		// Get Salesforce ClientId
		res.setStr_sf_ClientId(SUtil.readSetting(SConst.SETTING_CLIENTID));
		// Get Salesforce ClientSecret
		res.setStr_sf_ClientSecret(SUtil.readSetting(SConst.SETTING_CLIENTSECRET));
		// Get Salesforce RestEndpoint
		res.setStr_sf_RestEndpoint(SUtil.readSetting(SConst.SETTING_REST_ENDPOINT));
		// Get Salesforce ApiVersion
		res.setStr_sf_ApiVersion(SUtil.readSetting(SConst.SETTING_API_VERSION));

		/**
		 * Connect to Salesforce
		 */
		// Login request URL
		HttpClient httpclient = HttpClientBuilder.create().build();
		String loginURL = res.getStr_sf_LoginUrl() +
				res.getStr_sf_GrantServices()  +
                "&client_id=" + res.getStr_sf_ClientId() +
                "&client_secret=" + res.getStr_sf_ClientSecret() +
                "&username=" + res.getStr_sf_Username() +
                "&password=" + res.getStr_sf_Password();
        HttpPost httpPost = new HttpPost(loginURL);
        HttpResponse response = null;
        response = httpclient.execute(httpPost);
        
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            System.out.println("Error authenticating to Force.com: "+statusCode);
            return;
        }
 
        String getResult = null;
        getResult = EntityUtils.toString(response.getEntity());
 
        JSONObject jsonObject = null;
        String loginAccessToken = null;
        String loginInstanceUrl = null;
 
        try {
            jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
            loginAccessToken = jsonObject.getString("access_token");
            loginInstanceUrl = jsonObject.getString("instance_url");
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
 
        baseUri = loginInstanceUrl + res.getStr_sf_RestEndpoint() + res.getStr_sf_ApiVersion() ;
        oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken) ;
        System.out.println("Successful login");
    
        //Read Table Node
    	NodeList lstMain = SUtil.readXMLFile(xmlLink, "table");

		Node mainNode = lstMain.item(0);
		if (mainNode.getNodeType() == Node.ELEMENT_NODE) {

			Element eField = (Element) mainNode;
			String sMode = eField.getAttribute("mode").toLowerCase();
			String isHeader = eField.getAttribute("header").toLowerCase();
			sObject = eField.getAttribute("sobject");
			 /**
			 * Read file CSV and Map
			 */
			lstMapCSV = SUtil.readCSVFile(csvLink ,isHeader);
		
			/**
			 * Upload, Update, Delete to Salesforce Object
			 */
			upsertDeleteCSVData(isHeader, xmlLink, sMode, lstMapCSV);
			
		}
       
		httpPost.releaseConnection();
		} catch (ClientProtocolException cpException) {
			cpException.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

	}
	
	//Upload, Update, Delete CSV Data
	public static void upsertDeleteCSVData(String isHeader,String xmlLink
							, String mode, List<Map<String, String>> lstMapCSV) {
			List<String> lstData = null;
	        String keySField = null;
	        JSONObject jsonCSV = null;
	        try {
	        	
	        	NodeList lstCol = SUtil.readXMLFile(xmlLink, "column");
	        	lstData = new ArrayList<String>();
	            //Insert, Update, Delete into Object
	        	for (Map<String, String> m : lstMapCSV) {
	        		String keyField = null;
	        		JSONObject sfObject = new JSONObject();
	        		List<String> lstColumn = new ArrayList<String>();
		    		for (int temp = 0; temp < lstCol.getLength(); temp++) {
		    			Node colNode = lstCol.item(temp);
		    			
		    			if (colNode.getNodeType() == Node.ELEMENT_NODE) {

		    				Element eField = (Element) colNode;
		    				String sfield = eField.getAttribute("sfield");
		    				String key = eField.getAttribute("key").toLowerCase();
		    				lstColumn.add(sfield);
		    				String csvData = isHeader.equals("true") ?  eField.getAttribute("csvfield"): eField.getAttribute("csvindex");
		    				String type = eField.getAttribute("type");
		    				if(key.equals("true")){
			    				keyField = csvData;
			    				keySField = sfield;
		    				}
		    				if(type.toLowerCase().equals("date")){
		    					String dateStr = m.get(csvData);
		    					Date dateFormat = Date.valueOf(dateStr.substring(0,4)
										   +"-" + dateStr.substring(4,6)
										   +"-" + dateStr.substring(6,8));
		    					sfObject.put(sfield, dateFormat);
		    				}else{
		    					sfObject.put(sfield, m.get(csvData));
		    				}
		    				
		    			}
		    			
		    		}
		    		//Get Object SF
		    		jsonCSV = getCSVObject(lstColumn);
		            
					if (mode.equals("insert")) {
						//Insert to SF Object
						insertCSVData(sfObject);
					} else if (mode.equals("update")){
						if(keyField != null){
							//Update to SF Object
							updateCSVData(keySField,m.get(keyField),jsonCSV,sfObject);
						}
					} else{
						//get list CSV Data
						lstData.add(m.get(keyField));
					}
			}
        	if (mode.equals("delete")) {
        		//Delete SF Object
        		deleteCSVData(lstData,keySField,jsonCSV);
        	}
        	
		} catch (JSONException e) {
			e.printStackTrace();
		}catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	//Insert CSV Data
    public static void insertCSVData(JSONObject sfObject) {
        try {
        	
        	HttpClient httpClient = HttpClientBuilder.create().build();
            StringEntity body = new StringEntity(sfObject.toString(1));
            body.setContentType("application/json");
            
        	String uri = baseUri + "/sobjects/" + sObject;
        	
        	//Use HttpPost to upload data
			HttpPost httpPost = new HttpPost(uri);
			httpPost.addHeader(oauthHeader);
			httpPost.addHeader(prettyPrintHeader);
			httpPost.setEntity(body);

			HttpResponse response = httpClient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			
			if (statusCode == 201) {
				String response_string = EntityUtils.toString(response.getEntity());
				JSONObject json = new JSONObject(response_string);
				objectId = json.getString("id");
				System.out.println("New Object has been insert with id: "+ objectId);
				
			} else {
				System.out.println("Unsuccessful. Status code returned is "+ statusCode);
			}
			
        } catch (JSONException e) {
            System.out.println("Issue creating JSON or processing results");
            e.printStackTrace();
        }catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
	//Update CSV Data
    public static void updateCSVData(String keySField,String mKey,JSONObject json,JSONObject sfObject) {
        String uri = null;
        try {
        	
             JSONArray j = json.getJSONArray("records");
             
             Map<String, String> mapObject = new HashMap<String, String>();
             
             for (int i = 0; i < j.length(); i++){
             		String record= json.getJSONArray("records").getJSONObject(i).getString(keySField);
             		
             		if(record.equals(mKey)){
             			mapObject.put(mKey, json.getJSONArray("records").getJSONObject(i).getString("Id"));
                	}
         	}
        	if(!mapObject.isEmpty()){
		        		HttpClient httpClient = HttpClientBuilder.create().build();
			            StringEntity body = new StringEntity(sfObject.toString(1));
			            body.setContentType("application/json");
			    
        			    uri = baseUri + "/sobjects/" + sObject + "/"+ mapObject.get(mKey);
	    			    
	    			    //Use HttpPatch to update data
	    			  	HttpPatch httpPatch = new HttpPatch(uri);
						httpPatch.addHeader(oauthHeader);
						httpPatch.addHeader(prettyPrintHeader);
						httpPatch.setEntity(body);
			 
			            HttpResponse response = httpClient.execute(httpPatch);
			 
			            int statusCode = response.getStatusLine().getStatusCode();
			            if (statusCode == 204) {
			            	System.out.println("Updated Object successfully.");
			            } else {
			            	System.out.println("Unsuccessful. Status code returned is "+ statusCode);
			            }
        	}else{
        		System.out.println("Nothing to Update");
        	}
        } catch (JSONException e) {
            System.out.println("Issue creating JSON or processing results");
            e.printStackTrace();
        }catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
	//Delete CSV Data
    public static void deleteCSVData(List<String> lstData,String keySField,JSONObject json) {
        String uri = null;
        try {
        	
             JSONArray j = json.getJSONArray("records");
             
             List<Map<String, String>> mapObject = new ArrayList<Map<String, String>>();
             Map<String, String> mapStr = new HashMap<String, String>();
             
             for (int i = 0; i < j.length(); i++){
             		String record= json.getJSONArray("records").getJSONObject(i).getString(keySField);
             		mapStr = new HashMap<String, String>();
                 		if(!lstData.contains(record)){
                 			mapStr.put("delete", json.getJSONArray("records").getJSONObject(i).getString("Id"));
                 			mapObject.add(mapStr);
                 		}
         	}
             
        	if(!mapObject.isEmpty()){
	        	for(Map<String,String> m:mapObject){
	    			uri = baseUri + "/sobjects/" + sObject + "/"+ m.get("delete");
	    			  HttpClient httpClient = HttpClientBuilder.create().build();
	    			
	    			  	//Use HttpDelete to delete data
			            HttpDelete httpDelete = new HttpDelete(uri);
			            httpDelete.addHeader(oauthHeader);
			            httpDelete.addHeader(prettyPrintHeader);
			 
			            HttpResponse response = httpClient.execute(httpDelete);
			 
			            int statusCode = response.getStatusLine().getStatusCode();
			            if (statusCode == 204) {
			                System.out.println("Deleted csv data successfully with ID:" + m.get("delete"));
			            } else {
			                System.out.println("Delete NOT successful. Status code is " + statusCode);
			            }
	        	}
        	}else{
        		System.out.println("Nothing to delete");
        	}
        } catch (JSONException e) {
            System.out.println("Issue creating JSON or processing results");
            e.printStackTrace();
        }catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
    //Get Data from SF Object
    public static JSONObject getCSVObject(List<String> lstColumn) {
    	JSONObject json = null;
    	try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            String uri = baseUri + "/query?q=Select+ID";
			 for(String a:lstColumn){
				 uri += "+,+" + a;
			 }
			 uri += "+From+"+ sObject;
			
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader(oauthHeader);
            httpGet.addHeader(prettyPrintHeader);
 
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            
            if (statusCode == 200) {
            	String response_string = EntityUtils.toString(response.getEntity());
                json = new JSONObject(response_string);
               
            } else {
            	System.out.println("Get data from SF object error");
                System.exit(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return json;
    }

}
