/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.*;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;
    public Set<Integer> hashset = new HashSet<>();

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
       return Double.compare( other.score, score );
    }


    //
    // YOUR CODE HERE
    //
    //the constructor of PostingsEntry
    public PostingsEntry(int docID, double score, int offset){
        this.docID = docID;
        this.score = score;
        this.hashset.add(offset);
    }

    public PostingsEntry(int docID, int offset){
        this.docID = docID;
        this.hashset.add(offset);
    }

    public PostingsEntry(int docID){
        this.docID = docID;
    }

    public Set<Integer> getPositionSet(){
        return this.hashset;
    }
}

