/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.Set;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType, NormalizationType normType ) { 
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        if (queryType == QueryType.INTERSECTION_QUERY){
            return intersectionQuery(query);
        }else if(queryType == QueryType.PHRASE_QUERY){
            return phraseQuery(query);
        }
        return null;
    }

    /** creat an array to store all the returned postingsList result of terms */
    public PostingsList[] getTermsPostingsLists(Query query){
        //creat an array to store all the returned postingsList result of terms
        PostingsList[] allList = new PostingsList[query.size()];
        //loop all the term in the query
        for (int i = 0; i < query.size(); i++) {
            allList[i] = index.getPostings(query.queryterm.get(i).term);
            System.out.println(query.queryterm.get(i).term + " size : " + allList[i].size());
        }
        return allList;
    }
    public PostingsList intersectionQuery(Query query) {

        int termNum = query.size();
        System.out.println("term num: " + termNum);
        if (termNum <= 1) {
            return index.getPostings(query.queryterm.get(0).term);
        } else {
            //creat an array to store all the returned postingsList result
            PostingsList[] allList = getTermsPostingsLists(query);
            PostingsList resultList = null;
            for (int i = 0; i < termNum; i++) {
                if (i == 0) {
                    resultList = intersectTwoTerm(allList[i], allList[i + 1]);
                    i++;
                } else {
                    resultList = intersectTwoTerm(resultList, allList[i]);
                }
            }
            //System.out.println(resultList.toString());
            return resultList;
        }
    }

    public PostingsList intersectTwoTerm(PostingsList list1, PostingsList list2){
        PostingsList resultList =  new PostingsList();
        //int entryNum = Math.min(list1.size(), list2.size());
        int i = 0;
        int j = 0;
        while(i < list1.size() && j < list2.size()){

            if (list1.get(i).docID == list2.get(j).docID){
                resultList.addEntry(list1.get(i));
                i++;
                j++;
            }else if (list1.get(i).docID < list2.get(j).docID){
                i++;
            }else if (list1.get(i).docID > list2.get(j).docID){
                j++;
            }
        }
        return resultList;
    }

    public PostingsList phraseQuery(Query query) {

        int termNum = query.size();
        System.out.println("term num: " + termNum);
        if (termNum <= 1){
            return index.getPostings(query.queryterm.get(0).term);
        }else{
            //the number of term greater than 1
            PostingsList[] allList = getTermsPostingsLists(query);
            PostingsList resultList = null;
            for (int i = 0; i < termNum; i++){
                if (i == 0){
                    resultList = phraseTwoTerm(allList[i], allList[i + 1],1);
                    i++;
                }else{
                    //phrase with the resultList
                    resultList = phraseTwoTerm(resultList, allList[i],1);
                }
            }

            return resultList;
        }
    }

    public PostingsList phraseTwoTerm(PostingsList list1, PostingsList list2, int offset){
        PostingsList resultList =  new PostingsList();
        int i = 0;
        int j = 0;
        while(i < list1.size() && j < list2.size()){
            if (list1.get(i).docID == list2.get(j).docID){
                int currDocID = list1.get(i).docID;
                Set<Integer> positions1 = list1.get(i).hashset;
                Set<Integer> positions2 = list2.get(j).hashset;

                //some test output
//                System.out.println(list1.get(i).docID + "p1 position");
//                for (int p1 : list1.get(i).hashset){
//                    System.out.println(p1);
//                }
//                System.out.println(list2.get(j).docID + "p2 position");
//                for (int p2 : list2.get(j).hashset){
//                    System.out.println(p2);
//                }
                //loop and check if there is position-offset in list1
                //from back to check the front
                for (int p2: positions2){
                    //System.out.println("check if there is: " + (p2 - offset) + " in p1");
                    if(positions1.contains(p2 - offset)){
                        System.out.println(p2 - offset + "in the p1");
                        //if resultList is null
                        if (resultList.size() == 0){
                            resultList.addEntry(currDocID, p2);
                        }else{
                            //if current docID doesn't exist in the current resultList
                            if (resultList.get(resultList.size()-1).docID != currDocID){
                                resultList.addEntry(currDocID, p2);
                            }else{
                                //if current docID exists in the current resultList
                                resultList.updateOffset(resultList.size()-1, p2);
                            }
                        }
                    }
                }
                i++;
                j++;
            }else if (list1.get(i).docID < list2.get(j).docID){
                i++;
            }else if (list1.get(i).docID > list2.get(j).docID){
                j++;
            }
        }
        return resultList;
    }
}