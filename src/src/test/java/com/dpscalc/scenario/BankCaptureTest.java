package com.dpscalc.scenario;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankCaptureTest {
    @Test public void normalizesNotesWithoutInventingPlaceholderOwnershipOrChargedVariants(){
        Map<Integer,Integer> items=new TreeMap<>();
        BankCapture.addItem(items,4152,3,false,true,4151);
        BankCapture.addItem(items,4151,1,false,false,4152);
        BankCapture.addItem(items,14000,1,true,false,4151);
        BankCapture.addItem(items,1127,0,false,false,-1);
        BankCapture.addItem(items,11908,1,false,false,-1);
        assertEquals(Map.of(4151,4,11908,1),items);
        BankCapture.addItem(items,4151,Integer.MAX_VALUE,false,false,-1);
        assertEquals(Integer.valueOf(Integer.MAX_VALUE),items.get(4151));
    }
}
