package com.getabine.logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class JamesErrorMailParser {
	
	public static LinkedHashMap<String, Integer> domainCount = new LinkedHashMap<String, Integer>();
	public static LinkedHashMap<String, Integer> errorSenderDomainCount = new LinkedHashMap<String, Integer>();

	public static void parseErrorMail(String filePath){
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filePath));
			//out.println("SMTP LOGS " + filePath + " PARSED @ " + new Timestamp(new Date().getTime()));

			String line, tld;
			Integer numCounts;

			while ((line = br.readLine()) != null) {
				if(line.startsWith("Failed recipient")){
					while(!(line = br.readLine()).startsWith("Error message")){
						int indexOfAtSign = line.indexOf('@');
						if(indexOfAtSign > 0){
							tld = Utils.parseTLD(line.substring(indexOfAtSign+1, line.length()));
							numCounts = domainCount.get(tld);

							if(numCounts != null){
								//System.out.println("found a key");
								domainCount.put(tld, numCounts+1);
							} else {
								domainCount.put(tld, new Integer(1));
							}
							
						}
					}				
					
				} else if(line.startsWith("From: postmaster@opayq.com")){
					System.out.println("from line " + line);
					if( (line = br.readLine()).indexOf("To: ") > -1){
						System.out.println("To: line" + line);
						if(line.indexOf("@") > -1){
							
						
						System.out.println("parse this tld: "+ line.substring(line.indexOf("@"), line.length()));
						tld = Utils.parseTLD(line.substring(line.indexOf("@") + 1, line.length()));
						} else {
							tld = "NOATSIGN";
						}
						
						System.out.println("storing this tld: " + tld);
						numCounts = errorSenderDomainCount.get(tld);

						if(numCounts != null){
							//System.out.println("found a key");
							errorSenderDomainCount.put(tld, numCounts+1);
						} else {
							errorSenderDomainCount.put(tld, new Integer(1));
						}
					}
				} else {
					continue;
				}
			}

			br.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * @param linkedHashMap
	 * @param connectionURL
	 * @param user
	 * @param password
	 */
	public static void storeToDatabase(LinkedHashMap<String, Integer> linkedHashMap, String connectionURL, String user, String password){
		Connection conn = null;

		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://"+connectionURL, user, password);
		} catch (Exception ex){

		} finally {
			if(conn != null){
				try{
					conn.close();
				}catch (Exception ex){

				}
			}
		}

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File folder = new File(args[0]);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
		    if (file.isFile()) {	
		        parseErrorMail(file.getAbsolutePath());
		    }
		}
		
		Utils.printOutput(domainCount, args[1]);
		Utils.printOutput(errorSenderDomainCount, args[2]);

	}

}
