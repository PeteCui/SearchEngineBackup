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

        
        System.out.println("AaAaAa".hashCode());
        System.out.println("Aacdb".hashCode());
        System.out.println("BBBrwerwerwer".hashCode());
        int code = "BBBrwerwerwer".hashCode();
        System.out.println(~code+1);
        
        

    }
}
