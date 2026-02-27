package com.fitness.ui.plans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(viewModel: PlanViewModel) {
    val currentPlan by viewModel.currentPlan.collectAsStateWithLifecycle()
    val allPlans by viewModel.allPlans.collectAsStateWithLifecycle()
    var showHistory by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showHistory) "计划历史备份" else "当前训练计划") },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(Icons.Default.History, contentDescription = "历史")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showHistory) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        // 模拟更新：添加所有动作作为新计划
                        viewModel.updatePlan("我的全能计划", listOf("benchpress", "squat", "plank"))
                    },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("更新计划") }
                )
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
            Text("暂无计划，请点击下方按钮创建", color = MaterialTheme.colorScheme.outline)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(plan.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("版本: v${plan.version}", style = MaterialTheme.typography.bodySmall)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("包含动作: ${plan.exercisesJson}")
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
                supportingContent = { Text("创建于: ${df.format(Date(plan.createdAt))}") },
                trailingContent = { if (plan.isCurrent) SuggestionChip(onClick = {}, label = { Text("当前") }) }
            )
            Divider()
        }
    }
}
