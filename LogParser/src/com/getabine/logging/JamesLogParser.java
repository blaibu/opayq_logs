package com.getabine.logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class JamesLogParser { // could do some fancy factory stuff here later... for now, just a bunch of static methods

	public static final int MAX_RESULTS = 10;
	public static final String DEFAULT_OUTPUT_PATH = "/Users/blai/Documents/james_stats.txt";
	/**
	 * mailetDomainCount is a hash between domain -> # of times that domain was the sender of a mail that was marked as sent successfully
	 */
	public static LinkedHashMap<String, Integer> mailetDomainCount = new LinkedHashMap<String, Integer>();

	public static HashMap<String, Integer> mailetStats = new HashMap<String, Integer>();
	/**
	 * smtpDomainCount is a hash between domain -> # of times that domain was the sender of a mail that was received by the mail server
	 */
	public static LinkedHashMap<String, Integer> smtpDomainCount = new LinkedHashMap<String, Integer>();
	/**
	 * opayqReceivedMailCount is a hash between opayq user -> # of times that user received a mail 
	 */
	public static LinkedHashMap<String, Integer> opayqReceivedMailCount = new LinkedHashMap<String, Integer>();

	/**
	 * Parse the smtp log file.  The JAMES smtp log file contains information on when mail
	 * is spooled, including the "from" address of the mail and the virtual user (opayq account).
	 * 
	 * Maybe not the most memory efficient....
	 * Output a file that tells us the 
	 * 	-top 10 users who had mail spooled successfully (opayq accounts) (=> mails received)
	 * 	-top 10 domains that mail was received from (=> the top 10 domains where email originated from to receivers)
	 * 
	 * @param filePath
	 * @param optionalOutputPath
	 */
	public static void parseSMTP(String filePath, String optionalOutputPath){
		String outputPath = optionalOutputPath;

		if(optionalOutputPath == null){
			outputPath = DEFAULT_OUTPUT_PATH;
		}

		/*TreeBidiMap domainCount = new TreeBidiMap(); // String, Integer
		TreeBidiMap opayqReceivedMailCount = new TreeBidiMap();*/

		BufferedReader br;
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputPath, false)));
			br = new BufferedReader(new FileReader(filePath));
			out.println("SMTP LOGS " + filePath + " PARSED @ " + new Timestamp(new Date().getTime()));

			String line, tld, opayqUser;
			//int lineNum = 0;
			while ((line = br.readLine()) != null) {
				//01/11/12 21:34:07 INFO  smtpserver: Successfully spooled mail Mail1351805647702-91924 from info@gold-halloween-black.com on 209.17.191.89 for [fadcc6e5@opayq.com]
				int indexOfAtSign = line.indexOf('@');
				int indexOfBracket = line.indexOf("[");
				if(indexOfAtSign > 0 && indexOfBracket > 0) { // && line.indexOf("opayq") > 0
					int indexOfOn = line.indexOf(" on ");
					if(indexOfOn > 0) { // there can be @ and [ for rejected mails too...
						Integer numCounts;
						if(indexOfAtSign+1 < indexOfOn){
							tld = Utils.parseTLD(line.substring(indexOfAtSign+1, indexOfOn));
							if(tld.equals("opayq.com")) continue;
							//System.out.println("TLD: " + tld);
							numCounts = smtpDomainCount.get(tld);

							if(numCounts != null){
								smtpDomainCount.put(tld, numCounts+1);
							} else {
								smtpDomainCount.put(tld, new Integer(1));
							}
						}

						opayqUser = Utils.parseEmailUser(line.substring(indexOfBracket+1, line.indexOf("]")+1));

						numCounts = opayqReceivedMailCount.get(opayqUser);
						if(numCounts != null){
							opayqReceivedMailCount.put(opayqUser, numCounts+1);
						} else {
							opayqReceivedMailCount.put(opayqUser, new Integer(1));
						}
					}
				}
			} // close while

			br.close();
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	/**
	 * Parse the mailet logs, specifically looking for log lines from the RemoteDelivery mailet which handles email delivery.
	 * @param filePath
	 * @param optionalOutputPath
	 */

	public static void parseMailet(String filePath, String optionalOutputPath){
		String outputPath = optionalOutputPath;

		if(optionalOutputPath == null){
			outputPath = DEFAULT_OUTPUT_PATH;
		}

		BufferedReader br;
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputPath, true))); // true here since I run this after running SMTP; can definitely do this better but this was the quick fix
			br = new BufferedReader(new FileReader(filePath));
			out.println("MAILET LOG " + filePath + " PARSED @ " + new Timestamp(new Date().getTime()));

			String line;

			mailetStats.put("successful", new Integer(0));
			mailetStats.put("retry", new Integer(0));
			mailetStats.put("error", new Integer(0));

			while ((line = br.readLine()) != null) {
				int indexOfSuccessfullyTo = line.indexOf("successfully to ");
				if(indexOfSuccessfullyTo > 0){
					mailetStats.put("successful", mailetStats.get("successful")+1);
					//					System.out.println("line is: " + line);
					//					System.out.println("attempt to parse this tld: "+ line.substring(indexOfSuccessfullyTo + 16, line.indexOf(" at ")));

					String tld = Utils.parseTLD(line.substring(indexOfSuccessfullyTo + 16, line.indexOf(" at ")));
					Integer numCounts = mailetDomainCount.get(tld);
					if(numCounts == null){
						mailetDomainCount.put(tld, new Integer(1));
					} else {
						mailetDomainCount.put(tld, numCounts + 1);
					}

				} else if(line.indexOf("retries") > 0 && line.indexOf("after") > 0){
					mailetStats.put("retry", mailetStats.get("retry") + Integer.parseInt(line.substring(line.indexOf("after")+6, line.indexOf("retries")-1)));
				} else if (line.indexOf("Storing") > 0 && line.indexOf("error") > 0){
					mailetStats.put("error", mailetStats.get("error") +1); // stored a mail in error folder (unsuccessful)
				}
			}
			Integer numSuccessful = mailetStats.get("successful");
			Integer numRetries = mailetStats.get("retry");
			Integer numErrors = mailetStats.get("error");

			DecimalFormat df = new DecimalFormat();
			df.setMinimumFractionDigits(2);
			df.setMaximumFractionDigits(2);

			out.println("Number of successfully sent mail: " + numSuccessful);
			out.println("Number of retries: " + numRetries);
			out.println("Number of mails stored in error: " + numErrors);
			out.println("Number of mails successful/(successful + error): " + df.format((float)numSuccessful / (numSuccessful + numErrors)));
			//System.out.println("numsuccess: " + stats.get("successful"));
			//System.out.println("retries: " + stats.get("retry"));
			//System.out.println("errors: " + stats.get("error"));

			br.close();
			out.close();
		} catch (Exception ex){
			ex.printStackTrace();
		} 
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]){

		String smtpserverLogFilePath = args[0]; 
		String mailetLogFilePath = args[1];
		String outputPath = args[2];
		String successfullysentdomains = args[3];
		String spooledsenderdomains = args[4];
		String opayqspooledemailreceiver = args[5];

		long startTime = new Date().getTime();

		File folder = new File(smtpserverLogFilePath);
		if(folder.isFile()){
			JamesLogParser.parseSMTP(folder.getAbsolutePath(), outputPath);
		} else {
			File[] listOfFiles = folder.listFiles();

			for (File file : listOfFiles) {
				if (file.isFile()) {	
					JamesLogParser.parseSMTP(file.getAbsolutePath(), outputPath);
				}
			}
		}

		File mailetFolder = new File(mailetLogFilePath);

		if(folder.isFile()){
			JamesLogParser.parseMailet(folder.getAbsolutePath(), outputPath);
		} else {
			File[] mailetFiles = mailetFolder.listFiles();

			for (File file : mailetFiles) {
				if (file.isFile()) {	
					JamesLogParser.parseMailet(file.getAbsolutePath(), outputPath);
				}
			}
		}

		Utils.printOutput(mailetDomainCount, successfullysentdomains);
		Utils.printOutput(smtpDomainCount, spooledsenderdomains);
		Utils.printOutput(opayqReceivedMailCount, opayqspooledemailreceiver);
		System.out.println("Done in: " + (new Date().getTime() - startTime) + "ms");
	}
}
