package com.example.tokrw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToKrwTheme {
                ConverterScreen()
            }
        }
    }
}

@Composable
fun ToKrwTheme(content: @Composable () -> Unit) {
    // 계산기처럼 쓰는 화면이라 밝기와 무관하게 어두운 배경으로 고정한다.
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4F9CF9),
            background = Color(0xFF0B1020),
            surface = Color(0xFF141A2E),
            onBackground = Color(0xFFF2F5FF),
            onSurface = Color(0xFFF2F5FF),
        ),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(viewModel: ConverterViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickerOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Header(
                state = state,
                onCurrencyClick = { pickerOpen = true },
                onRefresh = viewModel::refresh,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End,
            ) {
                AmountBlock(state)
            }

            RateLine(state)
            Spacer(Modifier.height(12.dp))

            Keypad(
                onDigit = viewModel::onDigit,
                onDecimalPoint = viewModel::onDecimalPoint,
                onBackspace = viewModel::onBackspace,
                onClear = viewModel::onClear,
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (pickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { pickerOpen = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CurrencyPicker(
                selected = state.currency,
                snapshot = state.snapshot,
                onPick = {
                    viewModel.selectCurrency(it)
                    pickerOpen = false
                },
            )
        }
    }
}

@Composable
private fun Header(state: ConverterState, onCurrencyClick: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onCurrencyClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(state.currency.flag, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                state.currency.code,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "통화 바꾸기",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }

        Spacer(Modifier.weight(1f))

        if (state.loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "환율 새로 받기",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun AmountBlock(state: ConverterState) {
    Text(
        text = "${formatAmountInput(state.amountInput)} ${state.currency.code}",
        fontSize = 34.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        textAlign = TextAlign.End,
        maxLines = 1,
    )
    Spacer(Modifier.height(14.dp))

    val converted = state.convertedKrw
    Text(
        text = if (converted == null) "—" else "₩ ${formatKrw(converted)}",
        fontSize = if ((converted?.let { formatKrw(it).length } ?: 1) > 12) 44.sp else 56.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.End,
        maxLines = 1,
    )
}

@Composable
private fun RateLine(state: ConverterState) {
    val message = when {
        state.error != null && state.snapshot == null -> state.error
        state.unsupported -> "${state.currency.code} 환율은 지금 공급처에서 제공하지 않습니다"
        state.krwPerUnit == null -> "환율을 불러오는 중입니다"
        else -> {
            val unit = state.currency.displayUnit
            val per = state.krwPerUnit!! * unit
            "$unit ${state.currency.code} = ${formatRate(per)} KRW"
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Text(
            text = message,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.End,
        )
        val snapshot = state.snapshot
        if (snapshot != null) {
            Text(
                text = "${formatUpdatedAt(snapshot.fetchedAtMillis)} · ${snapshot.providerLabel}" +
                    if (state.error != null) " · 새로고침 실패" else "",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                textAlign = TextAlign.End,
            )
        }
    }
}

private val KEYPAD_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf(".", "0", "⌫"),
)

@Composable
private fun Keypad(
    onDigit: (Char) -> Unit,
    onDecimalPoint: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (row in KEYPAD_ROWS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (key in row) {
                    KeypadKey(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            when (key) {
                                "." -> onDecimalPoint()
                                "⌫" -> onBackspace()
                                else -> onDigit(key.first())
                            }
                        },
                        onLongClick = { if (key == "⌫") onClear() },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    key: String,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .keyClick(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        if (key == "⌫") {
            Icon(
                Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "지우기 (길게 누르면 전체 삭제)",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(
                text = key,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** 짧게 누르면 입력, 길게 누르면 전체 삭제. 키패드 키 전용 동작이다. */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.keyClick(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    combinedClickable(onClick = onClick, onLongClick = onLongClick)

@Composable
private fun CurrencyPicker(
    selected: Currency,
    snapshot: RateSnapshot?,
    onPick: (Currency) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) Currencies.all
        else Currencies.all.filter {
            it.code.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxHeight(0.9f)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("통화 이름이나 코드로 검색") },
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(filtered, key = { it.code }) { currency ->
                val rate = snapshot?.krwPerUnit(currency.code)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(currency) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(currency.flag, fontSize = 24.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            currency.code,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            currency.name,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                    }
                    if (rate != null) {
                        Text(
                            text = formatRate(rate * currency.displayUnit),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                    if (currency.code == selected.code) {
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "선택됨",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clip(CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConverterPreview() {
    ToKrwTheme { ConverterScreen() }
}
