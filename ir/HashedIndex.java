/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        // YOUR CODE HERE
        //
        //if the hashmap doesn't contain the token
        if(!index.containsKey(token)){
            //new PostingList
            PostingsList newPostingList = new PostingsList(token);
            //add new PostingEntry into this PostingsList
            newPostingList.addEntry(docID, offset);
            //update the hashmap
            index.put(token, newPostingList);
        }else{
            //if the hashmap contain the token
            PostingsList currPostings = index.get(token);
            //if the last docID does not match current docID
            if (currPostings.get(currPostings.size()-1).docID != docID){
                //add new PostingEntry into current PostingsList
                currPostings.addEntry(docID, offset);
            }else{
                //update exist PostingEntry in current PostingsList
                //if match means there is already a PostingEntry with same docID in the PostingsList
                currPostings.updateOffset(currPostings.size()-1, offset);
            }
        }
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        if (index.containsKey(token)){
            return index.get(token);
        }else{
            return null;
        }
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }

}
