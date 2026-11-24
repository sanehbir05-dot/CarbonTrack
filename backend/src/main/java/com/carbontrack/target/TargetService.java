package com.carbontrack.target;

import com.carbontrack.common.BadRequestException;
import com.carbontrack.common.NotFoundException;
import com.carbontrack.dashboard.DashboardService;
import com.carbontrack.target.dto.TargetDto;
import com.carbontrack.target.dto.TargetRequest;
import java.math.BigDecimal;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every method takes the caller's companyId. It never comes from the request. */
@Service
public class TargetService {

    private final TargetRepository targets;
    private final DashboardService dashboard;

    public TargetService(TargetRepository targets, DashboardService dashboard) {
        this.targets = targets;
        this.dashboard = dashboard;
    }

    @Transactional(readOnly = true)
    public List<TargetDto> list(Long companyId) {
        int currentYear = DashboardService.defaultYear();
        BigDecimal currentKg = dashboard.yearTotal(companyId, currentYear);
        Map<Integer, BigDecimal> baselineTotals = new HashMap<>();
        return targets.findByCompanyIdOrderByIdAsc(companyId).stream()
                .map(t -> {
                    BigDecimal baselineKg = baselineTotals.computeIfAbsent(t.getBaselineYear(),
                            y -> dashboard.yearTotal(companyId, y));
                    return TargetDto.of(t, currentYear, TargetCalculator.evaluate(t.getBaselineYear(),
                            t.getTargetYear(), t.getReductionPct(), baselineKg, currentKg, currentYear));
                })
                .toList();
    }

    @Transactional
    public TargetDto create(Long companyId, TargetRequest req) {
        if (req.targetYear() <= req.baselineYear()) {
            throw new BadRequestException("targetYear must be after baselineYear");
        }
        if (req.baselineYear() > Year.now().getValue()) {
            throw new BadRequestException("baselineYear cannot be in the future");
        }
        Target t = new Target();
        t.setCompanyId(companyId);
        t.setName(req.name().trim());
        t.setBaselineYear(req.baselineYear());
        t.setTargetYear(req.targetYear());
        t.setReductionPct(req.reductionPct());
        targets.save(t);

        int currentYear = DashboardService.defaultYear();
        return TargetDto.of(t, currentYear, TargetCalculator.evaluate(t.getBaselineYear(), t.getTargetYear(),
                t.getReductionPct(), dashboard.yearTotal(companyId, t.getBaselineYear()),
                dashboard.yearTotal(companyId, currentYear), currentYear));
    }

    @Transactional
    public void delete(Long companyId, Long id) {
        Target t = targets.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Target not found"));
        targets.delete(t);
    }
}
