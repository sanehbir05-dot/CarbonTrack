package com.carbontrack.incentive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.carbontrack.common.Tier;
import com.carbontrack.incentive.IncentiveCalculator.Result;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class IncentiveCalculatorTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static IncentiveRule rule(Tier tier, String min) {
        IncentiveRule r = new IncentiveRule();
        r.setTier(tier);
        r.setMinReductionPct(d(min));
        r.setCreditPoints(100);
        return r;
    }

    private final List<IncentiveRule> rules = List.of(
            rule(Tier.BRONZE, "5.00"), rule(Tier.SILVER, "10.00"), rule(Tier.GOLD, "20.00"));

    private Tier tierAt(String reduction) {
        IncentiveRule a = IncentiveCalculator.evaluate(rules, d(reduction)).achieved();
        return a == null ? Tier.NONE : a.getTier();
    }

    @Test
    void picksTheHighestTierWhoseMinimumIsMet() {
        assertEquals(Tier.NONE, tierAt("0"));
        assertEquals(Tier.NONE, tierAt("4.99"));
        assertEquals(Tier.BRONZE, tierAt("5"));
        assertEquals(Tier.BRONZE, tierAt("9.99"));
        assertEquals(Tier.SILVER, tierAt("10"));
        assertEquals(Tier.SILVER, tierAt("12"));
        assertEquals(Tier.GOLD, tierAt("20"));
        assertEquals(Tier.GOLD, tierAt("55"));
    }

    @Test
    void negativeReductionIsNone() {
        assertEquals(Tier.NONE, tierAt("-3"));
    }

    @Test
    void nextTierIsTheLowestUnmetOne() {
        Result r = IncentiveCalculator.evaluate(rules, d("12"));

        assertEquals(Tier.SILVER, r.achieved().getTier());
        assertEquals(Tier.GOLD, r.next().getTier());
    }

    @Test
    void nextTierIsBronzeWhenNothingIsAchieved() {
        Result r = IncentiveCalculator.evaluate(rules, d("0"));

        assertNull(r.achieved());
        assertEquals(Tier.BRONZE, r.next().getTier());
    }

    @Test
    void nextTierIsNullAtTheTopTier() {
        assertNull(IncentiveCalculator.evaluate(rules, d("20")).next());
        assertNull(IncentiveCalculator.evaluate(rules, d("40")).next());
    }

    @Test
    void noRulesMeansNoTierAndNoNext() {
        Result r = IncentiveCalculator.evaluate(List.of(), d("50"));

        assertNull(r.achieved());
        assertNull(r.next());
    }

    @Test
    void gapIsMinusReductionFlooredAtZero() {
        IncentiveRule gold = rule(Tier.GOLD, "20.00");

        assertEquals(d("8.00"), IncentiveCalculator.gapPct(gold, d("12")));
        assertEquals(d("0.00"), IncentiveCalculator.gapPct(gold, d("25")));
        assertEquals(d("23.00"), IncentiveCalculator.gapPct(gold, d("-3")));
    }

    @Test
    void reductionPctUsesBaselineAndCurrent() {
        assertEquals(0, d("12").compareTo(IncentiveCalculator.reductionPct(d("1000"), d("880"))));
        assertEquals(0, d("-10").compareTo(IncentiveCalculator.reductionPct(d("1000"), d("1100"))));
    }

    @Test
    void reductionPctIsZeroWithoutBaselineOrCurrentData() {
        assertEquals(0, IncentiveCalculator.reductionPct(BigDecimal.ZERO, d("500")).signum());
        assertEquals(0, IncentiveCalculator.reductionPct(d("1000"), BigDecimal.ZERO).signum());
    }

    @Test
    void tierDecisionUsesTheExactValueNotTheRoundedOne() {
        // 9.996% would display as 10.00 but has not reached SILVER.
        BigDecimal exact = IncentiveCalculator.reductionPct(d("100000"), d("90004"));

        assertEquals(Tier.BRONZE, IncentiveCalculator.evaluate(rules, exact).achieved().getTier());
    }
}
