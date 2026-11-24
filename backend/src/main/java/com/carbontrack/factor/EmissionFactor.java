package com.carbontrack.factor;

import com.carbontrack.common.Category;
import com.carbontrack.common.Scope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Reference data, loaded by the V2 migration. Read only through the API. */
@Entity
@Table(name = "emission_factors")
@Getter
@Setter
@NoArgsConstructor
public class EmissionFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Scope scope;

    @Column(nullable = false)
    private String unit;

    @Column(name = "value_kg_co2e_per_unit", nullable = false, precision = 14, scale = 6)
    private BigDecimal valueKgCo2ePerUnit;

    private String source;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private Integer year;
}
