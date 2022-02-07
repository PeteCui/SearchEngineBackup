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
    public ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

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

    private PostingsEntry addAndMerge(PostingsEntry entry) {
        int start = 0;
        int end = this.list.size()-1;
        int i;
        while(end >= start) {
            i = (start + end)/2;
            if(this.list.get(i).docID == entry.docID) {
                this.list.get(i).merge(entry);
                return this.list.get(i);
            } else if(entry.docID > this.list.get(i).docID) {
                start = i+1;
            } else {
                end = i-1;
            }
        }
        this.list.add(start, entry);
        return entry;
    }

    public void sortMerge(PostingsList list2) {
        for(int i=0;i<list2.list.size();++i) {
            this.addAndMerge(new PostingsEntry(list2.list.get(i)));
        }
    }
}

