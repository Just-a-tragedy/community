package com.nowcoder.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SortTest {
    @Test
    public void sortShell(){
        int[] array=new int[]{9,7,5,8,1,4,2,3,0,6};
        if(array.length>0){
            int len=array.length;
            int gap=len/2;
            while(gap>0){
                for (int i=gap; i<array.length; i++){
                    int temp=array[i];
                    int index=i-gap;
                    while (index>=0 && array[index]>temp){
                        array[index+gap]=array[index];
                        index-=gap;
                    }
                    array[index+gap]=temp;
                }
                gap/=2;
            }
        }
        for(int i=0; i<array.length ;i++){
            System.out.println(array[i]);
        }

    }
}
