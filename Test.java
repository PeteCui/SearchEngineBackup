import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.sound.sampled.SourceDataLine;

public class Test {
    public static void main(String[] args) {
        // ArrayList<Integer> list1 = new ArrayList<>();
        // ArrayList<Integer> list2 = new ArrayList<>();
        // list1.add(1);
        // list1.add(2);
        // list1.add(3);

        // list2.add(1);
        // list2.add(2);

        // int entryNum = Math.min(list1.size(), list2.size());
        // int i = 0;
        // int j = 0;
        // ArrayList<Integer> resultList = new ArrayList<>();
        // while(i < entryNum && j < entryNum){
        //     System.out.println("list1:" + list1.get(i));
        //     System.out.println("list2:" + list2.get(j));
        //     if (list1.get(i) == list2.get(j)){
        //         resultList.add(list1.get(i));
        //         i++;
        //         j++;
        //     }else if (list1.get(i)< list2.get(j)){
        //         i++;
        //     }else if (list1.get(i) > list2.get(j)){
        //         j++;
        //     }
        // }
        // System.out.println(resultList);

        
        // System.out.println("AaAaAa".hashCode());
        // System.out.println("Aacdb".hashCode());
        // System.out.println("BBBrwerwerwer".hashCode());
        // int code = "BBBrwerwerwer".hashCode();
        // System.out.println(~code+1);

        // String a = "zombie";
        // String b = "attack";
        // String c = "math";
        // String d = "1234534534543";
        // System.out.println(a.getBytes().length);
        // System.out.println(b.getBytes().length);
        // System.out.println(c.getBytes().length);
        // System.out.println(d.getBytes().length);

        // long test = 0L;
        // long test2 = 10L;
        // System.out.println(test);
        // System.out.println(test2);

        try {
            RandomAccessFile af = new RandomAccessFile("C:/Users/pppp/IdeaProjects/assignment1/test.txt", "rw");

        
            
            long ptr = 5788884            ;
            af.seek(611953L*12);
            af.write("a".getBytes());
            af.seek(0);
            System.out.println(af.readInt());
            System.out.println(af.readLong());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println("not find!");
        }
        // String token = "a";
        // int raw_value = token.hashCode();
        // if (raw_value < 0){
        //     raw_value = (~raw_value+1);
        //     long entryPtr = (raw_value % 611953L) * 12;
        //     //make it positive
        //     System.out.println(entryPtr);
        // }else{
        //     //return directly if raw_value is already positive
        //     long entryPtr = (raw_value % 611953L) * 12;
        //     System.out.println(entryPtr);
        // }
        long test = 0L;
        if(test == 0){
            System.out.println("yes");
        }
        
    }
}
