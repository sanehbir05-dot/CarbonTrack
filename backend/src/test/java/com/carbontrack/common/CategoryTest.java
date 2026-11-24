package com.carbontrack.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void unitsAndScopesMatchContract() {
        assertEquals(9, Category.values().length);
        assertEquals("kWh", Category.ELECTRICITY.getUnit());
        assertEquals(Scope.SCOPE_2, Category.ELECTRICITY.getScope());
        assertEquals(Scope.SCOPE_1, Category.DIESEL.getScope());
        assertEquals("passenger_km", Category.BUSINESS_TRAVEL_AIR.getUnit());
        assertEquals(Scope.SCOPE_3, Category.WASTE_LANDFILL.getScope());
    }
}
