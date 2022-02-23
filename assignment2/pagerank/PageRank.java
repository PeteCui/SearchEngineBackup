package assignment2.pagerank;

import java.util.*;

import java.io.*;

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
	 *   (from index order to id in the graph file)
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

	final static double STOP = 0.15;

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
		//begin to surf
//		iterate( noOfDocs, 1000 );
//		//MC_1
//		MC_simulation_12(filename, noOfDocs,1,1,true);
//		MC_simulation_12(filename,noOfDocs,2,1,true);
//		MC_simulation_12(filename,noOfDocs,3,1,true);
////		//MC_2
//		MC_simulation_12(filename,noOfDocs,1,2,true);
//		MC_simulation_12(filename,noOfDocs,2,2,true);
//		MC_simulation_12(filename,noOfDocs,3,2,true);
//		//MC_4
//		MC_simulation_45(filename,noOfDocs,1,4,true);
//		MC_simulation_45(filename,noOfDocs,2,4,true);
//		MC_simulation_45(filename,noOfDocs,3,4,true);
//		//MC_5
//		MC_simulation_45(filename,noOfDocs,1,5,true);
//		MC_simulation_45(filename,noOfDocs,2,5,true);
//		MC_simulation_45(filename,noOfDocs,3,5,true);

//		calculateDifference(noOfDocs);

//		MC_simulation_12(filename,noOfDocs,1,2,true);
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
			String docID = line.substring( 0, index );
			Integer fromdoc = docNumber.get( docID );
			//  Have we seen this document before?
			if ( fromdoc == null ) {	
		    	// This is a previously unseen doc, so add it to the table.
		    	fromdoc = fileIndex++;
		    	docNumber.put( docID, fromdoc );
				// System.out.println("title: " + title);
				// System.out.println("fromdoc: " + fromdoc);
		    	docName[fromdoc] = docID;
			}
			// Check all outlinks.
			StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
			while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
		    	String otherTitle = tok.nextToken();
		    	Integer otherDoc = docNumber.get( otherTitle );
		    	if ( otherDoc == null ) {
					// This is a previous unseen doc, so add it to the table.
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
		//System.out.println(fileIndex);
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

	void iterate( int numberOfDocs, int maxIterations ){
		
		//calculate how many pages point to each page for reducing loop time
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
				//the probability of jumping to page i from sink page
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
		printTopN(30, x_new, numberOfDocs, fileName);
	}

	public void printTopN(int N, double[] pageRank, int numberOfDocs, String fileName){
		//create rankList for compare
		ArrayList<TitleRank> rankList = new ArrayList<>();
		for(int i = 0; i < numberOfDocs; i++){
			rankList.add(new TitleRank(docName[i], pageRank[i]));
		}
		//sort
		Collections.sort(rankList);
		//write to disk
		writePageRankWithDocName(N, rankList, fileName);
		//writePageRankWithDocID(N, rankList, fileName);
		//print the top N
		for(int i = 0; i <=N-1 ;i++){
			System.out.println(rankList.get(i).title + " : " + rankList.get(i).rank);
		}
	}

	public void writePageRankWithDocName(int N, ArrayList<TitleRank> rankList, String fileName){
		try {
//			//create title(NodeID) - file name mapping
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
				//from the NodeID to fetch the file name from the hash map
				String res = titleFileMapping.get(rankList.get(i).title) + ":" + rankList.get(i).rank;
//				String res = docNumber.get(rankList.get(i).title) + ":" + rankList.get(i).rank;
				byte[] data = res.getBytes();
				
				file.write(data);
				file.write("\n".getBytes());
			}
			file.close();
		} catch ( Exception e) {
			System.out.println("write page rank wrong!");
		}
	}

	public void writePageRankWithDocID(int N, ArrayList<TitleRank> rankList, String fileName){
		try {
			RandomAccessFile file = new RandomAccessFile(fileName, "rw");
			for(int i = 0; i < N-1; i++){
				String res = rankList.get(i).title + ":" + rankList.get(i).rank;
				byte[] data = res.getBytes();

				file.write(data);
				file.write("\n".getBytes());
			}
			file.close();
		} catch ( Exception e) {
			System.out.println("write page rank wrong!");
		}
	}

	// simulate N runs of the random walk initiated at a randomly chosen page
	public double[] MC_simulation_12(String filename, int noOfDocs, int time, int mode, boolean write){
		int maximum = noOfDocs;
		int N = noOfDocs * time;
		double[] pageRank = new double[noOfDocs];

		//loop
		for (int i = 0; i < N; i++){
			//select a random page to start
			int curPage = 0;
			if (mode == 1){
				curPage = generateRandomNumber(noOfDocs);
			}else if (mode == 2){
				curPage = i % noOfDocs;
			}
			int count = 0;
			while(generateProbability() > STOP || count > maximum){
				if (link.get(curPage) == null){
					//sink page
					curPage = generateRandomNumber(noOfDocs);
				}else{
					//non sink page
					//get nextPage index
					int index = generateRandomNumber(link.get(curPage).size());
					int j = 0;
					for (int pageID : link.get(curPage).keySet()){
						//the corresponding page
						if (j == index){
							curPage = pageID;
							//not break the run, just means find the next linked page
							break;
						}
						j++;
					}
				}
				count++;
			}
			//stop at here
			pageRank[curPage] += 1.0;
		}

		for (int i = 0; i < noOfDocs; i++){
			pageRank[i] /= N;
		}
		if (write){
			String[] prefix = filename.split("\\.");
			printTopN(30,pageRank,noOfDocs, "./2.7data/" + prefix[0] + "MC" + mode + "N" + time);
		}
		return pageRank;
	}

	public double[] MC_simulation_45(String filename, int noOfDocs, int time, int mode, boolean write){
		int maximum = noOfDocs;
		int N = noOfDocs * time;
		double[] pageRank = new double[noOfDocs];
		int totalNum = 0;

		//loop
		for (int i = 0; i < N; i++){
			//select a random page to start
			int curPage = 0;
			if (mode == 5){
				curPage = generateRandomNumber(noOfDocs);
			}else if (mode == 4){
				curPage = i % noOfDocs;
			}
			//the maximum jump
			int count = 0;
			while(generateProbability() > STOP || count > maximum){
				if (link.get(curPage) == null){
					//sink page
					curPage = generateRandomNumber(noOfDocs);
					//update visit
					pageRank[curPage] += 1.0;
					totalNum++;
					//stop and break this run
					break;
				}else{
					//non sink page
					//get nextPage index
					int index = generateRandomNumber(link.get(curPage).size());
					int j = 0;
					for (int pageID : link.get(curPage).keySet()){
						//the corresponding page
						if (j == index){
							curPage = pageID;
							//update visit
							pageRank[curPage] += 1.0;
							totalNum++;
							//not break the run, just means find the next linked page
							break;
						}
						j++;
					}
				}
				count++;
			}
		}

		for (int i = 0; i < noOfDocs; i++){
			pageRank[i] /= totalNum;
		}

		if (write){
			String[] prefix = filename.split("\\.");
			printTopN(30,pageRank,noOfDocs, "./2.7data/" + prefix[0] + "MC" + mode + "N" + time);
		}

		return pageRank;

	}

	public int generateRandomNumber(int size){
		Random rand = new Random();
		return rand.nextInt(size);

	}

	public double generateProbability(){
		Random rand = new Random();
		return rand.nextFloat();
	}

	public void calculateDifference(int noOfDocs) {
		HashMap<String, Double> top30Map = new HashMap<String, Double>();
		try {
			//read top 30
			BufferedReader in = new BufferedReader( new FileReader( "davis_top_30.txt" ));
			String line;
			while ((line = in.readLine()) != null) {
				String[] elements = line.split(":");
				top30Map.put(elements[0], Double.valueOf(elements[1]));
			}
		} catch ( Exception e) {
			e.printStackTrace();
			System.out.println("calculate difference wrong!");
		}

		for (int time = 1; time < 6; time++) {
			double[] res1 = MC_simulation_12(null, noOfDocs, time, 1, false);
			double[] res2 = MC_simulation_12(null, noOfDocs, time, 2, false);
			double[] res3 = MC_simulation_45(null, noOfDocs, time, 4, false);
			double[] res4 = MC_simulation_45(null, noOfDocs, time, 5, false);

//			getSquaredDiff(top30Map,res1);
//			getSquaredDiff(top30Map,res2);
//			getSquaredDiff(top30Map,res3);
//			getSquaredDiff(top30Map,res4);
//			System.out.println( "MC1 " + time + ":" + getSquaredDiff(top30Map,res1));
//			System.out.println( "MC2 " + time + ":" + getSquaredDiff(top30Map,res2));
//			System.out.println( "MC4 " + time + ":" + getSquaredDiff(top30Map,res3));
//			System.out.println( "MC5 " + time + ":" + getSquaredDiff(top30Map,res4));

			try {
				RandomAccessFile file = new RandomAccessFile("./2.7data/diffData", "rw");
				file.seek(file.length());
				String out1 = "MC1 " + time + ":" + String.format("%.9g%n", getSquaredDiff(top30Map, res1));
				byte[] data1 = out1.getBytes();
				file.write(data1);
				file.write("\n".getBytes());
				String out2 = "MC2 " + time + ":" + String.format("%.9g%n", getSquaredDiff(top30Map, res2));
				byte[] data2 = out2.getBytes();
				file.write(data2);
				file.write("\n".getBytes());
				String out3 = "MC3 " + time + ":" + String.format("%.9g%n", getSquaredDiff(top30Map, res3));
				byte[] data3 = out3.getBytes();
				file.write(data3);
				file.write("\n".getBytes());
				String out4 = "MC4 " + time + ":" + String.format("%.9g%n", getSquaredDiff(top30Map, res4));
				byte[] data4 = out4.getBytes();
				file.write(data4);
				file.write("\n".getBytes());
				file.write("\n".getBytes());

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public double getSquaredDiff(HashMap<String, Double> map1 , double[] res){
		//mapping
		HashMap<String, Double> map2 = new HashMap<>();
		for(int i = 0; i < res.length; i++){
			map2.put(docName[i], res[i]);
		}
		//calculate
		double sum = 0.0;
		for (Map.Entry<String, Double> entry : map1.entrySet()){
//			System.out.println(entry.getKey() + " : " + map2.get(entry.getKey()));
//			System.out.println(entry.getKey() + " : " + entry.getValue());
//			System.out.println("\n");
			sum += Math.pow(map2.get(entry.getKey()) - entry.getValue() ,2);
		}
		return sum;
	}

	/* --------------------------------------------- */
	// stupid method, too slow to calculate but easy to understand
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