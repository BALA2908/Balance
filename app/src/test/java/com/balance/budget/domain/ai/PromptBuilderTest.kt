package com.balance.budget.domain.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The privacy guard. Cloud AI (opt-in) may only ever receive aggregated,
 * anonymized facts — never raw transactions or merchant names. These tests fail
 * loudly if a merchant string ever leaks into any prompt.
 */
class PromptBuilderTest {

    private val s = AiTestData.snapshot()

    @Test fun `no prompt leaks the merchant name`() {
        val prompts = listOf(
            PromptBuilder.analystFacts(s),
            PromptBuilder.advisorFacts(s),
            PromptBuilder.forecastFacts(s),
            PromptBuilder.affordabilityFacts(5_000_00, canAfford = true, poolMinor = 11_600_00),
        )
        prompts.forEach { p ->
            assertFalse("prompt leaked merchant: $p", p.contains(AiTestData.SENTINEL_MERCHANT))
        }
    }

    @Test fun `analyst facts use category names and rupee figures`() {
        val facts = PromptBuilder.analystFacts(s)
        assertTrue(facts.contains("Food"))
        assertTrue(facts.contains("₹"))
        assertTrue(facts.contains("10,000")) // month-to-date, Indian grouping
    }

    @Test fun `affordability facts state the verdict explicitly`() {
        val yes = PromptBuilder.affordabilityFacts(5_000_00, canAfford = true, poolMinor = 11_600_00)
        assertTrue(yes.contains("YES"))
        val no = PromptBuilder.affordabilityFacts(5_000_00, canAfford = false, poolMinor = 3_000_00)
        assertTrue(no.contains("NO"))
    }
}
