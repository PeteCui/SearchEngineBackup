package ir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class PersistentScalableHashedIndex extends PersistentHashedIndex {


    /** The directory where the intermediate index files are stored. */
    public static final String INTER_INDEXDIR = "./inter_index";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 3500000L;

    /** redeclare following parameter for modifying easily **/

    /** The dictionary entry byte on disk. */
    public static final int ENTRYSIZE = 24;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** TABLESIZE/3/5(5~6 intermediate files) = 200000L */
//    public static final long SPECIFICTPOINT = 200000L;
    public static final long SPECIFICTPOINT = 1500000L;

    /** Inserted Token count */
    long count = 0L;

    /** Write back round */
    int round = 0;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

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

    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created.
     */
    public PersistentScalableHashedIndex(){
        //the first round (0 round)
        try{
            dictionaryFile = new RandomAccessFile( INTER_INDEXDIR + "/" + DICTIONARY_FNAME + round, "rw" );
            dataFile = new RandomAccessFile( INTER_INDEXDIR + "/" + DATA_FNAME + round, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    @Override
    /**
     * Insert a specific number of tokens
     * As reaching the upper limit, invoke clean up to store the index into desk
     */
    public void insert( String token, int docID, int offset){
         //if count less than SPECIFICTPOINT write token into memory
         if (count < SPECIFICTPOINT){
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
            //update count
            count++;
        }else{
             //if count greater than SPECIFICTPOINT write token from memory into files
             cleanup();
             //update count
             count = 0L;
         }

    }

    /**
     * Write index to file after indexing is done.
     * My plan:
     * 2. store the index on disk
     * 3. clean the hash map index
     * 4. clean the hash table docNames
     * 5. clean the hash table docLengths
     * 6. reset the pointer free to 0
     * 7. update the round
     * 8. recreate the random-access files dictionaryFile and dataFile
     */
    @Override
    public void cleanup(){
        System.err.println( index.keySet().size() + " unique words" );
        System.err.println( "Writing index to disk..." );
        writeIndex();
        System.err.println( "clean the hash map index...");
        index.clear();
        System.err.println( "clean the hash table docNames...");
        docNames.clear();
        System.err.println( "clean the hash table docLengths...");
        docLengths.clear();
        System.err.println( "reset the pointer free to 0...");
        free = 0L;
        //update round
        round ++;
        System.err.println( "round update to " + round);
        System.err.println( "recreate the random-access files dictionaryFile and dataFile...");
        //the N rounds
        try {
            dictionaryFile = new RandomAccessFile(INTER_INDEXDIR + "/" + DICTIONARY_FNAME + round, "rw");
            dataFile = new RandomAccessFile(INTER_INDEXDIR + "/" + DATA_FNAME + round, "rw");
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        System.err.println( "done!" );
    }

    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" ,true);
        //make the hashmap into set, then we can iterate it.
        for ( Map.Entry<Integer,String> entry : docNames.entrySet() ) {
            Integer key = entry.getKey();
            //docId;docName;docLengths\n
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write( docInfoEntry.getBytes() );
        }
        fout.close();
    }

    @Override
    public void writeIndex(){
        int collisions = 0;
        try {
            writeDocInfo();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }



}
