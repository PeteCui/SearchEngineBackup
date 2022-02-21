package assignment2.pagerank;

import java.util.*;

import javax.sound.sampled.SourceDataLine;
import javax.xml.crypto.Data;

import java.io.*;
import java.security.KeyStore.Entry;

public class PageRank {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a HashMap, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a HashMap whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

	String fileName = "pageRankDavis";
	String title_file_mapping = "davisTitles.txt";


	public class TitleRank implements Comparable<TitleRank>{
		String title;
		double rank;

		public TitleRank(String title, double rank){
			this.title = title;
			this.rank = rank;
		}

		public int compareTo(TitleRank other) {
			return Double.compare(other.rank, rank);
		}

		public String toString(){
			return this.title + ":" + this.rank;
		}

	} 

       
    /* --------------------------------------------- */


    public PageRank( String filename ) {
		//fetch file 
		int noOfDocs = readDocs( filename );
		//begain to surf
		iterate( noOfDocs, 1000 );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures. 
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
	int fileIndex = 0;
	try {
	    System.err.println( "Reading file... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
			int index = line.indexOf( ";" );
			String title = line.substring( 0, index );
			Integer fromdoc = docNumber.get( title );
			//  Have we seen this document before?
			if ( fromdoc == null ) {	
		    	// This is a previously unseen doc, so add it to the table.
		    	fromdoc = fileIndex++;
		    	docNumber.put( title, fromdoc );
				// System.out.println("title: " + title);
				// System.out.println("fromdoc: " + fromdoc);
		    	docName[fromdoc] = title;
			}
			// Check all outlinks.
			StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
			while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
		    	String otherTitle = tok.nextToken();
		    	Integer otherDoc = docNumber.get( otherTitle );
		    	if ( otherDoc == null ) {
					// This is a previousy unseen doc, so add it to the table.
					otherDoc = fileIndex++;
					docNumber.put( otherTitle, otherDoc );
					docName[otherDoc] = otherTitle;
		    	}
				// Set the probability to 0 for now, to indicate that there is
				// a link from fromdoc to otherDoc.
				if ( link.get(fromdoc) == null ) {
					link.put(fromdoc, new HashMap<Integer,Boolean>());
				}
				if ( link.get(fromdoc).get(otherDoc) == null ) {
					link.get(fromdoc).put( otherDoc, true );
					out[fromdoc]++;
				}
			}
	    }
		in.close();
	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
			System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
			System.err.print( "done. " );
	    }
	}
	catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
		System.err.println( "Read " + fileIndex + " number of documents" );
		return fileIndex;
    }


    /* --------------------------------------------- */
	// stupid method, too slow to caculate but easy to understand
    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    // void iterate( int numberOfDocs, int maxIterations ) {
		
	// 	double[] x = new double[numberOfDocs];
	// 	//initiate the transition matrix
	// 	Arrays.fill(x, 1.0/numberOfDocs);
	// 	//System.out.println(Arrays.toString(x));
	// 	int iteration = 0;
	// 	//jump
	// 	double jumpP = BORED/numberOfDocs;

	// 	while(iteration < maxIterations){
	// 		System.out.println("iteration: "+ iteration);
	// 		double[] x_new = new double[numberOfDocs];
	// 		double sum;
	// 		for(int i = 0; i < numberOfDocs; i++){
	// 			double temp = x[i];
	// 			for(int j = 0; j < numberOfDocs; j++){
	// 				//sink page
	// 				if(link.get(j) == null){
	// 					x_new[i] = x_new[i] + temp * 1/numberOfDocs;
	// 				}
	// 				//not a sink page
	// 				else{
	// 					//link to page i
	// 					if(link.get(j).get(i) != null){
	// 						//link to page i
	// 						x_new[i] = x_new[i] + temp * [(1-BORED)/out[j] + jumpP];
	// 					}else{
	// 						//not link to page i
	// 						x_new[i] = x_new[i] + temp * jumpP;
	// 					}
	// 				}
	// 			}
	// 			sum += x_new[i];
	// 		}
	// 		//normalization
	// 		for(int i = 0; i < numberOfDocs; i++){
	// 			x_new[i] = x_new[i]/sum;
	// 		}

	// 		double delta = 0.0;
	// 		for(int i = 0; i < numberOfDocs; i++){
	// 			delta += Math.abs(x[i] - x_new[i]);
	// 			System.out.println(delta);
	// 		}


	// 		if(delta <= EPSILON){
	// 			break;
	// 		}
	// 		System.arraycopy(x_new, 0, x, 0, x_new.length);
	// 		iteration++;
	// 	}

	// 	System.out.println("value: " + x.toString());

	void iterate( int numberOfDocs, int maxIterations ){
		
		//caculate how many pages point to each page for reducing loop time
		HashMap<Integer,HashMap<Integer,Boolean>> pagesToMe = new HashMap<Integer,HashMap<Integer,Boolean>>();
		ArrayList<Integer> sinkPages = new ArrayList<>();
		for(int i = 0; i < numberOfDocs; i++){
			int from = i;
			if(link.get(i) != null){
				//pages existing outlinks and which pages point to them
				for(Map.Entry<Integer, Boolean> entry : link.get(i).entrySet()) {
					int to = entry.getKey();
					if(pagesToMe.get(to) == null){
						pagesToMe.put(to, new HashMap<Integer,Boolean>());
					}
					pagesToMe.get(to).put(from, true);
				}
			}else{
				//sink pages
				sinkPages.add(from);
			}
        }
		System.out.println(pagesToMe.size());
		System.out.println(sinkPages.size());

		//System.out.println(pagesToMe.size());
		double[] x = new double[numberOfDocs];
		double[] x_new = new double[numberOfDocs];
		//initiate the transition matrix
		//Arrays.fill(x, 1.0/numberOfDocs);
		x[0] = 1.0;
		int iteration = 0;
		while(iteration < maxIterations){
			if(iteration != 0){
				System.arraycopy(x_new, 0, x, 0, x_new.length);
			}
			//the sum probability of surf to page i from the sink page
			double sinkPagesToMeProbability = 0.0;
			for(int from : sinkPages){
				sinkPagesToMeProbability += (double)x[from] * (1 - BORED) / (double)numberOfDocs;
			}
			double sum = 0.0;
			for(int i = 0; i < numberOfDocs; i++){
				//the probability of jumping to page i from every page
				x_new[i] = BORED / (double)numberOfDocs;
				x_new[i] += sinkPagesToMeProbability;
				HashMap<Integer,Boolean> toMeLinks = pagesToMe.get(i);
				if(toMeLinks != null){
					for(int from : toMeLinks.keySet()){
						//the probability of surfing to page i from the page linking to page i 
						x_new[i] += x[from] * (1 - BORED ) / out[from];
					}
				}
				sum += x_new[i];
			}

			// //normalization
			for(int i = 0; i < numberOfDocs; i++){
				x_new[i] = x_new[i]/sum;
			}

			double delta = 0.0;
			for(int i = 0; i < numberOfDocs; i++){
				sum += x_new[i];
				delta += Math.abs(x[i] - x_new[i]);
			}

			// System.out.println(iteration);
			// System.out.println(delta);
			if(delta <= EPSILON){
				break;
			}
			iteration++;
		}
		printTopN(30, x_new, numberOfDocs);
	}

	public void printTopN(int N, double[] pageRank, int numberOfDocs){
		//create rankList for compare
		ArrayList<TitleRank> rankList = new ArrayList<>();
		for(int i = 0; i < numberOfDocs; i++){
			rankList.add(new TitleRank(docName[i], pageRank[i]));
		}
		//sort
		Collections.sort(rankList);
		//write to disk
		writePageRank(rankList);
		//print the top N
		for(int i = 0; i <=N-1 ;i++){
			System.out.println(rankList.get(i).title + " : " + rankList.get(i).rank);
		}
	}

	public void writePageRank(ArrayList<TitleRank> rankList){
		try {
			//create title - file name mapping
			BufferedReader in = new BufferedReader( new FileReader( title_file_mapping ));
			String line;
			HashMap<String, String> titleFileMapping = new HashMap<String, String>();
			while ((line = in.readLine()) != null) {
				String[] elements = line.split(";");
				titleFileMapping.put(elements[0], elements[1]);
			}
			//write filename - rank mapping file
			RandomAccessFile file = new RandomAccessFile(fileName, "rw");
			for(int i = 0; i < rankList.size(); i++){
				String res = titleFileMapping.get(rankList.get(i).title) + ":" + rankList.get(i).rank;
				byte[] data = res.getBytes();
				
				file.write(data);
				file.write("\n".getBytes());
			}
			file.close();
		} catch ( Exception e) {
			System.out.println("write page rank wrong!");
		}
	}

    // }


    /* --------------------------------------------- */


    public static void main( String[] args ) {
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    new PageRank( args[0] );
	}
    }
}