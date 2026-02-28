package com.fitness.ui.plans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitness.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(viewModel: PlanViewModel, onStartPlan: (Int) -> Unit) {
    val currentPlan by viewModel.currentPlan.collectAsStateWithLifecycle()
    val allPlans by viewModel.allPlans.collectAsStateWithLifecycle()
    var showHistory by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (showHistory) R.string.plan_history else R.string.current_plan)) },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(Icons.Default.History, contentDescription = stringResource(R.string.plan_history))
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showHistory) {
                Column(horizontalAlignment = Alignment.End) {
                    if (currentPlan != null) {
                        SmallFloatingActionButton(
                            onClick = { onStartPlan(currentPlan!!.id) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.PlayArrow, "Start")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    ExtendedFloatingActionButton(
                        onClick = { 
                            viewModel.updatePlan("Full Body Plan", listOf("benchpress", "squat", "plank", "pushup"))
                        },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text(stringResource(R.string.update_plan)) }
                    )
                }
            }
        }
    ) { padding ->
        if (showHistory) {
            HistoryList(allPlans, padding)
        } else {
            CurrentPlanView(currentPlan, padding)
        }
    }
}

@Composable
fun CurrentPlanView(plan: com.fitness.data.local.PlanEntity?, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (plan == null) {
            Text(stringResource(R.string.no_plan), color = MaterialTheme.colorScheme.outline)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(plan.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.plan_version, plan.version), style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.plan_exercises, plan.exercisesJson))
                }
            }
        }
    }
}

@Composable
fun HistoryList(plans: List<com.fitness.data.local.PlanEntity>, padding: PaddingValues) {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
        items(plans) { plan ->
            ListItem(
                headlineContent = { Text("${plan.name} (v${plan.version})") },
                supportingContent = { Text(stringResource(R.string.plan_created_at, df.format(Date(plan.createdAt)))) },
                trailingContent = { 
                    if (plan.isCurrent) SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.current_tag)) }) 
                }
            )
            HorizontalDivider()
        }
    }
}
