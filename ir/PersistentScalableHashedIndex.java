package ir;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class PersistentScalableHashedIndex extends PersistentHashedIndex {


    /** The directory where the intermediate index files are stored. */
    public static final String INTER_INDEXDIR = "./inter_index";

    /** The dictionary hash table on disk can fit this many entries. */
//    public static final long TABLESIZE = 3500000L;
    public static final long TABLESIZE = 611953L;

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

    /** The queue for intermediate index file. */
    public LinkedList<String> fileQueue;

    /** This background thread responsible for checking the number of intermediate file */
    Background back;


    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created.
     */
    public PersistentScalableHashedIndex(){
        //the first round (0 round)
        //the file in INDEXDIR will also create, because the super() automatically call
        try{
            dictionaryFile = new RandomAccessFile( INTER_INDEXDIR + "/" + DICTIONARY_FNAME + round, "rw" );
            dataFile = new RandomAccessFile( INTER_INDEXDIR + "/" + DATA_FNAME + round, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        //init file queue
        fileQueue = new LinkedList<>();
        //start the backend thread to check if at least two intermediate files
        back = new Background(fileQueue, this);
        back.start();
    }

    @Override
    /**
     * Insert a specific number of tokens
     * As reaching the upper limit, invoke clean up to store the index into desk
     */
    public void insert( String token, int docID, int offset){

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
        //if count greater than SPECIFICTPOINT write token from memory into files
        if (count > SPECIFICTPOINT){
             periodicCleanup();
             //update count
             count = 0L;
         }

    }

    /**
     * Write index to file as reach a specific point.
     * My plan:
     * 2. store the index on disk
     * 2.1 check if at least two intermediate indexes have been written
     * 2.2 a thread for merging start.
     * 3. clean the hash map index
     * 4. clean the hash table docNames
     * 5. clean the hash table docLengths
     * 6. reset the pointer free to 0
     * 7. update the round
     * 8. recreate the random-access files dictionaryFile and dataFile
     */
    public void periodicCleanup(){
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
        System.err.println( "recreate the random-access files dictionaryFile and dataFile...");
        //the N rounds
        try {
            dictionaryFile.close();
            dataFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            synchronized (fileQueue){
                //offer current dictionary file name and data file name into queue
                fileQueue.offer(String.valueOf(round));
            }
            System.out.println("fileQueue size from main thread " + fileQueue.size());
            //update round
            round ++;
            System.err.println( "round update to " + round);
            dictionaryFile = new RandomAccessFile(INTER_INDEXDIR + "/" + DICTIONARY_FNAME + round, "rw");
            dataFile = new RandomAccessFile(INTER_INDEXDIR + "/" + DATA_FNAME + round, "rw");
        } catch ( IOException e ) {
            //e.printStackTrace();
        }

        System.err.println( "periodic cleanup done!" );
    }

    @Override
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.println( "Writing index to disk..." );
        writeIndex();
        try {
            dictionaryFile.close();
            dataFile.close();
            synchronized (fileQueue){
                //offer current dictionary file name and data file name into queue
                fileQueue.offer(String.valueOf(round));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //tell the background the indexing is over
        System.out.println("queue size " + fileQueue.size());
        back.callStop();
        System.err.println( "Writing clean up merge to disk..." );
        /** add!!!!the last merge!*/




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

            int i = 0;
            for (Map.Entry<String, PostingsList> entry : index.entrySet()) {
                i++;
                //1. write the PostingsList into data file and return the length
                //System.out.println("1.1");
                String dataString = entry.getKey() + " " + entry.getValue().toString();
                //System.out.println(dataString);
                //System.out.println("2");
                int dataLength = writeData(dataFile, dataString, free);
                //2. create a new entry for this PostingsList
                //System.out.println("3");
                Entry termEntry = new Entry(free, dataLength);
                //3. update the free ptr according to the data length
                free += dataLength;
                //4. calculate the hashValue of term to decide where to store it
                long entryPtr = getEntryPtr(entry.getKey());
                //5. check if collisions
                //System.out.println("Key:" + entry.getKey() + " Hash Value:: " + entryPtr);
                Entry currEntry = readEntry(dictionaryFile, entryPtr);
                //Entry termEntry = new Entry(free, 10);
                if (currEntry == null) {
                    //5.1 write the free ptr and data length into dictionary file
                    //System.out.println(entry.getKey());
                    //System.out.println("1 new entry:" + i + "Key :" + entry.getKey());
                    writeEntry(dictionaryFile, termEntry, entryPtr);
                } else if (currEntry.ptr == 0L || currEntry.length == 0) {
                    //5.2 no entry at here
                    //System.out.println(entry.getKey());
                    //System.out.println("2 new entry:" + i + "Key :" + entry.getKey());
                    writeEntry(dictionaryFile, termEntry, entryPtr);
                } else {
                    //flag
                    boolean flag = true;
                    //5.3 loop until null and write the free ptr and data length into dictionary file
                    while (flag) {
                        //System.out.println("3 new entry:" + i + "Key :" + entry.getKey());
                        //update collisions
                        collisions++;
                        //update entryPtr
                        entryPtr += ENTRYSIZE;
                        //return to the beginning of dictionary file
                        if (entryPtr > ENTRYSIZE * TABLESIZE) {
                            entryPtr = entryPtr % TABLESIZE;
                        }
                        Entry newEntry = readEntry(dictionaryFile, entryPtr);
                        if (newEntry.length == -1 || newEntry.length == 0){
                            //update flag
                            flag = false;
                            //System.out.println(entry.getKey());
                            writeEntry(dictionaryFile,termEntry, entryPtr);
                        }
                    }
                }
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }

    public int writeData( RandomAccessFile file, String dataString, long ptr ) {
        try {
            file.seek( ptr );
            byte[] data = dataString.getBytes();
            file.write( data );
            return data.length;
        } catch ( IOException e ) {
            //e.printStackTrace();
            return -1;
        }
    }

    /**
     *  Reads data from the data file
     */
    String readData( RandomAccessFile file, long ptr, int size ) {
        try {
            file.seek(ptr);
            byte[] data = new byte[size];
            file.readFully(data);
            return new String(data);
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }

    //  Reading and writing to the dictionary file.

    /**
     *  Writes an entry to the dictionary hash table file.
     *
     *  @param file  The stream to write
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry(RandomAccessFile file, Entry entry, long ptr ) {
        //
        //  YOUR CODE HERE
        //
        //System.out.println("writeEntry");
        try {
            file.seek(ptr);
            //write ptr into dictionary with Long data type
            file.writeLong(entry.ptr);
            //write length into dictionary with Int data type
            file.writeInt(entry.length);

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
     *  @param file  The stream to read
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry(RandomAccessFile file, long ptr ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        Entry entry = null;
        try {
            file.seek(ptr);
            //int 4 byte long 8 byte
            long dataPtr = file.readLong();
            int dataLength = file.readInt();
            entry = new Entry(dataPtr, dataLength);
        } catch (IOException e) {
            //EOF exception
            //e.printStackTrace();
            return new Entry(-1, -1);
        }
        return entry;
    }

    /**
     *  Merge two dictionary files and two data files
     */
    public void mergeIndex(String suffix1, String suffix2) {
        try {

            //create a stream for the dic1 and dic2 files waiting for merging
//            String dic1Path = INTER_INDEXDIR + "/" + DICTIONARY_FNAME + suffix1;
//            String dic2Path = INTER_INDEXDIR + "/" + DICTIONARY_FNAME + suffix2;
            RandomAccessFile dic1 = new RandomAccessFile(INTER_INDEXDIR + "/" + DICTIONARY_FNAME + suffix1, "rw");
            RandomAccessFile dic2 = new RandomAccessFile(INTER_INDEXDIR + "/" + DICTIONARY_FNAME + suffix2, "rw");
            //create a stream for the data1 and data2 files waiting for merging
//            String data1Path = INTER_INDEXDIR + "/" + DATA_FNAME + suffix1;
//            String data2Path = INTER_INDEXDIR + "/" + DATA_FNAME + suffix2;
            RandomAccessFile data1 = new RandomAccessFile(INTER_INDEXDIR + "/" + DATA_FNAME + suffix1, "rw");
            RandomAccessFile data2 = new RandomAccessFile(INTER_INDEXDIR + "/" + DATA_FNAME + suffix2, "rw");
            //create a stream for the new dic and data files waiting for
            //add new name into queue
            RandomAccessFile newDic = new RandomAccessFile(INTER_INDEXDIR + "/" + DICTIONARY_FNAME + suffix1 + suffix2, "rw");
            RandomAccessFile newData = new RandomAccessFile(INTER_INDEXDIR + "/" + DATA_FNAME + suffix1 + suffix2, "rw");

            //fetch Index from two different dictionary file and data file
            HashMap<String, PostingsList> map1 = readIndex(dic1, data1);
            HashMap<String, PostingsList> map2 = readIndex(dic2, data2);
            System.out.println(suffix1 + " size " + map1.size());
            System.out.println(suffix2 + " size " + map2.size());

            //write index
            long ptr = 0;
            for (Map.Entry<String, PostingsList> entry : map1.entrySet()){
                PostingsList list1 = entry.getValue();
                if (map2.containsKey(entry.getKey())){
                    PostingsList list2 = map2.get(entry.getKey());
                    //speed up
                    if (list1.size() >= list2.size()){
//                        System.out.println("merge size " + list1.size());
//                        for (PostingsEntry postingsEntry : list2.list){
//                            list1.list.add(postingsEntry);
//                        }
                        //System.out.println("size merge" + list1.size());
                        list1.sortMerge(list2);
                        int dataLength = writeMergedEntry(newDic, newData, entry.getKey(),list1,ptr);

                        ptr += dataLength;


                    }else{
//                        for (PostingsEntry postingsEntry : list1.list){
//                            list2.list.add(postingsEntry);
//                        }
                        list2.sortMerge(list1);
                        int dataLength = writeMergedEntry(newDic, newData, entry.getKey(),list2,ptr);

                        ptr += dataLength;
                    }
                    map2.remove(entry.getKey());
                }else{
                    ptr += writeMergedEntry(newDic, newData, entry.getKey(),list1,ptr);
                }
            }

            for(Map.Entry<String, PostingsList> entry : map2.entrySet()) {
                PostingsList list2 = entry.getValue();
                ptr += writeMergedEntry(newDic, newData, entry.getKey(),list2,ptr);
            }

            dic1.close();
            dic2.close();
            data1.close();
            data2.close();
            newDic.close();
            newData.close();
            synchronized(fileQueue) {
                fileQueue.offer(suffix1 + suffix2);
            }

            //delete the file which merged
            removeFile(INTER_INDEXDIR + "/" + DICTIONARY_FNAME + suffix1);
            removeFile(INTER_INDEXDIR + "/" + DICTIONARY_FNAME + suffix2);
            removeFile(INTER_INDEXDIR + "/" + DATA_FNAME + suffix1);
            removeFile(INTER_INDEXDIR + "/" + DATA_FNAME + suffix2);

        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }

    }

    public int writeMergedEntry(RandomAccessFile newDic, RandomAccessFile newData, String token, PostingsList list, long ptr){
        //write data
        String dataString = token + " " + list.toString();
        int dataLength = writeData(newData, dataString, ptr);
        //write dictionary
        Entry termEntry = new Entry(ptr, dataLength);
        long entryPtr = getEntryPtr(token);
        //check if collisions
        Entry currEntry = readEntry(newDic, entryPtr);
        if (currEntry.length == -1){
            //no entry at here
            writeEntry(newDic, termEntry, entryPtr);
        }else if (currEntry.length == 0){
            //no entry at here
            writeEntry(newDic, termEntry, entryPtr);
        }else{
            boolean flag = true;
            while (flag){
                //System.out.println("3 new entry:" + i + "Key :"+ entry.getKey());
                //update entryPtr
                entryPtr += ENTRYSIZE;
                //return to the beginning of dictionary file
                if(entryPtr > ENTRYSIZE * TABLESIZE){
                    entryPtr = entryPtr % TABLESIZE;
                }
                Entry newEntry = readEntry(newDic, entryPtr);
                if (newEntry.length == -1 || newEntry.length == 0){
                    //update flag
                    flag = false;
                    //System.out.println(entry.getKey());
                    writeEntry(newDic, termEntry, entryPtr);
                }
            }
        }

        return dataLength;
    }

    /**
     * remove the file
     * @param path
     */
    public void removeFile(String path) {
        try {
            File file = new File(path);
            file.delete();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  Reads dictionary from the dictionary file
     */
    private HashMap<String, PostingsList> readIndex(RandomAccessFile dictionaryFile, RandomAccessFile dataFile) {
        long ptr = 0;
        HashMap<String, PostingsList> dicMap = new HashMap<>();
        while(true){
            Entry currEntry = readEntry(dictionaryFile,ptr);
//            System.out.println("length" + currEntry.length);
//            System.out.println("ptr" + currEntry.ptr);
            if (currEntry.length == -1)
                break;
            if(currEntry.length != 0){
                String dataString = readData(dataFile, currEntry.ptr, currEntry.length);
                PostingsList curPostinsList = convertPostings(dataString);
                //System.out.println("term:"+ curPostinsList.term);
                dicMap.put(curPostinsList.term, curPostinsList);
            }
            ptr += ENTRYSIZE;
        }
        try {
            dictionaryFile.close();
            dataFile.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
        return dicMap;
    }
}
