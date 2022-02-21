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

    /** Used for return specific term postingsList, if this is a mixed postingList no need to set it */
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

    PostingsList(String term){
        this.term = term;
    }

    PostingsList(){
        this.term = null;
    }

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
        return  term + " " + thisPostingsList;
    }

    //binary search
    private void mergeEntry(PostingsEntry newEntry) {
        int start = 0;
        int end = this.list.size()-1;
        int mid;
        while(start <= end) {
            mid = (end - start) / 2 + start;
            if(this.list.get(mid).docID == newEntry.docID) {
                //same id
                this.list.get(mid).mergePosition(newEntry);
                return ;
            } else if(newEntry.docID > this.list.get(mid).docID) {
                //right
                start = mid+1;
            } else {
                //left
                end = mid-1;
            }
        }
        //if not find add the entry at here
        this.list.add(start, newEntry);
    }

    public void mergeList(PostingsList list2) {
        for(int i=0;i<list2.list.size();++i) {
            //create a new entry to merge
            this.mergeEntry(new PostingsEntry(list2.list.get(i)));
        }
    }

    public void calculateTD_IDF(NormalizationType normType, Index index) {
        //N
        int N = index.docLengths.size();
        //System.out.println("N: " + N);
        //df_t
        int df_t;
        df_t = list.size();
        //System.out.println("df_t: " + df_t);
        double idf_t = Math.log((double)N/(double)df_t);
        for (PostingsEntry entry : list){
            int tf = entry.positions.size();
            //System.out.println("tf: " + tf);
            double lenDoc;
            if(normType == NormalizationType.NUMBER_OF_WORDS){
                lenDoc = index.docLengths.get(entry.docID);
            }else{
                lenDoc = index.docEuclidean.get(entry.docID);
            }
            entry.score =  (tf * idf_t)/lenDoc;
            //System.out.println(entry.docID + " : " + entry.score);
        }
    }

    public void readPageRank(Index index) {
        for (PostingsEntry entry : list){
            //get the doc Name
            //System.out.println("entry id " + entry.docID);
            String rawName = index.docNames.get(entry.docID);
            String[] nameElements = rawName.split("\\\\");
            String name = nameElements[nameElements.length-1];
            //System.out.println("entry name " + name);
            //get the page rank from mapping
            if (index.docNamePageRank.get(name) == null){
                entry.score = 0.0;
            }else{
                entry.score = index.docNamePageRank.get(name);
            }
            System.out.println(entry.docID + " : " + entry.score);
        }
    }

    public void calculateNetScore(double factor, Index index) {
        //normalization
        double pageRankSum = 0.0;
        double TD_IDFSum = 0.0;
        for (PostingsEntry entry : list){
            //get the doc Name
            String rawName = index.docNames.get(entry.docID);
            String[] nameElements = rawName.split("\\\\");
            String name = nameElements[nameElements.length-1];
            //get the page rank from mapping
            if (index.docNamePageRank.get(name) == null){
                pageRankSum += 0.0;
            }else{
                pageRankSum += index.docNamePageRank.get(name);
            }
            TD_IDFSum += entry.score;
        }
        //calculate net score
        for (PostingsEntry entry : list){
            //get the doc Name
            String rawName = index.docNames.get(entry.docID);
            String[] nameElements = rawName.split("\\\\");
            String name = nameElements[nameElements.length-1];
            //get the page rank from mapping
            if (index.docNamePageRank.get(name) == null){
                entry.score = factor * (entry.score / TD_IDFSum);
            }else{
                entry.score = factor * (entry.score / TD_IDFSum) + (1 - factor) * (index.docNamePageRank.get(name) / pageRankSum);
            }
        }
        System.out.println("sum of TF IDF:" + TD_IDFSum);
        System.out.println("sum of rank page: " + pageRankSum);
    }
}

