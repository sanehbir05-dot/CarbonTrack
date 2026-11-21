package com.carbontrack.incentive;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncentiveRuleRepository extends JpaRepository<IncentiveRule, Long> {

    List<IncentiveRule> findAllByOrderByMinReductionPctAsc();
}
