/* Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.lobby;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class HazardMapRegistry {
    private static final Map<String, MapDefinition> MAPS;

    static {
        Map<String, MapDefinition> map = new LinkedHashMap<>();
        map.put("zero_dam", new MapDefinition("zero_dam", "零號大壩", 0, 15, 18));
        map.put("longbow_valley", new MapDefinition("longbow_valley", "長弓溪谷", 30000, 18, 24));
        map.put("space_base_confidential", new MapDefinition("space_base_confidential", "航天基地(機密)", 187500, 16, 19));
        map.put("space_base_top_secret", new MapDefinition("space_base_top_secret", "航天基地(絕密)", 600000, 16, 18));
        MAPS = Collections.unmodifiableMap(map);
    }

    public static MapDefinition getMap(String mapId) {
        if (mapId == null || mapId.isBlank()) {
            return MAPS.get("zero_dam");
        }
        return MAPS.getOrDefault(mapId, MAPS.get("zero_dam"));
    }

    public static Map<String, MapDefinition> getAllMaps() {
        return MAPS;
    }
}
