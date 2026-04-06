package com.tcc.androidnative.feature.services.ui

import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.feature.services.data.DeleteServiceOutcome
import com.tcc.androidnative.feature.services.data.ServiceModel
import com.tcc.androidnative.feature.services.data.ServicesPage
import com.tcc.androidnative.feature.services.data.ServicesRepository
import com.tcc.androidnative.testutil.MainDispatcherRule
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServicesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submit form should require service description`() = runTest {
        val viewModel = ServicesViewModel(FakeServicesRepository())
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.submitForm()
        runCurrent()

        val message = viewModel.uiState.value.transientMessage
        assertEquals(R.string.feedback_service_description_required, message?.textResId)
        assertEquals(MessageTone.ERROR, message?.tone)
        assertEquals(MessageDurations.SHORT_3S, message?.durationMillis)
    }

    @Test
    fun `submit form should require positive value`() = runTest {
        val viewModel = ServicesViewModel(FakeServicesRepository())
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.updateForm(description = "Design", valueInput = "0")
        viewModel.submitForm()
        runCurrent()

        val message = viewModel.uiState.value.transientMessage
        assertEquals(R.string.feedback_service_value_positive_required, message?.textResId)
        assertEquals(MessageTone.ERROR, message?.tone)
    }

    @Test
    fun `single delete with linked appointments should show warning message`() = runTest {
        val viewModel = ServicesViewModel(
            FakeServicesRepository(
                deleteOutcome = DeleteServiceOutcome.HAS_LINK
            )
        )
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.deleteSelection()
        runCurrent()

        val message = viewModel.uiState.value.transientMessage
        assertEquals(R.string.feedback_service_delete_link_warning, message?.textResId)
        assertEquals(MessageTone.WARNING, message?.tone)
    }
}

private class FakeServicesRepository(
    private val deleteOutcome: DeleteServiceOutcome = DeleteServiceOutcome.DELETED
) : ServicesRepository {
    override suspend fun list(description: String?, uiPageIndex: Int, pageSize: Int): ServicesPage {
        return ServicesPage(
            items = listOf(ServiceModel(id = 1L, description = "Design", value = BigDecimal("50.00"))),
            pageIndex = 1,
            totalPages = 1,
            totalItems = 1
        )
    }

    override suspend fun getById(id: Long): ServiceModel {
        return ServiceModel(id = id, description = "Design", value = BigDecimal("50.00"))
    }

    override suspend fun create(description: String, valueInput: String): ServiceModel {
        return ServiceModel(id = 99L, description = description, value = BigDecimal(valueInput))
    }

    override suspend fun update(id: Long, description: String, valueInput: String): ServiceModel {
        return ServiceModel(id = id, description = description, value = BigDecimal(valueInput))
    }

    override suspend fun delete(id: Long): DeleteServiceOutcome = deleteOutcome

    override suspend fun bulkDelete(ids: List<Long>): Pair<Int, Int> = 0 to 0
}
