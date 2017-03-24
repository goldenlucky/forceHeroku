package agrex.sforce;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import au.com.bytecode.opencsv.CSVReader;

import procesing.Util;

public class SUtil {
	public static Properties prop;

	public static String readSetting(String param) {

		if (prop == null) {

			prop = Util.readSetting(SMain.SETTING_PATH);
		}
		return prop.getProperty(param);
	}

	//Read CSV File
	public static List<Map<String, String>> readCSVFile(String linkFile, String isHeader) {
		List<Map<String, String>> lstMapCSV = null;
		try {
			String csvFilename = linkFile;
			CSVReader csvReader = new CSVReader(new FileReader(csvFilename));
			String[] cols = null;
			lstMapCSV = new ArrayList<Map<String, String>>();
			Map<String, String> map = new HashMap<String, String>();
			//Have Header
			if(isHeader.equals("true")){
				String[] header = csvReader.readNext();
				while ((cols = csvReader.readNext()) != null) {
					map = new HashMap<String, String>();
					for (int i = 0; i < header.length; i++) {
						map.put(header[i], cols[i]);
					}
					lstMapCSV.add(map);
				}
			}else{
				//Not have Header
				while ((cols = csvReader.readNext()) != null) {
					map = new HashMap<String, String>();
					for (int i = 0; i < cols.length; i++) {
						map.put(String.valueOf(i), cols[i]);
					}
					lstMapCSV.add(map);
				}
			}
			
			csvReader.close();

		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lstMapCSV;
	}

	//Read XML File
	public static NodeList readXMLFile(String xmlLink, String tagName) {
		NodeList lstNode = null;
		try {
			File xmlFile = new File(xmlLink);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);

			lstNode = doc.getElementsByTagName(tagName);
			
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lstNode;
	}
	
}
