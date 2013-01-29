package com.getabine.logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

public class JamesErrorMailParser {

    /**
     *  failedRecipientDomain is hash between domain -> # of times that domain was the intended recipient of a mail that ended in delivery failure
     */
	public static LinkedHashMap<String, Integer> failedRecipientDomain = new LinkedHashMap<String, Integer>();
	/**
	 * errorSenderDomainCount is a hash between domain -> # of times that domain was the sender of a mail that ended in delivery failure
	 */
	public static LinkedHashMap<String, Integer> errorSenderDomainCount = new LinkedHashMap<String, Integer>();

	/**
	 * 
	 * @param filePath
	 */
	public static void parseErrorMail(String filePath){
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filePath));

			String line, tld;
			Integer numCounts;

			while ((line = br.readLine()) != null) {
				if(line.startsWith("Failed recipient")){
					while(!(line = br.readLine()).startsWith("Error message")){
						int indexOfAtSign = line.indexOf('@');
						if(indexOfAtSign > 0){
							tld = Utils.parseTLD(line.substring(indexOfAtSign+1, line.length()));
							numCounts = failedRecipientDomain.get(tld);

							if(numCounts != null){
								//System.out.println("found a key");
								failedRecipientDomain.put(tld, numCounts+1);
							} else {
								failedRecipientDomain.put(tld, new Integer(1));
							}

						}
					}				

				} else if(line.startsWith("From: postmaster@opayq.com")){
					if( (line = br.readLine()).indexOf("To: ") > -1){
						//System.out.println("To: line" + line);
						if(line.indexOf("@") > -1){
							tld = Utils.parseTLD(line.substring(line.indexOf("@") + 1, line.length()));
						} else {
							tld = "NOATSIGN";
						}

						numCounts = errorSenderDomainCount.get(tld);

						if(numCounts != null){
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
	public static void storeSenderDomainsToDatabase(LinkedHashMap<String, Integer> linkedHashMap, String connectionURL, String user, String password){
		Connection conn = null;
		String currentTime = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://" + connectionURL, user, password);
			PreparedStatement ps = conn.prepareStatement("INSERT INTO daily_top_email_sender_delivery_error_stats (domain, num_errored, stat_date)" +
					"VALUES(?,?,?)");
			
			for (String s : linkedHashMap.keySet()) { 
					ps.setString(1, s);
					ps.setInt(2, linkedHashMap.get(s));
					ps.setString(3, currentTime);
			}
			
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
	 * 
	 * @param linkedHashMap
	 * @param connectionURL
	 * @param user
	 * @param password
	 */
	public static void storeRecipientDomainsToDatabase(LinkedHashMap<String, Integer> linkedHashMap, String connectionURL, String user, String password){
		Connection conn = null;
		String currentTime = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://" + connectionURL, user, password);
			PreparedStatement ps = conn.prepareStatement("INSERT INTO daily_top_failed_recipient_domains (domain, num_delivery_error, stat_date)" +
					"VALUES(?,?,?)");
			
			for (String s : linkedHashMap.keySet()) { 
					ps.setString(1, s);
					ps.setInt(2, linkedHashMap.get(s));
					ps.setString(3, currentTime);
			}
			
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

		if(folder.isFile()){
			parseErrorMail(folder.getAbsolutePath());

		} else {
			
			File[] listOfFiles = folder.listFiles();

			for (File file : listOfFiles) {
				if (file.isFile()) {	
					parseErrorMail(file.getAbsolutePath());
				}
			}
		}

		Utils.printOutput(failedRecipientDomain, args[1]);
		Utils.printOutput(errorSenderDomainCount, args[2]);
		
		String connectionURL = args[3];
		String user = args[4];
		String password = args[5];
		
		storeSenderDomainsToDatabase(errorSenderDomainCount, connectionURL, user, password);
		storeRecipientDomainsToDatabase(failedRecipientDomain, connectionURL, user, password);
	}

}
