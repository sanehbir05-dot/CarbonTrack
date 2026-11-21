package com.carbontrack.target;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Only the inputs are stored. Progress (kg values, achievedPct, status) is computed on every read. */
@Entity
@Table(name = "targets")
@Getter
@Setter
@NoArgsConstructor
public class Target {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private Long companyId;

    @Column(nullable = false)
    private String name;

    @Column(name = "baseline_year", nullable = false)
    private Integer baselineYear;

    @Column(name = "target_year", nullable = false)
    private Integer targetYear;

    @Column(name = "reduction_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal reductionPct;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
