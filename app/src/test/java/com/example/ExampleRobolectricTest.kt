package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.SignalShieldDatabase
import com.example.data.ThreatLogRepository
import com.example.data.entity.SystemEventEntity
import com.example.data.entity.ThreatLogEntity
import com.example.model.*
import com.example.ui.components.StreamAnomalyThreatCard
import com.example.ui.screens.ThreatLogScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var db: SignalShieldDatabase
  private lateinit var repository: ThreatLogRepository

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, SignalShieldDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = ThreatLogRepository(db.threatLogDao(), db.systemEventDao())
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Unified Signal Shield", appName)
  }

  @Test
  fun `threat card renders high category and mitigation recommendations`() {
    val highThreatReport = StreamAnomalyReport(
      threatCategory = InterferenceCategory.HIGH,
      threatCategoryText = "High",
      interferenceType = "Broadband Co-Channel RF Jamming",
      confidenceScore = 0.96f,
      summary = "High-power jamming pulse detected.",
      detailedAnalysis = "20 MHz carrier saturation at 2.4 GHz.",
      mitigationRecommendation = "Execute fast frequency-hopping spread spectrum.",
      affectedBand = "2.4 GHz"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        StreamAnomalyThreatCard(
          report = highThreatReport,
          connectionStatus = ConnectionStatus.CONNECTED,
          activeServerUrl = "ws://10.0.2.2:8765",
          latencyMs = 12L,
          autoScanEnabled = true,
          onTriggerGeminiScan = {},
          onToggleAutoScan = {},
          onMitigateThreat = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("stream_anomaly_threat_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("threat_category_badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("HIGH").assertIsDisplayed()
  }

  @Test
  fun `threat categories conform to High Medium Low specifications`() {
    val high = StreamAnomalyReport(threatCategory = InterferenceCategory.HIGH, threatCategoryText = "High")
    val medium = StreamAnomalyReport(threatCategory = InterferenceCategory.MEDIUM, threatCategoryText = "Medium")
    val low = StreamAnomalyReport(threatCategory = InterferenceCategory.LOW, threatCategoryText = "Low")

    assertEquals("High", high.threatCategoryText)
    assertEquals("Medium", medium.threatCategoryText)
    assertEquals("Low", low.threatCategoryText)
  }

  @Test
  fun `room database stores and retrieves threat log history`() = runBlocking {
    val sampleEvent = DetectionEvent(
      id = "EVT-TEST-101",
      timestamp = 1700000000000L,
      source = SignalSource.RF,
      band = "2.4 GHz ISM",
      powerDbm = -45.0,
      thresholdDbm = -60.0,
      threatType = "WIDEBAND CHIRP JAMMING",
      severity = EventSeverity.CRITICAL,
      details = "High energy spectral saturation detected.",
      actionTaken = "NOTCH_FILTER_ENGAGED"
    )

    val rowId = repository.recordThreat(sampleEvent, notes = "Verified by operator")
    assertTrue(rowId > 0)

    val allThreats = repository.allThreats.first()
    assertEquals(1, allThreats.size)
    assertEquals("EVT-TEST-101", allThreats[0].eventId)
    assertEquals("CRITICAL", allThreats[0].severity)
    assertEquals("RF", allThreats[0].source)
    assertEquals("WIDEBAND CHIRP JAMMING", allThreats[0].threatType)

    // Test system event logging
    repository.recordSystemEvent(
      category = "CONNECTION",
      title = "GNU Radio Flowgraph Connected",
      message = "WebSocket stream initialized on ws://10.0.2.2:8765",
      severity = "SUCCESS",
      targetEndpoint = "ws://10.0.2.2:8765"
    )

    val allSysEvents = repository.allSystemEvents.first()
    assertEquals(1, allSysEvents.size)
    assertEquals("CONNECTION", allSysEvents[0].category)
    assertEquals("GNU Radio Flowgraph Connected", allSysEvents[0].title)
  }

  @Test
  fun `threat log screen displays persisted room database entities and filters`() {
    val sampleThreats = listOf(
      ThreatLogEntity(
        id = 1L,
        eventId = "EVT-ROOM-001",
        timestamp = System.currentTimeMillis(),
        source = "FSO",
        band = "1550 nm C-Band",
        powerDbm = -12.0,
        thresholdDbm = -20.0,
        threatType = "DIRECT LASER BLINDING",
        severity = "CRITICAL",
        details = "Continuous wave laser burst detected on QPD receiver.",
        actionTaken = "AUTO_BLOCKED"
      )
    )

    val sampleSystemEvents = listOf(
      SystemEventEntity(
        id = 1L,
        timestamp = System.currentTimeMillis(),
        category = "CONNECTION",
        title = "GNU Radio Bridge Active",
        message = "Connected to flowgraph sink at ws://10.0.2.2:8765",
        severity = "SUCCESS"
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ThreatLogScreen(
          persistedThreats = sampleThreats,
          persistedSystemEvents = sampleSystemEvents,
          searchQuery = "",
          selectedFilter = "ALL",
          activeTab = 0,
          onSearchQueryChanged = {},
          onFilterChanged = {},
          onTabChanged = {},
          onDeleteThreat = {},
          onClearAllThreats = {},
          onDeleteSystemEvent = {},
          onClearAllSystemEvents = {},
          onExportJson = { "{}" }
        )
      }
    }

    composeTestRule.onNodeWithText("MONITORING LOG & AUDIT").assertIsDisplayed()
    composeTestRule.onNodeWithTag("threat_log_item_1").assertIsDisplayed()
    composeTestRule.onNodeWithText("DIRECT LASER BLINDING").assertIsDisplayed()
  }

  @Test
  fun `websocket manager initializes with simulation state and default pipelines`() {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    val wsManager = com.example.network.WebSocketManager(scope)

    assertEquals(ConnectionStatus.SIMULATION, wsManager.connectionStatus.value)
    assertEquals(0L, wsManager.latencyMs.value)
    assertEquals(0, wsManager.reconnectAttempt.value)
    assertEquals(false, wsManager.isReconnecting.value)
  }

  @Test
  fun `websocket manager manual disconnect stops reconnect engine`() {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    val wsManager = com.example.network.WebSocketManager(scope)

    wsManager.disconnect()
    assertEquals(ConnectionStatus.DISCONNECTED, wsManager.connectionStatus.value)
    assertEquals(false, wsManager.isReconnecting.value)
  }
}
