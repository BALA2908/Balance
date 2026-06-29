package com.balance.budget.feature.quickadd

import com.balance.budget.data.categorize.Categorizer
import com.balance.budget.data.categorize.LabelCountStore
import com.balance.budget.data.local.entity.CategoryEntity
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.fakes.FakeCategoryDao
import com.balance.budget.fakes.FakeExpenseDao
import com.balance.budget.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val fixedNow = 1_700_000_000_000L

    private fun buildViewModel(): Pair<QuickAddViewModel, FakeExpenseDao> {
        val categoryDao = FakeCategoryDao(
            listOf(
                CategoryEntity(id = 1, name = "Food", iconKey = "food", colorHex = "#E0795B", isDefault = true),
                CategoryEntity(id = 2, name = "Travel", iconKey = "travel", colorHex = "#6FA8A0", isDefault = true),
            )
        )
        val expenseDao = FakeExpenseDao(categoryDao)
        val vm = QuickAddViewModel(
            expenseRepository = ExpenseRepository(expenseDao, clock = { fixedNow }),
            categoryRepository = CategoryRepository(categoryDao),
            categorizer = Categorizer(InMemoryLabelCountStore()),
            clock = { fixedNow },
        )
        return vm to expenseDao
    }

    @Test
    fun `number pad builds the amount in paise`() = runTest {
        val (vm, _) = buildViewModel()
        vm.onDigit(1); vm.onDigit(5); vm.onDigit(0) // 150
        assertEquals(150_00L, vm.state.value.amountMinor)

        vm.onDecimal(); vm.onDigit(2); vm.onDigit(5) // 150.25
        assertEquals(150_25L, vm.state.value.amountMinor)
    }

    @Test
    fun `backspace removes the last character`() = runTest {
        val (vm, _) = buildViewModel()
        vm.onDigit(9); vm.onDigit(9)
        vm.onBackspace()
        assertEquals(9_00L, vm.state.value.amountMinor)
    }

    @Test
    fun `cannot save without an amount and a category`() = runTest {
        val (vm, _) = buildViewModel()
        assertFalse(vm.state.value.canSave)

        vm.onDigit(5); vm.onDigit(0)
        assertFalse("amount only, no category", vm.state.value.canSave)

        vm.selectCategory(1)
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun `save writes one expense through the single save path`() = runTest {
        val (vm, expenseDao) = buildViewModel()
        advanceUntilIdle() // let categories load

        vm.onDigit(2); vm.onDigit(5); vm.onDigit(0) // 250
        vm.selectCategory(2)
        vm.setNote("  Auto rickshaw  ")
        vm.save()
        advanceUntilIdle()

        assertEquals(1, expenseDao.inserted.size)
        val saved = expenseDao.inserted.first()
        assertEquals(250_00L, saved.amountMinor)
        assertEquals(2L, saved.categoryId)
        assertEquals("Auto rickshaw", saved.note) // trimmed
        assertEquals(fixedNow, saved.timestamp)
        assertTrue(vm.state.value.saved)
    }
}

/** In-memory categorizer store so the JVM test needs no Android Context/DataStore. */
private class InMemoryLabelCountStore : LabelCountStore {
    private val data = mutableMapOf<String, MutableMap<Long, Int>>()
    override suspend fun counts(normalizedLabel: String): Map<Long, Int> =
        data[normalizedLabel] ?: emptyMap()

    override suspend fun increment(normalizedLabel: String, categoryId: Long) {
        val m = data.getOrPut(normalizedLabel) { mutableMapOf() }
        m[categoryId] = (m[categoryId] ?: 0) + 1
    }
}
