/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.*;
import java.nio.charset.*;
import java.io.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
        String term;
        double weight;

        /**This is a constructor **/
        QueryTerm( String t, double w ) {
            term = t;
            weight = w;
        }
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
        StringTokenizer tok = new StringTokenizer( queryString );
        while ( tok.hasMoreTokens() ) {
            queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
        }    
    }
    
    
    /**
     *  Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for ( QueryTerm t : queryterm ) {
            len += t.weight; 
        }
        return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for ( QueryTerm t : queryterm ) {
            queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
        }
        return queryCopy;
    }
    
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {
        //
        //  YOUR CODE HERE
        //
        //count the original query
        HashMap<String,Double> vector = new HashMap<>();
        for (QueryTerm queryTerm : queryterm){
            if (!vector.containsKey(queryTerm.term)){
                vector.put(queryTerm.term, queryTerm.weight * alpha);
            }else{
                double previousValue = vector.get(queryTerm.term);
                previousValue += previousValue;
                vector.put(queryTerm.term, previousValue);
            }
        }

        //handle the relevant file
        int statisticsLen = docIsRelevant.length;
        int relevantCount = 0;
        for (int i= 0; i < statisticsLen; i++) {
            //if this result has been selected
            if (docIsRelevant[i]) {
                relevantCount++;
                //get the file name
                String file = engine.index.docNames.get(results.get(i).docID);
                System.out.println(file);
                //for 3.2
                if (file.equals("dataset\\davisWiki\\Math.f")){
                    file = "dataset\\davisWiki\\Mathematics.f";
                }
                System.out.println(file);
                //if (file.equals("./assignment2/pagerank/davisTitles.txt"+"Math.f"))
                try {
                    //read this file
                    Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                    //tokenize this file
                    Tokenizer tok = new Tokenizer(reader, true, false, true, engine.patterns_file);
                    while (tok.hasMoreTokens()) {
                        String token = tok.nextToken();
                        if (!vector.containsKey(token)) {
                            vector.put(token, (1.0/relevantCount) * beta);
                        } else {
                            double previousValue = vector.get(token);
                            vector.put(token, previousValue + (1.0/relevantCount) * beta);
                        }
                    }
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }

        queryterm = new ArrayList<QueryTerm>();
        for (HashMap.Entry<String, Double> entry : vector.entrySet()) {
            QueryTerm query = new QueryTerm(entry.getKey(), entry.getValue());
            queryterm.add(query);
        }

    }
}


