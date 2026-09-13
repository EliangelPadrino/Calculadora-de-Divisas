package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CalcToken
import com.example.data.CurrencyType
import com.example.data.HistoryEntity
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val usdToVes by viewModel.usdToVes.collectAsStateWithLifecycle()
    val eurToVes by viewModel.eurToVes.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val defaultInputCurrency by viewModel.defaultInputCurrency.collectAsStateWithLifecycle()
    val targetResultCurrency by viewModel.targetResultCurrency.collectAsStateWithLifecycle()
    val activeTokens by viewModel.activeTokens.collectAsStateWithLifecycle()
    val instantResultVes by viewModel.instantResultVes.collectAsStateWithLifecycle()
    val activeTapTokenId by viewModel.activeTapTokenId.collectAsStateWithLifecycle()
    val activeLongPressTokenId by viewModel.activeLongPressTokenId.collectAsStateWithLifecycle()
    val historyList by viewModel.history.collectAsStateWithLifecycle()

    // Alert states
    val alertsEnabled by viewModel.alertsEnabled.collectAsStateWithLifecycle()
    val usdUpperThreshold by viewModel.usdUpperThreshold.collectAsStateWithLifecycle()
    val usdLowerThreshold by viewModel.usdLowerThreshold.collectAsStateWithLifecycle()
    val eurUpperThreshold by viewModel.eurUpperThreshold.collectAsStateWithLifecycle()
    val eurLowerThreshold by viewModel.eurLowerThreshold.collectAsStateWithLifecycle()
    val activeNotification by viewModel.activeNotification.collectAsStateWithLifecycle()

    // Offline simulation state
    val isOfflineSimulated by viewModel.isOfflineSimulated.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()

    // Dialog states
    var showMenuDialog by remember { mutableStateOf(false) }
    var selectedHistoryItem by remember { mutableStateOf<HistoryEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = IceBlueBg, // Pure clean light background
        topBar = {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 3-Line Menu Icon (Hamburger Menu)
                        IconButton(
                            onClick = { showMenuDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .testTag("menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menú de opciones adicionales",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "DivisaCalc",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    fontSize = 24.sp,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isOfflineSimulated) Color(0xFFF59E0B) else Color(0xFF22C55E))
                                )
                                Text(
                                    text = if (isOfflineSimulated) "Modo Offline Activo" else "Modo Online Activo",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DarkAccent.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }

                    // Top Right Avatar / Badge & Sync Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Font scaling buttons to satisfy the sight-problems requirements
                        IconButton(
                            onClick = { viewModel.decreaseFontScale() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .testTag("font_decrease_button")
                        ) {
                            Text(
                                text = "A-",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryBlue,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        IconButton(
                            onClick = { viewModel.increaseFontScale() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .testTag("font_increase_button")
                        ) {
                            Text(
                                text = "A+",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryBlue,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        IconButton(
                            onClick = { viewModel.syncRates() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = PrimaryBlue
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sincronizar Tasas",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Circular Badge "BS" in blue-50
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EurBlueBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryBlue,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Connection status sub-banner if in simulated offline
                AnimatedVisibility(
                    visible = isOfflineSimulated,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Modo Offline",
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Sin Conexión. Tasas del: ${viewModel.formatTimestamp(lastSyncTimestamp)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF78350F),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Rate buttons at the top
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dólar Button card
                    val isUsdSelected = targetResultCurrency == CurrencyType.USD
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .testTag("rate_button_usd"),
                        onClick = {
                            if (isUsdSelected) viewModel.setTargetResultCurrency(CurrencyType.VES)
                            else viewModel.setTargetResultCurrency(CurrencyType.USD)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUsdSelected) UsdRedBg else Color.White
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isUsdSelected) UsdRedBorder else UsdRedBorder.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "DÓLAR HOY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUsdSelected) UsdRedText else UsdRedText.copy(alpha = 0.7f),
                                    letterSpacing = (-0.2).sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = viewModel.formatValue(usdToVes),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = DarkAccent
                                    )
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "BS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = DarkAccent.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }

                    // Euro Button card
                    val isEurSelected = targetResultCurrency == CurrencyType.EUR
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .testTag("rate_button_eur"),
                        onClick = {
                            if (isEurSelected) viewModel.setTargetResultCurrency(CurrencyType.VES)
                            else viewModel.setTargetResultCurrency(CurrencyType.EUR)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEurSelected) EurBlueBg else Color.White
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isEurSelected) EurBlueBorder else EurBlueBorder.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "EURO HOY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEurSelected) EurBlueText else EurBlueText.copy(alpha = 0.7f),
                                    letterSpacing = (-0.2).sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = viewModel.formatValue(eurToVes),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = DarkAccent
                                    )
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "BS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = DarkAccent.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // MAIN EXPANDED CALCULATOR WRAPPER
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Warning Notification Banner floats within screen when rate exceeds threshold limit
                activeNotification?.let { notifMessage ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Alerta",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Alerta de Tipo de Cambio",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B)
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notifMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF7F1D1D),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { viewModel.dismissNotification() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(
                                            text = "Entendido",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // (1) Visual Screen Card (Display area)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .weight(1f), // Expanded display card to fill available height cleanly
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Floating instruction/hint
                        Text(
                            text = "Toque un monto para ver en Bs. Mantenga presionado para cambiar divisa.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DarkAccent.copy(alpha = 0.35f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        // Active Expression Display (Wrappable FlowRow)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 68.dp)
                                .weight(1f)
                                .padding(vertical = 12.dp)
                                .background(IceBlueBg.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (activeTokens.isEmpty()) {
                                Text(
                                    text = "0.00",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = DarkAccent.copy(alpha = 0.3f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    activeTokens.forEachIndexed { index, token ->
                                        when (token) {
                                            is CalcToken.NumberToken -> {
                                                val isTapped = activeTapTokenId == token.id
                                                val isLongPressed = activeLongPressTokenId == token.id

                                                Box {
                                                    // Styled currency digits as individual shaded boxes with rounding & specific colors, no borders, black digits
                                                    Row(
                                                        modifier = Modifier
                                                            .combinedClickable(
                                                                onClick = { viewModel.onTokenTapped(token.id) },
                                                                onLongClick = { viewModel.onTokenLongPressed(token.id) }
                                                            )
                                                            .testTag("num_token_$index"),
                                                        horizontalArrangement = Arrangement.spacedBy(0.5.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        token.valueString.forEach { char ->
                                                             Box(
                                                                 modifier = Modifier
                                                                     .padding(horizontal = 0.1.dp)
                                                                     .background(
                                                                         color = when (token.currency) {
                                                                             CurrencyType.USD -> UsdRedBg
                                                                             CurrencyType.EUR -> EurBlueBg
                                                                             CurrencyType.VES -> VesGreenBg
                                                                         },
                                                                         shape = RoundedCornerShape(3.dp)
                                                                     )
                                                             ) {
                                                                 Text(
                                                                     text = char.toString(),
                                                                     style = MaterialTheme.typography.bodyMedium.copy(
                                                                         fontWeight = FontWeight.Medium,
                                                                         color = Color.Black,
                                                                         fontSize = (25 * fontScale).sp
                                                                     ),
                                                                     modifier = Modifier.padding(horizontal = 1.5.dp, vertical = 1.dp)
                                                                 )
                                                             }
                                                        }
                                                    }

                                                    // Balloon Popups for user actions
                                                    // 1. Single Tap Tip (Value in Bs)
                                                    if (isTapped) {
                                                        Popup(
                                                            alignment = Alignment.BottomCenter,
                                                            offset = IntOffset(0, 4),
                                                            onDismissRequest = { viewModel.clearInteractivePopups() }
                                                        ) {
                                                            Surface(
                                                                color = DarkAccent,
                                                                shape = RoundedCornerShape(8.dp),
                                                                shadowElevation = 8.dp,
                                                                modifier = Modifier.padding(top = 4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "= ${viewModel.formatValue(viewModel.getTokenValueInVes(token))} Bs.",
                                                                    color = Color.White,
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // 2. Long Press Selector (Convert specific token to other format)
                                                    if (isLongPressed) {
                                                        Popup(
                                                            alignment = Alignment.BottomCenter,
                                                            offset = IntOffset(0, 4),
                                                            onDismissRequest = { viewModel.clearInteractivePopups() }
                                                        ) {
                                                            Surface(
                                                                color = Color.White,
                                                                shape = RoundedCornerShape(12.dp),
                                                                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                                                                shadowElevation = 8.dp,
                                                                modifier = Modifier.padding(top = 4.dp)
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier.padding(8.dp),
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "Asignar Divisa",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = DarkAccent
                                                                    )
                                                                    Row(
                                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                    ) {
                                                                        CurrencyType.values().forEach { curType ->
                                                                            if (curType != token.currency) {
                                                                                Box(
                                                                                    modifier = Modifier
                                                                                        .clip(RoundedCornerShape(6.dp))
                                                                                        .background(
                                                                                            when (curType) {
                                                                                                CurrencyType.USD -> UsdRedBg
                                                                                                CurrencyType.EUR -> EurBlueBg
                                                                                                CurrencyType.VES -> VesGreenBg
                                                                                            }
                                                                                        )
                                                                                        .combinedClickable(
                                                                                            onClick = {
                                                                                                viewModel.setTokenCurrency(token.id, curType)
                                                                                            }
                                                                                        )
                                                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                                ) {
                                                                                    Text(
                                                                                        text = curType.code,
                                                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                                                            fontWeight = FontWeight.Bold,
                                                                                            color = when (curType) {
                                                                                                CurrencyType.USD -> UsdRedText
                                                                                                CurrencyType.EUR -> EurBlueText
                                                                                                CurrencyType.VES -> VesGreenText
                                                                                            }
                                                                                        )
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            is CalcToken.OperatorToken -> {
                                                Text(
                                                    text = token.operator,
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        color = PrimaryBlue,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = (25 * fontScale).sp
                                                     ),
                                                    modifier = Modifier.align(Alignment.CenterVertically)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Large Calculated Result display with "TOTAL CONVERTIDO" header
                        val targetCurr = targetResultCurrency
                        val finalResVal = when (targetCurr) {
                            CurrencyType.VES -> instantResultVes
                            CurrencyType.USD -> if (usdToVes > 0.0) instantResultVes / usdToVes else 0.0
                            CurrencyType.EUR -> if (eurToVes > 0.0) instantResultVes / eurToVes else 0.0
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "TOTAL CONVERTIDO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DarkAccent.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = viewModel.formatValue(finalResVal),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = (42 * fontScale).sp,
                                        color = DarkAccent,
                                        letterSpacing = (-1).sp
                                    ),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Bottom)
                                        .padding(bottom = 6.dp)
                                        .background(
                                            color = when (targetCurr) {
                                                CurrencyType.USD -> UsdRedBg
                                                CurrencyType.EUR -> EurBlueBg
                                                CurrencyType.VES -> VesGreenBg
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${targetCurr.symbol} ${targetCurr.code}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            fontSize = (13 * fontScale).sp
                                        )
                                    )
                                }
                            }

                            // Subtitle of equivalence in BS (when viewing in USD/EUR)
                            AnimatedVisibility(
                                visible = targetCurr != CurrencyType.VES,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "Equivale a: ${viewModel.formatValue(instantResultVes)} ",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = DarkAccent.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = VesGreenBg,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Bs",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.Black,
                                                fontSize = (11 * fontScale).sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Keyboard baseline default currency selector (Subtle chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Divisa al escribir:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DarkAccent.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyType.values().forEach { curType ->
                            val isSelected = defaultInputCurrency == curType
                            val chipBg = if (isSelected) {
                                when (curType) {
                                    CurrencyType.USD -> UsdRedBg
                                    CurrencyType.EUR -> EurBlueBg
                                    CurrencyType.VES -> VesGreenBg
                                }
                            } else {
                                Color.White
                            }
                            val chipBorderColor = when (curType) {
                                CurrencyType.USD -> UsdRedBorder
                                CurrencyType.EUR -> EurBlueBorder
                                CurrencyType.VES -> VesGreenBorder
                            }
                            val chipTextColor = when (curType) {
                                CurrencyType.USD -> UsdRedText
                                CurrencyType.EUR -> EurBlueText
                                CurrencyType.VES -> VesGreenText
                            }
                            // Shadow is applied when selected to make it look "sombreada en todo momento"
                            val shadowElevation = if (isSelected) 4.dp else 0.dp
                            Box(
                                modifier = Modifier
                                    .shadow(elevation = shadowElevation, shape = RoundedCornerShape(8.dp))
                                    .background(chipBg)
                                    .combinedClickable(
                                        onClick = { viewModel.defaultInputCurrency.value = curType }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = chipBorderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("typing_currency_${curType.code}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = curType.symbol,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = chipTextColor
                                        )
                                    )
                                    Text(
                                        text = curType.code,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = chipTextColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // (2) Keypad Layout (Expanded nicely)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    val unmatchedCount = activeTokens.count { (it as? CalcToken.OperatorToken)?.operator == "(" } - 
                                         activeTokens.count { (it as? CalcToken.OperatorToken)?.operator == ")" }
                    val parenthesisKey = if (unmatchedCount > 0) ")" else "("

                    val keyRows = listOf(
                        listOf("C", "⌫", "%", "÷"),
                        listOf("7", "8", "9", "×"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf(parenthesisKey, "0", ",", "=")
                    )

                    keyRows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                val flex = 1f
                                KeypadButton(
                                    key = key,
                                    modifier = Modifier
                                        .weight(flex)
                                        .height(58.dp),
                                    fontScale = fontScale,
                                    onClick = {
                                        when (key) {
                                            "C" -> viewModel.onClearClicked()
                                            "⌫" -> viewModel.onDeleteClicked()
                                            "%" -> viewModel.onPercentClicked()
                                            "÷" -> viewModel.onOperatorClicked("÷")
                                            "×" -> viewModel.onOperatorClicked("×")
                                            "-" -> viewModel.onOperatorClicked("-")
                                            "+" -> viewModel.onOperatorClicked("+")
                                            "=" -> viewModel.onEqualClicked()
                                            ")" -> viewModel.onParenthesisClicked(")")
                                            "(" -> viewModel.onParenthesisClicked("(")
                                            else -> viewModel.onDigitClicked(key)
                                        }
                                    },
                                    onLongClick = if (key == ")") {
                                        { viewModel.onParenthesisClicked("(") }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // High Density Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryBlue.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Vía Hamburger Menú arriba accede a Historial y Alertas",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DarkAccent.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    )
                }
                Text(
                    text = "V. 2.4.0",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DarkAccent.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }

    // --- DIALOGS AND MODALS ---

    // 1. HAMBURGER OPTIONS DIALOG (Estado de Red, Alertas y Historial Reciente)
    if (showMenuDialog) {
        Dialog(onDismissRequest = { showMenuDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ajustes y Funciones",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        )
                        IconButton(onClick = { showMenuDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = DarkAccent.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // FUNCTIONALITY BLOCK 1: OFFLINE MODE
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = IceBlueBg.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOfflineSimulated) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isOfflineSimulated) Color(0xFFF59E0B) else Color(0xFF22C55E),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Estado de Red / Offline",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkAccent
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Cuando está offline, el conversor opera con caché Room local.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = DarkAccent.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Simular Conexión Offline",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkAccent
                                        )
                                    )
                                    Switch(
                                        checked = isOfflineSimulated,
                                        onCheckedChange = { viewModel.toggleOfflineSimulation(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFFF59E0B)
                                        ),
                                        modifier = Modifier.scale(0.85f)
                                    )
                                }
                                Text(
                                    text = "Último sync: ${viewModel.formatTimestamp(lastSyncTimestamp)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DarkAccent.copy(alpha = 0.4f),
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // FUNCTIONALITY BLOCK 2: CONVERSION RATE ALERTS
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = IceBlueBg.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Configuración de Alertas",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkAccent
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Alertas opcionales cuando la tasa supere o caiga bajo límites configurables.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = DarkAccent.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Activar Alertas de Tasas",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkAccent
                                        )
                                    )
                                    Switch(
                                        checked = alertsEnabled,
                                        onCheckedChange = { viewModel.toggleAlerts(it) },
                                        modifier = Modifier.scale(0.85f)
                                    )
                                }

                                if (alertsEnabled) {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = DarkAccent.copy(alpha = 0.1f)
                                    )

                                    // Limits for USD
                                    Text(
                                        text = "Límites Dólar (USD / VES):",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Superior: ${usdUpperThreshold} Bs.",
                                                style = MaterialTheme.typography.labelSmall.copy(color = DarkAccent.copy(alpha = 0.6f))
                                            )
                                            Slider(
                                                value = usdUpperThreshold.toFloat(),
                                                onValueChange = { viewModel.updateUsdBounds(it.toDouble(), usdLowerThreshold) },
                                                valueRange = 40f..45f,
                                                steps = 10,
                                                modifier = Modifier.height(26.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Inferior: ${usdLowerThreshold} Bs.",
                                                style = MaterialTheme.typography.labelSmall.copy(color = DarkAccent.copy(alpha = 0.6f))
                                            )
                                            Slider(
                                                value = usdLowerThreshold.toFloat(),
                                                onValueChange = { viewModel.updateUsdBounds(usdUpperThreshold, it.toDouble()) },
                                                valueRange = 35f..41f,
                                                steps = 12,
                                                modifier = Modifier.height(26.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Limits for EUR
                                    Text(
                                        text = "Límites Euro (EUR / VES):",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Superior: ${eurUpperThreshold} Bs.",
                                                style = MaterialTheme.typography.labelSmall.copy(color = DarkAccent.copy(alpha = 0.6f))
                                            )
                                            Slider(
                                                value = eurUpperThreshold.toFloat(),
                                                onValueChange = { viewModel.updateEurBounds(it.toDouble(), eurLowerThreshold) },
                                                valueRange = 43f..48f,
                                                steps = 10,
                                                modifier = Modifier.height(26.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Inferior: ${eurLowerThreshold} Bs.",
                                                style = MaterialTheme.typography.labelSmall.copy(color = DarkAccent.copy(alpha = 0.6f))
                                            )
                                            Slider(
                                                value = eurLowerThreshold.toFloat(),
                                                onValueChange = { viewModel.updateEurBounds(eurUpperThreshold, it.toDouble()) },
                                                valueRange = 38f..43f,
                                                steps = 10,
                                                modifier = Modifier.height(26.dp)
                                            )
                                        }
                                    }

                                    // Simulated Testing Rate change button
                                    Button(
                                        onClick = {
                                            // Force rate update above Upper limit to show real notification trigger!
                                            viewModel.simulateRateJump(42.50, 45.10)
                                            showMenuDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = UsdRedBg),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "Test Alerta (Simular Fluctuación)",
                                            color = UsdRedText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        // FUNCTIONALITY BLOCK 3: HISTORIAL RECIENTE (Limit last 5 logs)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = IceBlueBg.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.List,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Últimas 5 Conversiones",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = DarkAccent
                                            )
                                        )
                                    }

                                    if (historyList.isNotEmpty()) {
                                        TextButton(
                                            onClick = { viewModel.clearHistory() },
                                            colors = ButtonDefaults.textButtonColors(contentColor = UsdRedText),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = "Vaciar",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val recentHistory = historyList.take(5)

                                if (recentHistory.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(45.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Historial vacío.",
                                            color = DarkAccent.copy(alpha = 0.4f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                } else {
                                    recentHistory.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .background(Color.White, RoundedCornerShape(8.dp))
                                                .border(BorderStroke(1.dp, IceBlueBg), RoundedCornerShape(8.dp))
                                                .combinedClickable(
                                                    onClick = { selectedHistoryItem = item }
                                                )
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.expression,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = DarkAccent.copy(alpha = 0.5f),
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.result,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = PrimaryBlue,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "Detalles",
                                                tint = PrimaryBlue.copy(alpha = 0.4f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showMenuDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Aceptar",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // 2. DETAILED CONVERSION POPUP DIALOG (Permite ver los detalles de cada una)
    selectedHistoryItem?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedHistoryItem = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                    Text(
                        text = "Detalle de Conversión",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Fecha y hora:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = DarkAccent),
                            modifier = Modifier.width(90.dp)
                        )
                        Text(
                            text = viewModel.formatTimestamp(item.timestamp),
                            style = MaterialTheme.typography.bodySmall.copy(color = DarkAccent.copy(alpha = 0.7f))
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Expresión:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = DarkAccent),
                            modifier = Modifier.width(90.dp)
                        )
                        Text(
                            text = item.expression,
                            style = MaterialTheme.typography.bodySmall.copy(color = DarkAccent.copy(alpha = 0.7f)),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Resultado:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = DarkAccent),
                            modifier = Modifier.width(90.dp)
                        )
                        Text(
                            text = item.result,
                            style = MaterialTheme.typography.bodySmall.copy(color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        )
                    }

                    Divider(color = DarkAccent.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                    // Reference Rate caching detail
                    Text(
                        text = "Valores de Tasa de Cambio de de Referencia actuales:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = DarkAccent.copy(alpha = 0.4f))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1 USD = ${viewModel.formatValue(usdToVes)} Bs.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = VesGreenText)
                        )
                        Text(
                            text = "1 EUR = ${viewModel.formatValue(eurToVes)} Bs.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = EurBlueText)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedHistoryItem = null }) {
                    Text(text = "Cerrar", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHistoryItem(item.id)
                        selectedHistoryItem = null
                    }
                ) {
                    Text(text = "Eliminar Registro", style = MaterialTheme.typography.labelMedium.copy(color = UsdRedText, fontWeight = FontWeight.Bold))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadButton(
    key: String,
    modifier: Modifier = Modifier,
    fontScale: Float = 1.0f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val isOperator = key == "+" || key == "-" || key == "×" || key == "÷"
    val isAction = key == "C" || key == "⌫" || key == "%"
    val isEqual = key == "="

    val containerColor = when {
        isEqual -> Color(0xFF2563EB) // bg-blue-600
        isOperator -> PrimaryBlue // bg-blue-500
        isAction -> Color(0xFFF1F5F9) // bg-slate-100
        else -> Color.White // bg-white
    }
    val contentColor = when {
        isEqual -> Color.White
        isOperator -> Color.White
        isAction -> DarkAccent
        else -> DarkAccent
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("keypad_$key"),
        color = containerColor,
        border = if (isAction || isEqual || isOperator) null else BorderStroke(
            width = 1.dp,
            color = DarkAccent.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (isAction) 0.dp else if (isEqual) 3.dp else 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (key == "⌫") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retroceder",
                    tint = contentColor,
                    modifier = Modifier.size((20 * fontScale).dp)
                )
            } else {
                Text(
                    text = key,
                    color = contentColor,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isOperator || isEqual) (22 * fontScale).sp else (18 * fontScale).sp
                    )
                )
            }
        }
    }
}

// Custom scale extension modifier (just inline implementation for safety)
private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.padding(all = 0.dp) // Dummy operation for fallback or we can use graphicScale
)
