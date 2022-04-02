/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.*;
import java.util.*;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /**
     * The index to be searched by this Searcher.
     */
    Index index;

    /**
     * The k-gram index to be searched by this Searcher
     */
    KGramIndex kgIndex;

    /**
     * The file containing the pageranks.
     */
    String rank_file;

    /**
     * The file containing the euclidean length.
     */
    String euclidean_file;

    /**
     * The factor for net score
     */
    public static final double factor = 0.5;

    /**
     * HITSRanker
     */
    HITSRanker hitsRanker;

    String linksFilename = "./assignment2/pagerank/linksDavis.txt";

    String titlesFilename = "./assignment2/pagerank/davisTitles.txt";

    /**
     * Constructor
     */
    public Searcher(Index index, KGramIndex kgIndex, String rank_file, String euclidean_file) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.rank_file = rank_file;
        createPageRankMapping();
        this.euclidean_file = euclidean_file;
        this.hitsRanker = new HITSRanker(linksFilename, titlesFilename, index);
        createEuclideanMapping();
    }

    private void createPageRankMapping() {
        System.out.println("Page Rank are loaded from disk!");
        try {
            FileInputStream inputStream = new FileInputStream(rank_file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                String[] elements = line.split(":");
                index.docNamePageRank.put(elements[0], Double.valueOf(elements[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createEuclideanMapping() {
        System.out.println("Euclidean length are loaded from disk!");
        try {
            FileInputStream inputStream = new FileInputStream(euclidean_file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                String[] elements = line.split(":");
                index.docEuclidean.put(Integer.valueOf(elements[0]), Double.valueOf(elements[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Searches the index for postings matching the query.
     *
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normType) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        if (queryType == QueryType.INTERSECTION_QUERY) {
            return intersectionQuery(query);
        } else if (queryType == QueryType.PHRASE_QUERY) {
            return phraseQuery(query);
        } else if (queryType == QueryType.RANKED_QUERY) {
            return rankQuery(query, rankingType, normType);
        }

        return null;
    }

    /**
     * creat an array to store all the returned postingsList result of terms
     */
    public PostingsList[] getTermsPostingsLists(Query query) {
        //creat an array to store all the returned postingsList result of terms
        PostingsList[] allList = new PostingsList[query.size()];
        //loop all the term in the query
        for (int i = 0; i < query.size(); i++) {
            allList[i] = index.getPostings(query.queryterm.get(i).term);
            //if a word is not found, return null
            if (allList[i] == null)
                return null;
        }
        return allList;
    }

    public PostingsList intersectionQuery(Query query) {

        int termNum = query.size();
        System.out.println("term num: " + termNum);
        if (termNum <= 1) {
            //test PostingsList.toString
            //System.out.println(index.getPostings(query.queryterm.get(0).term).toString());
            return index.getPostings(query.queryterm.get(0).term);
        } else {
            //creat an array to store all the returned postingsList result
            PostingsList[] allList = getTermsPostingsLists(query);
            if (allList == null) {
                return null;
            }

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

    public PostingsList intersectTwoTerm(PostingsList list1, PostingsList list2) {
        PostingsList resultList = new PostingsList();
        //int entryNum = Math.min(list1.size(), list2.size());
        int i = 0;
        int j = 0;
        while (i < list1.size() && j < list2.size()) {

            if (list1.get(i).docID == list2.get(j).docID) {
                resultList.addEntry(list1.get(i));
                i++;
                j++;
            } else if (list1.get(i).docID < list2.get(j).docID) {
                i++;
            } else if (list1.get(i).docID > list2.get(j).docID) {
                j++;
            }
        }
        return resultList;
    }

    public PostingsList phraseQuery(Query query) {

        int termNum = query.size();
        System.out.println("term num: " + termNum);
        if (termNum <= 1) {
            return index.getPostings(query.queryterm.get(0).term);
        } else {
            //the number of term greater than 1
            PostingsList[] allList = getTermsPostingsLists(query);
            if (allList == null) {
                return null;
            }
            PostingsList resultList = null;
            for (int i = 0; i < termNum; i++) {
                if (i == 0) {
                    resultList = phraseTwoTerm(allList[i], allList[i + 1], 1);
                    i++;
                } else {
                    //phrase with the resultList
                    resultList = phraseTwoTerm(resultList, allList[i], 1);
                }
            }

            return resultList;
        }
    }

    public PostingsList phraseTwoTerm(PostingsList list1, PostingsList list2, int offset) {
        PostingsList resultList = new PostingsList();
        int i = 0;
        int j = 0;
        while (i < list1.size() && j < list2.size()) {
            if (list1.get(i).docID == list2.get(j).docID) {
                int currDocID = list1.get(i).docID;
                ArrayList<Integer> positions1 = list1.get(i).positions;
                ArrayList<Integer> positions2 = list2.get(j).positions;

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
                for (int p2 : positions2) {
                    //System.out.println("check if there is: " + (p2 - offset) + " in p1");

                    if (positions1.contains(p2 - offset)) {
                        //System.out.println(p2 - offset + "in the p1");
                        //if resultList is null
                        if (resultList.size() == 0) {
                            resultList.addEntry(currDocID, p2);
                        } else {
                            //if current docID doesn't exist in the current resultList
                            if (resultList.get(resultList.size() - 1).docID != currDocID) {
                                resultList.addEntry(currDocID, p2);
                            } else {
                                //if current docID exists in the current resultList
                                resultList.updateOffset(resultList.size() - 1, p2);
                            }
                        }
                    }
                }
                i++;
                j++;
            } else if (list1.get(i).docID < list2.get(j).docID) {
                i++;
            } else if (list1.get(i).docID > list2.get(j).docID) {
                j++;
            }
        }
        return resultList;
    }

    public PostingsList rankQuery(Query query, RankingType rankingType, NormalizationType normType) {
        int termNum = query.size();
        System.out.println("term num: " + termNum);
        if (termNum <= 1) {
            String term = query.queryterm.get(0).term;
            //fetch the corresponding postingsList
            PostingsList resPostingsList = index.getPostings(term);
            //put the weight of this query term into its result postingsList
            resPostingsList.weight = query.queryterm.get(0).weight;
            if (resPostingsList == null) {
                return null;
            }
            //score the postingsList
            if (rankingType == RankingType.TF_IDF) {
                resPostingsList.calculateTD_IDF(normType, index);
                //sort the postingsList according to the score
                Collections.sort(resPostingsList.list);
            } else if (rankingType == RankingType.PAGERANK) {
                resPostingsList.readPageRank(index);
                //sort the postingsList according to the score
                Collections.sort(resPostingsList.list);
            } else if (rankingType == RankingType.COMBINATION) {
                resPostingsList.calculateTD_IDF(normType, index);
                resPostingsList.calculateNetScore(factor, index);
                //sort the postingsList according to the score
                Collections.sort(resPostingsList.list);
            } else if (rankingType == RankingType.HITS) {
                //HITRanker handle the rawPostingsList
                //System.out.println("GUI modify successfully");
                return hitsRanker.rank(resPostingsList);
            }
            return resPostingsList;

        } else {
            //approximation of the cosine similarity
            HashSet<String> termSet = new HashSet<String>();
            for (int i = 0; i < query.size(); i++) {
                termSet.add(query.queryterm.get(i).term);
            }
            //creat an array to store all the returned postingsList result of terms
            PostingsList[] allList = new PostingsList[termSet.size()];
            //loop all the term in the query
            int i = 0;
            for (String term : termSet) {
                //System.out.println("term: "+ term);
                allList[i] = index.getPostings(term);
                //put the weight of this query term into its result postingsList
                allList[i].weight = query.queryterm.get(i).weight;
                i++;
            }
            //if totally a null list return null
            PostingsList[] noNullList = eliminateNullList(allList);
            if (noNullList == null) {
                return null;
            }
            //System.out.println(noNullList.length);
            //score the postingsList
            for (PostingsList list : noNullList) {
                if (rankingType == RankingType.TF_IDF) {
                    list.calculateTD_IDF(normType, index);
                } else if (rankingType == RankingType.PAGERANK) {
                    list.readPageRank(index);
                } else if (rankingType == RankingType.COMBINATION) {
                    //calculate the TD_IDF
                    list.calculateTD_IDF(normType, index);
                }
            }
            PostingsList resPostingsList = unitePostingsList(noNullList, rankingType);

            //if combination, we should add the page rank together
            if (rankingType == RankingType.COMBINATION) {
                resPostingsList.calculateNetScore(factor, index);
                //sort the postingsList according to the score
                Collections.sort(resPostingsList.list);
            } else if (rankingType == RankingType.HITS) {
                //HITRanker handle the rawPostingsList
                //System.out.println("GUI modify successfully");
                resPostingsList = hitsRanker.rank(resPostingsList);
            } else {
                //sort the postingsList according to the score
                Collections.sort(resPostingsList.list);
            }
            return resPostingsList;
        }
    }


    private PostingsList[] eliminateNullList(PostingsList[] allList) {
        int size = 0;
        //System.out.println("pre:" + allList.length);
        for(PostingsList list : allList){
            if (list != null) {
                size++;
            }
        }
        if (size == 0){
            return null;
        }
        //create a new size array
        PostingsList[] noNUllPostingsList = new PostingsList[size];
        int i = 0;
        for (PostingsList list : allList) {
            if (list != null){
                noNUllPostingsList[i] = list;
                i++;
            }
        }
        //System.out.println("after:" + i);
        return noNUllPostingsList;
    }

    //Unite the postingsList for different terms, if two terms appear in the same article, add the score together
    private PostingsList unitePostingsList(PostingsList[] allList, RankingType rankingType) {
        if (allList == null){
            return null;
        }
        if (allList.length == 1){
            return allList[0];
        }
        PostingsList resPostingsList = null;
        for (int i = 0; i < allList.length; i++){
            if (i == 0){
                resPostingsList = uniteTwoPostingsList(allList[i], allList[i + 1], rankingType);
                i++;
            }else{
                //unite with the resultList
                resPostingsList = uniteTwoPostingsList(resPostingsList, allList[i], rankingType);
            }
        }

        return resPostingsList;
    }

    private PostingsList uniteTwoPostingsList(PostingsList list1, PostingsList list2, RankingType rankingType) {
        PostingsList resultList =  new PostingsList();
        int i = 0;
        int j = 0;
        while (i < list1.size() && j < list2.size()){
            if (list1.get(i).docID == list2.get(j).docID){
                //add the score together
                if (rankingType != RankingType.PAGERANK){
                    list1.get(i).score += list2.get(j).score;
                }
                resultList.addEntry(list1.get(i));
                i++;
                j++;
            }else if (list1.get(i).docID < list2.get(j).docID){
                resultList.addEntry(list1.get(i));;
                i++;
            }else if (list1.get(i).docID > list2.get(j).docID){
                resultList.addEntry(list2.get(j));
                j++;
            }
        }
        //add the rest entry into the resultList
        if (i == list1.size()){
            while (j < list2.size()){
                resultList.addEntry(list2.get(j));
                j++;
            }
        }else{
            while (i < list1.size()){
                resultList.addEntry(list1.get(i));
                i++;
            }
        }

        return resultList;
    }

}