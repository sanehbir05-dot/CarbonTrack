package com.carbontrack.incentive;

import com.carbontrack.common.Tier;
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

/** Reference data loaded by the V2 migration. The scheme is simulated, not a real government scheme. */
@Entity
@Table(name = "incentive_rules")
@Getter
@Setter
@NoArgsConstructor
public class IncentiveRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Tier tier;

    @Column(nullable = false)
    private String name;

    @Column(name = "min_reduction_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal minReductionPct;

    @Column(name = "credit_points", nullable = false)
    private Integer creditPoints;

    private String benefit;
}
