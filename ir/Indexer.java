/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;


/**
 *   Processes a directory structure and indexes all PDF and text files.
 */
public class Indexer {

    /** The index to be built up by this Indexer. */
    Index index;

    /** K-gram index to be built up by this Indexer */
    KGramIndex kgIndex;

    /** The next docID to be generated. */
    private int lastDocID = 0;

    /** The patterns matching non-standard words (e-mail addresses, etc.) */
    String patterns_file;

    /** The path for euclidean file */
    String euclidean_file;

    /** The hashmap for calculating euclidean distance */
    public HashMap<Integer, HashMap<String, Integer>> termCountInDocMap;

    /** Count the df */
    public HashMap<String, Integer> dfMap;

    /** Count the term appears in a doc */
    public HashMap<String, Boolean> termInDocMap;

    public HITSRanker hitsRanker;

    /* ----------------------------------------------- */


    /** Constructor */
    public Indexer(Index index, KGramIndex kgIndex, String patterns_file, String euclidean_file) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.patterns_file = patterns_file;
        this.euclidean_file = euclidean_file;
        this.termCountInDocMap = new HashMap<Integer, HashMap<String, Integer>>();
        this.dfMap = new HashMap<String, Integer>();
        this.termInDocMap = new HashMap<String, Boolean>();
    }


    /** Generates a new document identifier as an integer. */
    private int generateDocID() {
        return lastDocID++;
    }



    /**
     *  Tokenizes and indexes the file @code{f}. If <code>f</code> is a directory,
     *  all its files and subdirectories are recursively processed.
     */
    public void processFiles( File f, boolean is_indexing ) {
        // do not try to index fs that cannot be read
        if (is_indexing) {
            if ( f.canRead() ) {
                if ( f.isDirectory() ) {
                    String[] fs = f.list();
                    // an IO error could occur
                    if ( fs != null ) {
                        for ( int i=0; i<fs.length; i++ ) {
                            processFiles( new File( f, fs[i] ), is_indexing );
                        }
                    }
                } else {
                    // First register the document and get a docID
                    int docID = generateDocID();
                    if ( docID%1000 == 0 ) System.err.println( "Indexed " + docID + " files" );
                    try {
                        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
                        Tokenizer tok = new Tokenizer( reader, true, false, true, patterns_file );
                        int offset = 0;
                        while ( tok.hasMoreTokens() ) {
                            String token = tok.nextToken();
                            insertIntoIndex( docID, token, offset++ );
                            //updateTFHashMap(docID, token);
                            //updateTermInDocMap(token);
                        }
                        //updateDfMap();
                        //put the path not the real file name into the hash map
                        index.docNames.put( docID, f.getPath() );
                        index.docLengths.put( docID, offset );
                        reader.close();
                    } catch ( IOException e ) {
                        System.err.println( "Warning: IOException during indexing." );
                    }
                }
            }
        }
    }

    /* ----------------------------------------------- */


    /**
     *  Indexes one token.
     */
    public void insertIntoIndex( int docID, String token, int offset ) {
        index.insert( token, docID, offset );
        if (kgIndex != null)
            kgIndex.insert(token);
    }

    /**
     * the term frequency in different doc
     */
    public void updateTFHashMap(int docID, String token){
        //a new doc
        if (termCountInDocMap.get(docID) == null){
            HashMap<String, Integer> newMap = new HashMap<>();
            newMap.put(token, 1);
            termCountInDocMap.put(docID,newMap);
        }else{
            //old doc new word
            if(termCountInDocMap.get(docID).get(token) != null){
                //old doc old word
                int count = termCountInDocMap.get(docID).get(token) + 1;
                termCountInDocMap.get(docID).put(token,count);
            }else{
                //old doc new word
                termCountInDocMap.get(docID).put(token,1);
            }
        }
    }

    /**
     * the term frequency in one doc
     */
    public void updateTermInDocMap(String token) {
        termInDocMap.put(token,true);
    }

    /**
     * update the df, the value of corresponding key(term) ++
     */
    public void updateDfMap(){
        for (String term : termInDocMap.keySet()){
            if (dfMap.get(term) == null){
                dfMap.put(term,1);
            }else {
                int count = dfMap.get(term) + 1;
                dfMap.put(term,count);
            }
        }
        //clean for next iteration
        termInDocMap.clear();
    }



    /**
     * calculating Euclidean distance and make it persistence then clean up hash map for next iteration
     */
    public void cleanupHashMap() {
        System.out.println("Calculate the euclidean distance....");
        int N = index.docLengths.size();
        //loop each doc
        for (Map.Entry<Integer, HashMap<String, Integer>> docEntry : termCountInDocMap.entrySet()){
            double sum = 0.0;
            if (docEntry.getKey() % 1000 == 0){
                System.out.println("calculate docID" + docEntry.getKey());
            }
            //loop each term in the doc
            for (Map.Entry<String, Integer> termEntry : docEntry.getValue().entrySet()){
                System.out.println("cleanupHashMap_term: " + termEntry.getKey());
//                if (dfMap.get(termEntry.getKey()) == 0){
//                    continue;
//                }
                int df_t = dfMap.get(termEntry.getKey());
                double idf_t = Math.log((double)N/(double)df_t);
                sum += Math.pow(termEntry.getValue() * idf_t,2);
            }
            double euclideanLength = Math.sqrt(sum);
            String data = docEntry.getKey() + ":" + euclideanLength;
            //persistence
            try{
                RandomAccessFile file = new RandomAccessFile(euclidean_file, "rw");
                //put at the last place
                file.seek(file.length());
                file.write(data.getBytes());
                file.write("\n".getBytes());
                file.close();
            }catch( Exception e){
                System.out.println("write euclidean distance wrong!");
            }
        }
        System.out.println("Save euclidean distance ok....");
    }

}

