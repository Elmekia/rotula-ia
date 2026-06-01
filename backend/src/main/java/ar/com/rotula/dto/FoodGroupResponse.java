package ar.com.rotula.dto;

import ar.com.rotula.domain.FoodGroup;

import java.util.UUID;

public record FoodGroupResponse(
        UUID   id,
        String code,
        String name,
        int    sortOrder
) {
    public static FoodGroupResponse from(FoodGroup g) {
        return new FoodGroupResponse(g.getId(), g.getCode(), g.getName(), g.getSortOrder());
    }
}
