/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;

public class PostingsList {
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    public String term;

    /** Number of postings in this list. */
    public int size() {

        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {

        return list.get( i );
    }
    //
    //  YOUR CODE HERE
    //
    /** Add the new entry into list*/
    public void addEntry(int docID){

        this.list.add(new PostingsEntry(docID));

    }

    public void addEntry(int docID, int offset){

        this.list.add(new PostingsEntry(docID, offset));
    }

    public void addEntry(PostingsEntry entry){

        this.list.add(entry);
    }

    /** update the offset in current entry*/
    public void updateOffset(int index, int offset) {

        this.list.get(index).positions.add(offset);
    }

    @Override
    public String toString() {
        //return the String format wrote into data file
        String thisPostingsList = "";
        //System.out.println("1.1.1");
        for (PostingsEntry entry : list){
            thisPostingsList += entry.toString();
        }
        return thisPostingsList;
    }
}

