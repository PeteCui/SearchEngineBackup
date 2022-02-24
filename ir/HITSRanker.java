/**
 *   Computes the Hubs and Authorities for an every document in a query-specific
 *   link graph, induced by the base set of pages.
 *
 *   @author Dmytro Kalpakchi
 */

package ir;

import java.util.*;
import java.io.*;


public class HITSRanker {

    /**
     *   Max number of iterations for HITS
     */
    final static int MAX_NUMBER_OF_STEPS = 1000;

    /**
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;


    /**
     *   Convergence criterion: hub and authority scores do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.001;

    /**
     *   The inverted index
     */
    Index index;

    /**
     *   Mapping from the fileName to internal document ids used in the links file
     */
//    // fileName/title -> nodeID in links file
    HashMap<String, Integer> titleToId = new HashMap<String,Integer>();
    //  nodeID in links file -> fileName/title
    HashMap<Integer, String> IdToTiltle = new HashMap<Integer, String>();
    /**
     *   Sparse vector containing hub scores
     *   the key is nodeId
     */
    HashMap<Integer,Double> hubs;
    double hubW = 0.5;
    /**
     *   Sparse vector containing authority scores
     *   the key is nodeId
     */
    HashMap<Integer,Double> authorities;
    double authW = 1 - hubW;

    //I link to which pages ------ A
    //key is read order in links file
    HashMap<Integer,HashMap<Integer,Boolean>> linkOther = new HashMap<Integer,HashMap<Integer,Boolean>>();
    //Which pages link to me ------ A inverse
    HashMap<Integer,HashMap<Integer,Boolean>> otherlink = new HashMap<Integer,HashMap<Integer,Boolean>>();

    //The number of outlinks from each page(index order).
    int[] outNumber = new int[MAX_NUMBER_OF_DOCS];
    //The number of inlinks from each (index order).
    int[] inNumber = new int[MAX_NUMBER_OF_DOCS];

    //nodeID in links file -> read order
    HashMap<Integer,Integer> idToOrder = new HashMap<Integer,Integer>();
    //read order -> nodeID in links file
    String[] orderToId = new String[MAX_NUMBER_OF_DOCS];

    HashSet<Integer> baseSet;

    //convert file name to path
    String docPath = "dataset\\davisWiki\\";
    
    /* --------------------------------------------- */

    /**
     * Constructs the HITSRanker object
     * 
     * A set of linked documents can be presented as a graph.
     * Each page is a node in graph with a distinct nodeID associated with it.
     * There is an edge between two nodes if there is a link between two pages.
     * 
     * Each line in the links file has the following format:
     *  nodeID;outNodeID1,outNodeID2,...,outNodeIDK
     * This means that there are edges between nodeID and outNodeIDi, where i is between 1 and K.
     * 
     * Each line in the titles file has the following format:
     *  nodeID;pageTitle
     *  
     * NOTE: nodeIDs are consistent between these two files, but they are NOT the same
     *       as docIDs used by search engine's Indexer
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     * @param      index           The inverted index
     */
    public HITSRanker( String linksFilename, String titlesFilename, Index index ) {
        this.index = index;
        readDocs( linksFilename, titlesFilename );
    }


    /* --------------------------------------------- */

    /**
     * A utility function that gets a file name given its path.
     * For example, given the path "davisWiki/hello.f",
     * the function will return "hello.f".
     *
     * @param      path  The file path
     *
     * @return     The file name.
     */
    public String getFileName( String path ) {
        String result = "";
        StringTokenizer tok = new StringTokenizer( path, "\\/" );
        while ( tok.hasMoreTokens() ) {
            result = tok.nextToken();
        }
        return result;
    }


    /**
     * Reads the files describing the graph of the given set of pages.
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     */
    void readDocs( String linksFilename, String titlesFilename ) {
        //handle the linksFile
        int fileIndex = 0;
        try{
            System.out.println( "Reading file... " );
            BufferedReader in = new BufferedReader( new FileReader( linksFilename ));
            String line;
            while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS ) {
                int index = line.indexOf(";");
                String ID = line.substring(0,index);
                Integer fromdoc = idToOrder.get(Integer.valueOf(ID));
                // Have we seen this document before?
                if (fromdoc == null){
                    // This is a previously unseen doc, so add it to the table.
                    fromdoc = fileIndex++;
                    idToOrder.put( Integer.valueOf(ID), fromdoc );
                    orderToId[fromdoc] = ID;
                }
                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
                while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
                    String otherTitle = tok.nextToken();
                    Integer otherDoc = idToOrder.get( Integer.valueOf(otherTitle));
                    if ( otherDoc == null ) {
                        // This is a previousy unseen doc, so add it to the table.
                        otherDoc = fileIndex++;
                        idToOrder.put( Integer.valueOf(otherTitle), otherDoc );
                        orderToId[otherDoc] = otherTitle;
                    }
                    //update outlink hash map
                    if ( linkOther.get(fromdoc) == null ) {
                        linkOther.put(fromdoc, new HashMap<Integer,Boolean>());
                    }
                    if ( linkOther.get(fromdoc).get(otherDoc) == null ) {
                        linkOther.get(fromdoc).put( otherDoc, true );
                        outNumber[fromdoc]++;
                    }
                    //update inlink hashmap
                    if ( otherlink.get(otherDoc) == null ) {
                        otherlink.put(otherDoc, new HashMap<Integer,Boolean>());
                    }
                    if ( otherlink.get(otherDoc).get(fromdoc) == null ) {
                        otherlink.get(otherDoc).put( fromdoc, true );
                        inNumber[otherDoc]++;
                    }
                }
            }
            //System.out.println(fileIndex);
            in.close();
            if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
                System.err.print( "stopped reading since documents table is full. " );
            }

        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        //handle the titles file
        try{
            //create NodeID - file name mapping
            BufferedReader in = new BufferedReader( new FileReader( titlesFilename ));
            String line;
            while ((line = in.readLine()) != null) {
                String[] elements = line.split(";");
                IdToTiltle.put(Integer.valueOf(elements[0]), elements[1]);
                titleToId.put(elements[1], Integer.valueOf(elements[0]));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Perform HITS iterations until convergence
     *
     * @param      titles  The titles of the documents in the root set
     */
    private void iterate(String[] titles) {

        //HashSet<Integer> rootSet = new HashSet<>();
        baseSet = new HashSet<>();

        //find the baseSet and update rootset
        for (String title : titles){
            //add myself into the two sets
            //rootSet.add(idToOrder.get(titleToId.get(title)));

            //if this title exist in the database
            if (titleToId.get(title) != null){
                //title
//                System.out.println(title);
                //ID in links file
//                System.out.println(titleToId.get(title));
                //Read Order
//                System.out.println(idToOrder.get(titleToId.get(title)));
                baseSet.add(idToOrder.get(titleToId.get(title)));

                //find which page links to this page
                HashMap<Integer, Boolean> map1 = otherlink.get(idToOrder.get(titleToId.get(title)));
                if (map1 != null ){
                    for (Map.Entry<Integer, Boolean> pagesToMe : map1.entrySet()){
                        baseSet.add(pagesToMe.getKey());
                    }
                }
                //find which page this page of title links to
                HashMap<Integer, Boolean> map2 = linkOther.get(idToOrder.get(titleToId.get(title)));
                if (map2 != null ){
                    for (Map.Entry<Integer, Boolean> meToPages : map2.entrySet()){
                        baseSet.add(meToPages.getKey());
                    }
                }
            }
        }
        //finish find base set(Key is read order number and Value is true)
        //17478
//        System.out.println(idToOrder.size());
        //11412
//        System.out.println(otherlink.size());
        //10052
//        System.out.println(linkOther.size());
        //17478
//        System.out.println(baseSet.size());

        //initialization
        HashMap<Integer, Double> a = new HashMap<>();
        HashMap<Integer, Double> h = new HashMap<>();
        for (Integer i : baseSet){
            a.put(i, 1.0);
            h.put(i, 1.0);
        }

        HashMap<Integer, Double> a_new = new HashMap<Integer, Double>();
        HashMap<Integer, Double> h_new = new HashMap<Integer, Double>();


        int iteration = 0;
        while (iteration < MAX_NUMBER_OF_STEPS){
//            System.out.println("Iteration: " + iteration);
            //get new value
            a_new = Multiply(h, otherlink);
//            System.out.println(a_new.size());
            h_new = Multiply(a, linkOther);
//            System.out.println(h_new.size());

            double aDiff = calculateDiff(a_new, a);
            double hDiff = calculateDiff(h_new, h);

            if(aDiff <= EPSILON && hDiff <= EPSILON){
                break;
            }

            a = a_new;
            h = h_new;

            iteration++;
        }
        hubs = new HashMap<Integer, Double>();
        authorities = new HashMap<Integer, Double>();

        System.out.println(baseSet.size());
        for (Integer page : baseSet){
            //authorities
            if (a_new.get(page) != null){
                authorities.put(Integer.valueOf(orderToId[page]), a_new.get(page));
            }else{
                authorities.put(Integer.valueOf(orderToId[page]),0.0);
            }
            //hubs
            if (a_new.get(page) != null){
                //convert the order to id!
                hubs.put(Integer.valueOf(orderToId[page]), h_new.get(page));
            }else{
                //convert the order to id!
                hubs.put(Integer.valueOf(orderToId[page]),0.0);
            }
        }
    }

    private HashMap<Integer, Double> Multiply(HashMap<Integer, Double> vector, HashMap<Integer, HashMap<Integer, Boolean>> graph) {
        HashMap<Integer, Double> temp = new HashMap<Integer, Double>();
        //System.out.println("temp_before " + temp.size());
        //System.out.println("vector " + vector.size());
        //loop every coordinator
        for (Map.Entry<Integer, Double> coordinate : vector.entrySet()){
            //exist the graph regarding this coordinator
            if (graph.get(coordinate.getKey()) != null){
                double sum =0.0;
                //loop the node in the link regarding this coordinator
                for (Map.Entry<Integer, Boolean> nodeInLink: graph.get(coordinate.getKey()).entrySet()){
                    //if this node exists in the baseset
                    if (vector.get(nodeInLink.getKey()) != null){
                        //add the corresponding coordinator value
                        sum += vector.get(nodeInLink.getKey());
                    }
                }
                //update hashmap
                temp.put(coordinate.getKey(),sum);
            }else{
                //not exist the graph regarding this coordinator
                temp.put(coordinate.getKey(),0.0);
            }
        }
        //System.out.println("temp_before " + temp.size());
        //normalization - Euclidean distance
        double normal_sum = 0.0;
        for (Map.Entry<Integer, Double> coordinate : temp.entrySet()){
//            if (coordinate.getValue() > 200)
//                System.out.println(orderToId[coordinate.getKey()] + " : " + coordinate.getValue());
            normal_sum += Math.pow(coordinate.getValue(), 2);

        }
        for (Map.Entry<Integer, Double> coordinate : temp.entrySet()){
            temp.put(coordinate.getKey(), coordinate.getValue()/Math.sqrt(normal_sum));
        }
        return temp;
    }

    public double calculateDiff(HashMap<Integer, Double> last, HashMap<Integer, Double> cur){
//        System.out.println("last " + last.size());
//        System.out.println("cur " + last.size());
        double diff = 0.0;
        for (Map.Entry<Integer, Double> curEntry : cur.entrySet()){
//            System.out.println(curEntry.getValue() + "    " + last.get(curEntry.getKey()));
            diff += Math.abs(curEntry.getValue() - last.get(curEntry.getKey()));
        }
        return diff;
    }




    /**
     * Rank the documents in the subgraph induced by the documents present
     * in the postings list `post`.
     *
     * @param      post  The list of postings fulfilling a certain information need
     *
     * @return     A list of postings ranked according to the hub and authority scores.
     */
    PostingsList rank(PostingsList post) {
        String[] titles = new String[post.list.size()];
        HashMap<Integer, String> docIDToTitles = new HashMap<>();
        int i = 0;
        for (PostingsEntry entry : post.list){
            titles[i] = getFileName( index.docNames.get(entry.docID) );
            docIDToTitles.put(entry.docID, titles[i]);
            System.out.println(titleToId.get(titles[i]));
            i++;
        }
        //calculate hubs and authorities
        iterate(titles);

        //only return root set
//        System.out.println(authorities.size());
//        for (Map.Entry<Integer, Double> entry : authorities.entrySet()){
//            System.out.println("a : " + entry.getKey() + " " + entry.getValue());
//        }
//        System.out.println(hubs.size());
//        for (Map.Entry<Integer, Double> entry : hubs.entrySet()){
//            System.out.println("h : " + entry.getKey() + " " + entry.getValue());
//        }
        //combine them together
//        for (PostingsEntry entry : post.list){
////            System.out.println("docId "  + entry.docID );
////            System.out.println("title " + docIDToTitles.get(entry.docID));
////            System.out.println("Id " + titleToId.get(docIDToTitles.get(entry.docID)));
////            System.out.println("A" + authorities.get(titleToId.get(docIDToTitles.get(entry.docID))));
//            if( titleToId.get(docIDToTitles.get(entry.docID)) != null){
//                entry.score = authW * authorities.get(titleToId.get(docIDToTitles.get(entry.docID)));
//                entry.score += hubW * hubs.get(titleToId.get(docIDToTitles.get(entry.docID)));
//            }else
//                entry.score = 0.0;
//
//        }

        //return base set!
        PostingsList resPostingList = new PostingsList();
        for (int order : baseSet){
            if (orderToId[order] != null){
                System.out.println(orderToId[order]);
                System.out.println("title " + IdToTiltle.get(Integer.valueOf(orderToId[order])));
                System.out.println(docPath + IdToTiltle.get(Integer.valueOf(orderToId[order])));
                System.out.println(index.docNames.get(23));
                int docID = index.docID.get(docPath + IdToTiltle.get(Integer.valueOf(orderToId[order])));
                PostingsEntry entry = new PostingsEntry(docID);
                entry.score = authW * authorities.get(Integer.valueOf(orderToId[order]));
                entry.score += hubW * hubs.get(Integer.valueOf(orderToId[order]));
                resPostingList.addEntry(entry);
            }
        }

        Collections.sort(resPostingList.list);

        return resPostingList;
    }


    /**
     * Sort a hash map by values in the descending order
     *
     * @param      map    A hash map to sorted
     *
     * @return     A hash map sorted by values
     */
    private HashMap<Integer,Double> sortHashMapByValue(HashMap<Integer,Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<Integer,Double> > list = new ArrayList<Map.Entry<Integer,Double> >(map.entrySet());
      
            Collections.sort(list, new Comparator<Map.Entry<Integer,Double>>() {
                public int compare(Map.Entry<Integer,Double> o1, Map.Entry<Integer,Double> o2) { 
                    return (o2.getValue()).compareTo(o1.getValue()); 
                } 
            }); 
              
            HashMap<Integer,Double> res = new LinkedHashMap<Integer,Double>(); 
            for (Map.Entry<Integer,Double> el : list) { 
                res.put(el.getKey(), el.getValue()); 
            }
            return res;
        }
    } 


    /**
     * Write the first `k` entries of a hash map `map` to the file `fname`.
     *
     * @param      map        A hash map
     * @param      fname      The filename
     * @param      k          A number of entries to write
     */
    void writeToFile(HashMap<Integer,Double> map, String fname, int k) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
            
            if (map != null) {
                int i = 0;
                for (Map.Entry<Integer,Double> e : map.entrySet()) {
                    i++;
                    writer.write(e.getKey() + ": " + String.format("%.5g%n", e.getValue()));
                    if (i >= k) break;
                }
            }
            writer.close();
        } catch (IOException e) {}
    }


    /**
     * Rank all the documents in the links file. Produces two files:
     *  hubs_top_30.txt with documents containing top 30 hub scores
     *  authorities_top_30.txt with documents containing top 30 authority scores
     */
    void rank() {
        //convert the file name into an array
        iterate(titleToId.keySet().toArray(new String[0]));
        HashMap<Integer,Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer,Double> sortedAuthorities = sortHashMapByValue(authorities);
        writeToFile(sortedHubs, "./assignment2/hubs_top_30.txt", 30);
        writeToFile(sortedAuthorities, "./assignment2/authorities_top_30.txt", 30);
    }


    /* --------------------------------------------- */


    public static void main( String[] args ) {
        if ( args.length != 2 ) {
            System.err.println( "Please give the names of the link and title files" );
        }
        else {
            args[0] = "./assignment2/pagerank/" + args[0];
            args[1] = "./assignment2/pagerank/" + args[1];
            HITSRanker hr = new HITSRanker( args[0], args[1], null );
            hr.rank();
        }
    }
} 