/* Copyright (c) 2026 ray20123315. All Rights Reserved.
 * This file is part of "Delta Ops: Hazard Zone".
 * Proprietary and confidential.
 */
package com.deltaops.lobby;

public record MapDefinition(String mapId, String displayName, int minGearValue, int minPlayers, int maxPlayers) {
    public MapDefinition(String mapId, String displayName, int minGearValue) {
        this(mapId, displayName, minGearValue, 1, 4);
    }
}
