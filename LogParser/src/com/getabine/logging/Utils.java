package com.getabine.logging;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class Utils {


	/**
	 * look for TLDs -> let's do it with an easy non-overkill matching -> take the string from stuff before first @ to space before "on"
	 * then for TLD, split on '.' if greater than 2 strings, then take the last 2 strings as TLD unless either are < 3 characters each.  
	 * keep going until you have all strings or the total tld is at least 7 characters long (including '.'s)
	 * @param domain
	 * @return
	 */
	public static String parseTLD(String domain){
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
		return tld.trim().toLowerCase();
	}

	public static String parseEmailUser(String emailAddress){
		//String email = null;
		return emailAddress.substring(0, emailAddress.indexOf("@"));
	}

	public static void printOutput(LinkedHashMap<String, Integer> linkedHashMap, String outputPath){
		System.out.println("Utils.printOutput to: " + outputPath);
		PrintWriter out;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(outputPath, true)));

			HashSet<Integer> values = new HashSet<Integer>();
			values.addAll(linkedHashMap.values());
			ArrayList<Integer> arrayValues = new ArrayList<Integer>();

			Collections.addAll(arrayValues, values.toArray(new Integer[values.size()]));
			Collections.sort(arrayValues, Collections.reverseOrder());

			for (Integer i : arrayValues) { 
				/*if (last_i == i) // without duplicates
					continue;
				last_i = i;
				 */
				for (String s : linkedHashMap.keySet()) { 
					if (linkedHashMap.get(s) == i)   
						out.println(s + ": " + i);
				}
			}

			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

	
}
