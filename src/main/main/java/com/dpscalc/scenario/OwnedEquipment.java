package com.dpscalc.scenario;

import java.util.*;

/** One immutable view of the player's observed containers. No client objects cross threads. */
public final class OwnedEquipment {
    public final String profile;
    public final long revision, bankScannedAt;
    public final Map<Integer, Integer> bank, inventory, equipped, quantities;
    public final Map<String, Integer> levels;
    public final Map<Integer, String> names;
    public OwnedEquipment(String profile,long revision,long scanned,Map<Integer,Integer> bank,
                          Map<Integer,Integer> inventory,Map<Integer,Integer> equipped,Map<String,Integer> levels) {
        this(profile,revision,scanned,bank,inventory,equipped,levels,Map.of());
    }
    public OwnedEquipment(String profile,long revision,long scanned,Map<Integer,Integer> bank,
                          Map<Integer,Integer> inventory,Map<Integer,Integer> equipped,Map<String,Integer> levels,Map<Integer,String> names) {
        this.profile=profile;this.revision=revision;this.bankScannedAt=scanned;
        this.bank=immutable(bank);this.inventory=immutable(inventory);this.equipped=immutable(equipped);
        this.levels=Collections.unmodifiableMap(new TreeMap<>(levels));
        this.names=Collections.unmodifiableMap(new TreeMap<>(names));
        Map<Integer,Integer> total=new TreeMap<>();
        for(Map<Integer,Integer> source:Arrays.asList(bank,inventory,equipped))source.forEach((id,quantity)->{
            if(id>0&&quantity>0)total.merge(id,quantity,(a,b)->(int)Math.min(Integer.MAX_VALUE,(long)a+b));
        });
        quantities=immutable(total);
    }
    private static Map<Integer,Integer> immutable(Map<Integer,Integer> source){return Collections.unmodifiableMap(new TreeMap<>(source));}
    public static OwnedEquipment empty(long revision){return new OwnedEquipment(null,revision,0,Map.of(),Map.of(),Map.of(),Map.of());}
    public boolean ready(String expectedProfile){return bankScannedAt>0&&profile!=null&&profile.equals(expectedProfile);}
    public boolean owns(int id){return id<=0||quantities.getOrDefault(id,0)>0;}
}
