package ir;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

import static ir.Background.DATA_FNAME;
import static ir.Background.DICTIONARY_FNAME;
import static ir.Background.INTER_INDEXDIR;

public class Merger extends Thread{


    PersistentScalableHashedIndex index;
    String suffix1;
    String suffix2;
    int count;


    public Merger(PersistentScalableHashedIndex index, String suffix1, String suffix2) {
        this.index = index;
        this.suffix1 = suffix1;
        this.suffix2 = suffix2;
    }

    public void run(){
        index.mergeIndex(suffix1, suffix2);
    }
}
