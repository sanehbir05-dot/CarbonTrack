package com.carbontrack.action;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReductionActionRepository extends JpaRepository<ReductionAction, Long> {

    /** Newest first. The facility is optional, so it is a left join. */
    @Query("select a from ReductionAction a left join fetch a.facility where a.companyId = :companyId order by a.id desc")
    List<ReductionAction> findAllForCompany(@Param("companyId") Long companyId);
}
