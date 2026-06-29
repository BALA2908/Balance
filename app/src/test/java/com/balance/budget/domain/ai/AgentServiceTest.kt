package com.balance.budget.domain.ai

import com.balance.budget.fakes.FakeAiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgentServiceTest {

    private fun service(provider: FakeAiProvider) = AgentService(provider, Dispatchers.Unconfined)

    @Test fun `analyst falls back to deterministic text when AI unavailable`() = runTest {
        val s = AiTestData.snapshot()
        val result = service(FakeAiProvider(available = false)).analystSummary(s)
        assertEquals(AiTextSource.DETERMINISTIC, result.source)
        assertTrue(result.text.contains("10,000")) // real figures present
        assertTrue(result.text.contains("₹"))
    }

    @Test fun `analyst uses AI text when available`() = runTest {
        val s = AiTestData.snapshot()
        val provider = FakeAiProvider(available = true, summarizeResult = "A warm month recap.")
        val result = service(provider).analystSummary(s)
        assertEquals(AiTextSource.ON_DEVICE, result.source)
        assertEquals("A warm month recap.", result.text)
    }

    @Test fun `provider failure still yields a deterministic fallback`() = runTest {
        val s = AiTestData.snapshot()
        val provider = FakeAiProvider(available = true, throwOnCall = true)
        val result = service(provider).analystSummary(s)
        assertEquals(AiTextSource.DETERMINISTIC, result.source)
        assertTrue(result.text.isNotBlank())
    }

    @Test fun `advisor always yields three tips`() = runTest {
        val s = AiTestData.snapshot()
        val result = service(FakeAiProvider(available = false)).advisorTips(s)
        assertEquals(3, result.text.lines().count { it.isNotBlank() })
    }

    @Test fun `affordability verdict is YES when within the pool`() = runTest {
        val s = AiTestData.snapshot(poolMinor = 11_600_00)
        val result = service(FakeAiProvider(available = false)).answerQuestion(s, "can I afford 5000 this month?")
        assertEquals(AiTextSource.DETERMINISTIC, result.source)
        assertTrue(result.text.contains("Yes"))
        assertTrue(result.text.contains("5,000"))
    }

    @Test fun `affordability verdict is NO when over the pool`() = runTest {
        val s = AiTestData.snapshot(poolMinor = 3_000_00)
        val result = service(FakeAiProvider(available = false)).answerQuestion(s, "can I afford ₹5,000?")
        assertTrue(result.text.contains("stretch") || result.text.contains("over"))
    }

    @Test fun `question with no amount returns a gentle prompt`() = runTest {
        val s = AiTestData.snapshot()
        val result = service(FakeAiProvider(available = false)).answerQuestion(s, "how am I doing?")
        assertEquals(AiTextSource.DETERMINISTIC, result.source)
        assertTrue(result.text.isNotBlank())
    }
}
