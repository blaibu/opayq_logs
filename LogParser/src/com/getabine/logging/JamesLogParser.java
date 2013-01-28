package com.getabine.logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class JamesLogParser { // could do some fancy factory stuff here later... for now, just a bunch of static methods

	public static final int MAX_RESULTS = 10;
	public static final String DEFAULT_OUTPUT_PATH = "/Users/blai/Documents/james_stats.txt";

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
		LinkedHashMap<String, Integer> domainCount = new LinkedHashMap<String, Integer>();
		LinkedHashMap<String, Integer> opayqReceivedMailCount = new LinkedHashMap<String, Integer>();
		BufferedReader br;
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputPath, false)));
			br = new BufferedReader(new FileReader(filePath));
			out.println("SMTP LOGS " + filePath + " PARSED @ " + new Timestamp(new Date().getTime()));

			String line, tld, opayqUser;
			//int lineNum = 0;
			while ((line = br.readLine()) != null) {
				//lineNum++;
				//System.out.println("lineNum: " + lineNum + line);
				//01/11/12 21:34:07 INFO  smtpserver: Successfully spooled mail Mail1351805647702-91924 from info@gold-halloween-black.com on 209.17.191.89 for [fadcc6e5@opayq.com]
				int indexOfAtSign = line.indexOf('@');
				int indexOfBracket = line.indexOf("[");
				if(indexOfAtSign > 0 && indexOfBracket > 0) { // && line.indexOf("opayq") > 0
					int indexOfOn = line.indexOf(" on ");
					if(indexOfOn > 0) { // there can be @ and [ for rejected mails too...
						Integer numCounts;
						if(indexOfAtSign+1 < indexOfOn){
							tld = parseTLD(line.substring(indexOfAtSign+1, indexOfOn));
							if(tld.equals("opayq.com")) continue;
							//System.out.println("TLD: " + tld);
							numCounts = domainCount.get(tld);

							if(numCounts != null){
								//System.out.println("found a key");
								domainCount.put(tld, numCounts+1);
							} else {
								domainCount.put(tld, new Integer(1));
							}
						}

						opayqUser = parseEmailUser(line.substring(indexOfBracket+1, line.indexOf("]")+1));

						numCounts = opayqReceivedMailCount.get(opayqUser);
						if(numCounts != null){
							opayqReceivedMailCount.put(opayqUser, numCounts+1);
						} else {
							//System.out.println("new email: " + opayqUser);
							opayqReceivedMailCount.put(opayqUser, new Integer(1));
						}
					}
				}
			} // close while

			out.println("Top 10 opayq email recipients:");
			printTopN(opayqReceivedMailCount, MAX_RESULTS, out);
			out.println("Top 10 email originating domains:");
			printTopN(domainCount, MAX_RESULTS, out);

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
	 * look for TLDs -> let's do it with an easy non-overkill matching -> take the string from stuff before first @ to space before "on"
	 * then for TLD, split on '.' if greater than 2 strings, then take the last 2 strings as TLD unless either are < 3 characters each.  
	 * keep going until you have all strings or the total tld is at least 7 characters long (including '.'s)
	 * @param domain
	 * @return
	 */
	private static String parseTLD(String domain){
		String tld = null;
		String[] parts = domain.split("\\.");
		//System.out.println(domain);

		if(parts.length > 0){
			try{
				if(parts.length == 1){
					tld = parts[0];
				} else {
					tld = parts[parts.length-2] + '.' + parts[parts.length-1];
					int i = parts.length-3;
					while(tld.length() < 7 && i >-1) {// 3 + 1 + 3
						tld = parts[i] + '.' + tld;
						//System.out.println("TLD: " + tld);
					}
				}
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return tld.trim();
	}

	private static String parseEmailUser(String emailAddress){
		//String email = null;
		return emailAddress.substring(0, emailAddress.indexOf("@"));
	}

	private static void printTopN(LinkedHashMap<String, Integer> mapToSort, int max, PrintWriter out) throws IOException{
		int resultsToReturn = max;
		ArrayList<Integer> values = new ArrayList<Integer>();
		values.addAll(mapToSort.values());

		Collections.sort(values, Collections.reverseOrder());

		//int last_i = -1;
		if(values.size() < resultsToReturn){
			resultsToReturn = values.size();
		}

		for (Integer i : values.subList(0, resultsToReturn)) { 
			/*if (last_i == i) // without duplicates
				continue;
			last_i = i;
			 */
			for (String s : mapToSort.keySet()) { 
				if (mapToSort.get(s) == i)   
					out.println(s + ": " + i);
			}
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

			HashMap<String, Integer> stats = new HashMap<String, Integer>();
			CharSequence successSequence = "sent successfully";

			stats.put("successful", new Integer(0));
			stats.put("retry", new Integer(0));
			stats.put("error", new Integer(0));

			while ((line = br.readLine()) != null) {
				if(line.contains(successSequence)){
					stats.put("successful", stats.get("successful")+1);
				} else if(line.indexOf("retries") > 0 && line.indexOf("after") > 0){
					stats.put("retry", stats.get("retry") + Integer.parseInt(line.substring(line.indexOf("after")+6, line.indexOf("retries")-1)));
				} else if (line.indexOf("Storing") > 0 && line.indexOf("error") > 0){
					stats.put("error", stats.get("error") +1); // stored a mail in error folder (unsuccessful)
				}
			}
			Integer numSuccessful = stats.get("successful");
			Integer numRetries = stats.get("retry");
			Integer numErrors = stats.get("error");

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

	public static void main(String args[]){
		String smtpserverLogFilePath = args[0];
		String mailetLogFilePath = args[1];
		String outputPath = args[2];
		long startTime = new Date().getTime();
		//JamesLogParser.parseSMTP("/Users/blai/Documents/smtpserver-2012-11-04-00-00.log", args[2]);
		//JamesLogParser.parseMailet("/Users/blai/Documents/mailet-2012-11-03-00-00.log", args[2]);
		JamesLogParser.parseSMTP(smtpserverLogFilePath, outputPath);
		JamesLogParser.parseMailet(mailetLogFilePath, outputPath);
		System.out.println("Done in: " + (new Date().getTime() - startTime) + "ms");
	}
}
