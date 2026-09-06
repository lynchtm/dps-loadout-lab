package com.dpscalc.scenario;

import com.dpscalc.equipment.EquipmentPreparationFacade;
import java.util.*;

/** Only verified catalog aliases with cosmetic names inherit an equip-level requirement. */
public final class VariantRequirements {
    private VariantRequirements() {}
    public static boolean cosmetic(String actual, String base) {
        if(actual==null||base==null)return false;
        String a=actual.toLowerCase(Locale.ROOT).trim(),b=base.toLowerCase(Locale.ROOT).trim();
        a=a.replaceFirst("^echo ", "").replaceFirst(" \\((?:or|g|t|h[1-5]|deadman|l)\\)$", "");
        return a.equals(b);
    }
    public static Map<String,Integer> resolve(int id, String actualName, EquipmentPreparationFacade equipment,
                                             Map<Integer,Map<String,Integer>> requirements) {
        Map<String,Integer> exact=requirements.get(id);if(exact!=null)return exact;
        int base=equipment.canonicalId(id);
        if(base==id)return null;
        try {return cosmetic(actualName,equipment.getItemFacts(base).getName())?requirements.get(base):null;}
        catch(IllegalArgumentException ex){return null;}
    }
}
