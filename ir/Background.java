package ir;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Background extends Thread{

    public static final String INTER_INDEXDIR = "./inter_index";
    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The queue for intermediate index file. */
    LinkedList<String> fileQueue;

    PersistentScalableHashedIndex index;

    public boolean indexing = true;

    /** for merger thread */
    static ExecutorService executorService = Executors.newFixedThreadPool(1);

    Future future;

    public Background(){
    }

    public Background(LinkedList<String> fileQueue, PersistentScalableHashedIndex persistentScalableHashedIndex) {
        this.fileQueue = fileQueue;
        this.index = persistentScalableHashedIndex;

    }

    @Override
    public void run() {
        System.out.println("Background thread is running!!!");
        while(indexing){
            synchronized (fileQueue) {
                //System.out.println("queue size" + fileQueue.size());
                if (fileQueue.size() >= 2) {
                    String suffix1 = "";
                    String suffix2 = "";
                    synchronized (fileQueue) {
                        suffix1 = fileQueue.poll();
                        suffix2 = fileQueue.poll();
                        //start a thread to merge
                        Merger merger = new Merger(index, suffix1, suffix2);
                        future = executorService.submit(merger);
                    }
                }
            }
        }
        boolean waitThread = future.isDone();
        while(!waitThread){
            //the last merge
            synchronized (fileQueue){
                while(fileQueue.size() >= 2){
                    String suffix1 = "";
                    String suffix2 = "";
                    synchronized (fileQueue){
                        suffix1 = fileQueue.poll();
                        suffix2 = fileQueue.poll();
                        //start a thread to merge
                        Merger merger = new Merger(index, suffix1, suffix2);
                        future = executorService.submit(merger);
                    }
                }
                waitThread = future.isDone();
                //System.out.println(waitThread);
            }
        }
        executorService.shutdown();
        index.renameFile();
        System.out.println("backend thread terminate");
    }


    public void callStop(){
        indexing = false;
    }
}
