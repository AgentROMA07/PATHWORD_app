package com.pathword.taxi.presentation.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pathword.taxi.domain.model.Location
import com.pathword.taxi.domain.model.Order
import com.pathword.taxi.map.IMapProvider
import com.pathword.taxi.map.MapLocation
import com.pathword.taxi.map.MapMarker
import com.pathword.taxi.presentation.viewmodel.DriverState
import com.pathword.taxi.presentation.viewmodel.DriverViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class MockMapProvider : IMapProvider {
    @Composable
    override fun MapView(
        modifier: Modifier,
        initialLocation: MapLocation,
        markers: List<MapMarker>,
        onMapClick: ((MapLocation) -> Unit)?
    ) {
        // Empty mock
    }
}

@RunWith(AndroidJUnit4::class)
class CounterOfferTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun violentlyClickCounterOfferButton_doesNotCrash() {
        val mockViewModel = mock(DriverViewModel::class.java)

        val stateFlow = MutableStateFlow(
            DriverState(
                incomingOrders = listOf(
                    Order("1", 1L, Location(0.0, 0.0), "1000 KZT", "pending")
                )
            )
        )

        `when`(mockViewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            DriverScreen(
                mapProvider = MockMapProvider(),
                onBack = {},
                viewModel = mockViewModel
            )
        }

        // Violently click the counter-offer button to simulate spamming
        for (i in 0..50) {
            composeTestRule.onNodeWithText("Counter-offer").performClick()
        }

        // If we reach here without the UI freezing or crashing, test passes
    }

    @Test
    fun quicklySwitchingOnlineStatus_doesNotFreezeUI() {
        val mockViewModel = mock(DriverViewModel::class.java)

        val stateFlow = MutableStateFlow(
            DriverState(isOnline = false)
        )

        `when`(mockViewModel.state).thenReturn(stateFlow)

        composeTestRule.setContent {
            DriverScreen(
                mapProvider = MockMapProvider(),
                onBack = {},
                viewModel = mockViewModel
            )
        }

        // Violently click the online switch to simulate spamming
        for (i in 0..20) {
            composeTestRule.onNodeWithText("Offline").performClick()
        }
    }
}
