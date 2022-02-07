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
    //public Set<Integer> hashset = new HashSet<>();
    public ArrayList<Integer> positions = new ArrayList<>();

    public PostingsEntry(PostingsEntry entry) {
        this.docID = entry.docID;
        this.score = entry.score;
        this.positions = new ArrayList<Integer>(entry.positions);
    }

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
//        this.hashset.add(offset);
        this.positions.add(offset);
    }

    public PostingsEntry(int docID, int offset){
        this.docID = docID;
//        this.hashset.add(offset);
        this.positions.add(offset);
    }

    public PostingsEntry(int docID){
        this.docID = docID;
    }

    public ArrayList<Integer> getPosition(){

        return this.positions;
    }

    public String toString(){
        //System.out.println("1.1.2");
        //return the String format wrote into data file
        String thisSet = "";
        for (int position : positions){
            if (thisSet == ""){
                thisSet = docID + ":" + position;
            }else{
                thisSet = thisSet + "," + position;
            }
        }
        thisSet = thisSet + ";";
        return thisSet;
    }

    public void merge(PostingsEntry entry) {
        if(this.docID == entry.docID) {
            for(int i=0;i<entry.positions.size();++i) {
                this.insertPosition(entry.positions.get(i));
            }
        }
    }

    private void insertPosition(int position) {
        int start = 0;
        int end = this.positions.size() - 1;
        int i;
        while(end >= start){
            i = (start + end)/2;
            if(this.positions.get(i) == position) {
                return;
            } else if(position > this.positions.get(i)) {
                start = i+1;
            } else {
                end = i-1;
            }
        }
        this.positions.add(start,position);
    }
}

