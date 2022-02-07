/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary entry byte on disk. */
    public static final int ENTRYSIZE = 24;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

    /** my debug parameter. */
    int dictionaryEntryCountTest = 0;
    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        //
        //  YOUR CODE HERE
        //
        //start position in the data file
        long ptr;
        //the length of the data should be fetched
        int length;

        public Entry(long ptr, int length){
            this.ptr = ptr;
            this.length = length;
        }
    }


    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
        //
        //  YOUR CODE HERE
        //
        //System.out.println("writeEntry");
        try {
            dictionaryFile.seek(ptr);
            //write ptr into dictionary with Long data type
            dictionaryFile.writeLong(entry.ptr);
            //write length into dictionary with Int data type
            dictionaryFile.writeInt(entry.length);

//            byte[] data = "test".getBytes();
//            dictionaryFile.write(data);
            //dictionaryEntryCountTest += 1;
            //System.out.println(dictionaryEntryCountTest);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println("endEntry");
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) {   
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE 
        //
        Entry entry = null;
        try {
            dictionaryFile.seek(ptr);
            //int 4 byte long 8 byte
            long dataPtr = dictionaryFile.readLong();
            int dataLength = dictionaryFile.readInt();
            entry = new Entry(dataPtr, dataLength);
        } catch (IOException e) {
            //EOF exception
            return null;
        }
        return entry;
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        //make the hashmap into set, then we can iterate it.
        for ( Map.Entry<Integer,String> entry : docNames.entrySet() ) {
            Integer key = entry.getKey();
            //docId;docName;docLengths\n
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write( docInfoEntry.getBytes() );
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try ( BufferedReader br = new BufferedReader(freader) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(Integer.valueOf(data[0]), data[1] );
                docLengths.put(Integer.valueOf(data[0]), Integer.valueOf(data[2]));
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            // 
            //  YOUR CODE HERE
            //
            int i = 0;
            for (Map.Entry<String, PostingsList> entry : index.entrySet()){
                i++;
                //1. write the PostingsList into data file and return the length
                //System.out.println("1.1");
                String dataString = entry.getKey() + " " + entry.getValue().toString();
                //System.out.println(dataString);
                //System.out.println("2");
                int dataLength = writeData(dataString, free);
                //2. create a new entry for this PostingsList
                //System.out.println("3");
                Entry termEntry = new Entry(free, dataLength);
                //3. update the free ptr according to the data length
                free += dataLength;
                //4. calculate the hashValue of term to decide where to store it
                long entryPtr = getEntryPtr(entry.getKey());
                //5. check if collisions
                //System.out.println("Key:" + entry.getKey() + " Hash Value:: " + entryPtr);
                Entry currEntry = readEntry(entryPtr);
                //Entry termEntry = new Entry(free, 10);
                if (currEntry == null){
                    //5.1 write the free ptr and data length into dictionary file
                    //System.out.println(entry.getKey());
                    //System.out.println("1 new entry:" + i + "Key :"+ entry.getKey());
                    writeEntry(termEntry, entryPtr);
                }else if(currEntry.ptr == 0L || currEntry.length == 0){
                    //5.2 no entry at here
                    //System.out.println(entry.getKey());
                    System.out.println("2 new entry:" + i + "Key :"+ entry.getKey());
                    writeEntry(termEntry, entryPtr);
                }else{
                    //flag
                    boolean flag = true;
                    //5.3 loop until null and write the free ptr and data length into dictionary file
                    while (flag){
                        //System.out.println("3 new entry:" + i + "Key :"+ entry.getKey());
                        //update collisions
                        collisions++;
                        //update entryPtr
                        entryPtr += ENTRYSIZE;
                        //return to the beginning of dictionary file
                        if(entryPtr > ENTRYSIZE * TABLESIZE){
                            entryPtr = entryPtr % TABLESIZE;
                        }
                        Entry newEntry = readEntry(entryPtr);
                        if (newEntry == null || (newEntry.ptr == 0L || newEntry.length == 0)){
                            //update flag
                            flag = false;
                            //System.out.println(entry.getKey());
                            writeEntry(termEntry, entryPtr);
                        }
                    }
                }
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }


    public long getEntryPtr(String token){
        int raw_value = token.hashCode();
        if (raw_value < 0){
            raw_value = (~raw_value+1);
            long entryPtr = (raw_value % TABLESIZE) * ENTRYSIZE;
            //make it positive
            return entryPtr;
        }else{
            //return directly if raw_value is already positive
            long entryPtr = (raw_value % TABLESIZE) * ENTRYSIZE;
            return entryPtr;
        }
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        //fetch dictionary entry
        long entryPtr = getEntryPtr(token);
        Entry currEntry = readEntry(entryPtr);
        //System.out.println("test.ptr: " + currEntry.ptr);
        //System.out.println("test.length: " + currEntry.length);
        if (currEntry.length == 0)
            return null;
        //fetch data
        String dataString = readData(currEntry.ptr, currEntry.length);
        PostingsList curPostinsList = convertPostings(dataString);
        if (Objects.equals(curPostinsList.term, token)){
            return curPostinsList;
        }else{
            int count = 0;
            while ( count < 1000 ){
                //update entryPtr
                entryPtr += ENTRYSIZE;
                //fetch dictionary entry again
                currEntry = readEntry(entryPtr);
                if (currEntry.length == 0)
                    return null;
                dataString = readData(currEntry.ptr, currEntry.length);
                System.out.println("dataString: " + dataString);
                curPostinsList = convertPostings(dataString);
                if (Objects.equals(curPostinsList.term, token)){
                    return curPostinsList;
                } else{
                    count++;
                }
            }
        }

        //convert string to postingsList

        return null;
    }

    public PostingsList convertPostings(String postingsListString){
        //System.out.println("input:" + postingsListString);
        PostingsList newPostingList = new PostingsList();
        String[] stringArray = postingsListString.split(" ");
        //System.out.println("term: " + stringArray[0]);
        //System.out.println("postingsList: " + stringArray[1]);
        newPostingList.term = stringArray[0];
        String[] postingsArray = stringArray[1].split(";");
        for (String entryStr : postingsArray){
            //System.out.println("each doc:" + entryStr);
            String[] entryArray = entryStr.split(":");
            //docId
            int docId = Integer.parseInt(entryArray[0]);
            //System.out.println("docId:" + docId);
            newPostingList.addEntry(docId);
            String[] offsetArray = entryArray[1].split(",");
            //offset
            for (String offset : offsetArray){
                //System.out.println("offset:" + offset);
                newPostingList.updateOffset(newPostingList.size()-1, Integer.parseInt(offset));
            }
        }
        return newPostingList;
    }

    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        //  YOUR CODE HERE
        //
        if(!index.containsKey(token)){
            //new PostingList
            PostingsList newPostingList = new PostingsList();
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
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.println( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
}
