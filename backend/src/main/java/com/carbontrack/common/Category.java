package com.carbontrack.common;

/** Each category has a fixed unit and scope (shared contract A6). The client never sends them. */
public enum Category {
    ELECTRICITY("kWh", Scope.SCOPE_2),
    DIESEL("litre", Scope.SCOPE_1),
    PETROL("litre", Scope.SCOPE_1),
    NATURAL_GAS("m3", Scope.SCOPE_1),
    LPG("kg", Scope.SCOPE_1),
    BUSINESS_TRAVEL_AIR("passenger_km", Scope.SCOPE_3),
    BUSINESS_TRAVEL_ROAD("km", Scope.SCOPE_3),
    FREIGHT_ROAD("tonne_km", Scope.SCOPE_3),
    WASTE_LANDFILL("tonne", Scope.SCOPE_3);

    private final String unit;
    private final Scope scope;

    Category(String unit, Scope scope) {
        this.unit = unit;
        this.scope = scope;
    }

    public String getUnit() { return unit; }

    public Scope getScope() { return scope; }
}
