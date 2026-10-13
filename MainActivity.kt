package com.example.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.coroutines.flow.MutableStateFlow
import androidx.coroutines.flow.StateFlow
import androidx.coroutines.flow.update

data class Task(
    val id: Long,
    val title: String,
    val done: Boolean = false
)

class ChecklistViewModel : ViewModel() {
    private var nextId = 1L
    private var _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    fun add(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        _tasks.update { it + Task(id = nextId++, title = clean) }
    }

    fun toggle(id: Long) {
        _tasks.update { list ->
            list.map { if (it.id == id) it.copy(done = !it.done) else it}
        }
    }

    fun remove(id: Long) {
        _tasks.update { list -> list.filterNot { it.id == id } }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ChecklistScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(vm: ChecklistViewModel = viewModel()) {
    val tasks by vm.tasks.collectAsState()
    var input by remember { mutableStateOf("") }
    val doneCount = tasks.count { it.done }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Checklist") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = if (tasks.isEmpty()) "Nothing yet"
                    else "$doneCount / ${tasks.size} done"
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("New task") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    vm.add(input)
                    input = ""
                }) {
                    Text("Add")
                }
            }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { vm.toggle(task.id) },
                    onDelete = { vm.remove(task.id) }
                )
            }
        }
    }
}
}

@Composable
fun TaskRow(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Surface(
        tonalElevation = if (task.done) 0.dp else 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggle() }
            )
            Text(
                text = task.title,
                style = if (task.done) {
                    MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}