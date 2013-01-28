package com.getabine.logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class JamesErrorMailParser {
	
	public static LinkedHashMap<String, Integer> domainCount = new LinkedHashMap<String, Integer>();
	public static LinkedHashMap<String, Integer> opayqReceivedMailCount = new LinkedHashMap<String, Integer>();

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
							tld = parseTLD(line.substring(indexOfAtSign+1, line.length()));
							numCounts = domainCount.get(tld);

							if(numCounts != null){
								//System.out.println("found a key");
								domainCount.put(tld, numCounts+1);
							} else {
								domainCount.put(tld, new Integer(1));
							}
							
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
	 * look for TLDs -> let's do it with an easy non-overkill matching -> take the string from stuff before first @ to space before "on"
	 * then for TLD, split on '.' if greater than 2 strings, then take the last 2 strings as TLD unless either are < 3 characters each.  
	 * keep going until you have all strings or the total tld is at least 7 characters long (including '.'s)
	 * @param domain
	 * @return
	 */
	private static String parseTLD(String domain){
		
		String tld = null;
		String[] parts = domain.trim().split("\\.");
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
		
		return tld;
	}
	
	public static void printOutput(String outputPath){
		PrintWriter out;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(outputPath, false)));
			
			HashSet<Integer> values = new HashSet<Integer>();
			values.addAll(domainCount.values());
			ArrayList<Integer> arrayValues = new ArrayList<Integer>();
			
			Collections.addAll(arrayValues, values.toArray(new Integer[values.size()]));
			Collections.sort(arrayValues, Collections.reverseOrder());
	
			for (Integer i : arrayValues) { 
				/*if (last_i == i) // without duplicates
					continue;
				last_i = i;
				 */
				for (String s : domainCount.keySet()) { 
					if (domainCount.get(s) == i)   
						out.println(s + ": " + i);
				}
			}
		
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		
		printOutput(args[1]);

	}

}
