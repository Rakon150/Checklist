package com.example.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

private val Bg = Color(0xFF0A0A0A)
private val Ink = Color(0xFFF2F2F2)
private val Dim = Color(0xFF8A8A8A)
private val Faint = Color(0xFF525252)
private val Line = Color(0xFF1E1E1E)
private val LineSoft = Color(0xFF262626)

data class Task(
    val id: Long,
    val title: String,
    val notes: String = "",
    val done: Boolean = false,
    val doneSeq: Long = 0L
)

private fun seedTasks() = listOf(
    Task(id = 3, title = "Water plants"),
    Task(id = 2, title = "Prepare for exam"),
    Task(id = 1, title = "Pet the cat", done = true, doneSeq = 2)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Bg, surface = Bg,
                    onBackground = Ink, onSurface = Ink,
                    surfaceVariant = Bg, onSurfaceVariant = Dim,
                )
            ) {
                ChecklistScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen() {
    var tasks by remember { mutableStateOf(seedTasks()) }
    var input by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    var focusedId by remember { mutableStateOf<Long?>(null) }
    var nextId by remember { mutableStateOf(6L) }
    var nextDoneSeq by remember { mutableLongStateOf(3L) }
    val focusRequester = remember { FocusRequester() }

    fun order(ts: List<Task>): List<Task> {
        val undone = ts.filter { !it.done }
        val done = ts.filter { it.done }.sortedByDescending { it.doneSeq }
        return undone + done
    }

    fun addTask(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        tasks = order(listOf(Task(id = nextId++, title = clean)) + tasks)
        input = ""
        focusedId = null
        active = true
    }

    fun toggleDone(task: Task) {
        tasks = if (!task.done) {
            val without = tasks.filterNot { it.id == task.id }
            val insertAt = without.indexOfLast { !it.done } + 1
            val updated = task.copy(done = true, doneSeq = nextDoneSeq++)
            (without.subList(0, insertAt) + updated + without.subList(insertAt, without.size))
        } else {
            val updated = task.copy(done = false)
            order(listOf(updated) + tasks.filterNot { it.id == task.id })
        }
    }

    fun deleteTask(id: Long) {
        tasks = tasks.filterNot { it.id == id }
        if (focusedId == id) focusedId = null
    }

    fun goHero() {
        active = false
        focusedId = null
    }

    BackHandler(enabled = active) { goHero() }

    val doneCount = tasks.count { it.done }
    val progress = if (tasks.isEmpty()) 0f else doneCount.toFloat() / tasks.size
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 400), label = "progress")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (active) {
                    Canvas(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { goHero() }
                    ) {
                        val c = Dim
                        val w = size.width
                        val h = size.height
                        drawLine(c, start = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.25f),
                            end = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.5f), strokeWidth = 3f)

                        drawLine(c, start = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.5f),
                            end = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.75f), strokeWidth = 3f)
                    }
                }
            }
            if (active) {
                Text(
                    text = "$doneCount / ${tasks.size} done",
                    color = Faint, fontSize = 11.sp,
                    letterSpacing = 2.sp,
                )
            }
        }
        AnimatedContent(
            targetState = active,
            transitionSpec = {
                (fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 5 }) togetherWith (fadeOut(tween(500)) + slideOutVertically(tween(500)) { it / 5 }) using SizeTransform(clip = false)
            }, label = "screen",
        ){ isActive ->
            if (!isActive) {
            HeroState(
                input = input,
                onInput = { input = it },
                tasks = tasks,
                focusRequester = focusRequester,
                onAdd = { addTask(it) },
                onTeaserTap = { active = true },
            )
        } else {
            ActiveState(
                input = input,
                onInput = { input = it },
                tasks = tasks,
                progress = animatedProgress,
                focusedId = focusedId,
                onFocusRow = { focusedId = if (focusedId == it) null else it },
                onToggle = { toggleDone(it) },
                onDelete = { deleteTask(it) },
                onNotes = { id, notes ->
                    tasks = tasks.map { if (it.id == id) it.copy(notes = notes) else it }
                },
                onAdd = { addTask(it) },
            )}
        }
    }
}

@Composable
private fun HeroState(
    input: String,
    onInput: (String) -> Unit,
    tasks: List<Task>,
    focusRequester: FocusRequester,
    onAdd: (String) -> Unit,
    onTeaserTap: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {

    val needle = input.trim().lowercase()
    val teaser = remember(tasks) { tasks.take(5) }
    val shown = remember(teaser, needle) {
        if (needle.isEmpty()) teaser
        else teaser.filter { (it.title + " " + it.notes).lowercase().contains(needle) }
    }

    Spacer(Modifier.height(132.dp))
    Text(
        text = "CHECKLIST",
        color = Faint, fontSize = 11.sp, letterSpacing = 4.sp,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(14.dp))
    TextField(
        value = input,
        onValueChange = onInput,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAdd(input) }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color(0xFF6B6B6B),
            unfocusedIndicatorColor = LineSoft,
            focusedTextColor = Ink, unfocusedTextColor = Ink,
            cursorColor = Ink,
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 30.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center, letterSpacing = (-0.5).sp,
        ),
    )

    Spacer(Modifier.height(18.dp))
    if (tasks.isEmpty() && needle.isEmpty()) {
        Text(
            text = "Nothing yet",
            color = Color(0xFF4D4D4D), fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
    } else if (needle.isNotEmpty() && shown.isEmpty()) {
        Text(
            text = "No matches",
            color = Faint, fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
    } else {
        val sizes = listOf(17, 16, 15, 14, 13)
        val alphas = listOf(0.55f, 0.36f, 0.22f, 0.12f, 0.07f)
            shown.forEachIndexed { i, t ->
            val size = sizes.getOrElse(i) { 13 }
            val alpha = alphas.getOrElse(i) { 0.06f }
            val sc = Math.pow(0.9, i.toDouble()).toFloat()
            Text(
                text = highlightMatch(t.title, needle),
                fontSize = size.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(sc)
                    .alpha(alpha)
                    .clickable { onTeaserTap() }
                    .padding(vertical = 1.dp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun highlightMatch(text: String, needle: String): androidx.compose.ui.text.AnnotatedString {
    if (needle.isEmpty()) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = Ink)) { append(text) }
        }
    }
    val idx = text.lowercase().indexOf(needle)
    if (idx < 0) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = Ink)) { append(text) }
        }
    }
    return buildAnnotatedString {
        withStyle(SpanStyle(color = Ink)) { append(text.substring(0, idx)) }
        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) {
            append(text.substring(idx, idx + needle.length))
        }
        withStyle(SpanStyle(color = Ink)) { append(text.substring(idx + needle.length )) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveState(
    input: String,
    onInput: (String) -> Unit,
    tasks: List<Task>,
    progress: Float,
    focusedId: Long?,
    onFocusRow: (Long) -> Unit,
    onToggle: (Task) -> Unit,
    onDelete: (Long) -> Unit,
    onNotes: (Long, String) -> Unit,
    onAdd: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
    TextField(
        value = input,
        onValueChange = onInput,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAdd(input) }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color(0xFF6B6B6B),
            unfocusedIndicatorColor = LineSoft,
            focusedTextColor = Ink, unfocusedTextColor = Ink,
            cursorColor = Ink,
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF1C1C1C))
    ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Ink)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val doneN = tasks.count { it.done }
            Text(
                text = "$doneN / ${tasks.size} done",
                color = Faint, fontSize = 11.sp, letterSpacing = 2.sp,
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Faint, fontSize = 11.sp, letterSpacing = 2.sp,
            )
        }
        Spacer(Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Nothing yet", color = Color(0xFF4D4D4D), fontSize = 14.sp)
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    focused = focusedId == task.id,
                    onTap = { onFocusRow(task.id) },
                    onToggle = { onToggle(task) },
                    onDelete = { onDelete(task.id) },
                    onNotes = { onNotes(task.id, it) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    focused: Boolean,
    onTap: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onNotes: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strike by animateFloatAsState(
        targetValue = if (task.done) 1f else 0f,
        animationSpec = tween(durationMillis = 400), label = "strike"
    )
    val faded = 1f - 0.6f * strike

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (focused) Color(0xFF0E0E0E) else Color.Transparent)
            .then(if (focused) Modifier.border(1.dp, Color(0xFF2C2C2C)) else Modifier)
            .clickable { onTap() }
            .padding(horizontal = 10.dp, vertical = 14.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(if (task.done) Ink else Color.Transparent)
                    .then(if (!task.done) Modifier.border(1.5.dp, Dim) else Modifier)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center,
            ) {
                if (task.done) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                        drawLine(
                            color = Bg,
                            start = Offset(size.width * 0.25f, size.height * 0.55f),
                            end = Offset(size.width * 0.45f, size.height * 0.72f),
                            strokeWidth = 3f,
                        )
                        drawLine(
                            color = Bg,
                            start = Offset(size.width * 0.45f, size.height * 0.72f),
                            end = Offset(size.width * 0.78f, size.height * 0.3f),
                            strokeWidth = 3f,
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = task.title,
                color = Ink.copy(alpha = faded),
                fontSize = 15.sp,
                modifier = Modifier
                    .weight(1f)
                    .drawBehind {
                        val y = size.height / 2f
                        drawLine(
                            color = Dim,
                            start = Offset(0f, y),
                            end = Offset(size.width * strike, y),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
            )
            if (focused) {
                Text(
                    text = "x",
                    color = Faint, fontSize = 18.sp,
                    modifier = Modifier
                        .clickable{ onDelete() }
                        .padding(start = 8.dp),
                )
            }
        }
        if (focused) {
            Spacer(Modifier.height(10.dp))
            TextField(
                value = task.notes,
                onValueChange = onNotes,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "Add a note...", color = Color(0xFF3A3A3A), fontSize = 13.sp)},
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onTap() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Line,
                    unfocusedIndicatorColor = Line,
                    focusedTextColor = Dim,
                    unfocusedTextColor = Dim,
                    cursorColor = Ink,
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (task.done) "MARK NOT DONE" else "MARK DONE",
                    color = Dim, fontSize = 11.sp, letterSpacing = 2.sp,
                    modifier = Modifier.clickable{ onToggle() },
                )
                Text(
                    text = "TAP TO COLLAPSE",
                    color = Color(0xFF3D3D3D), fontSize = 11.sp, letterSpacing = 2.sp,
                )
            }
        } else if (task.notes.isNotEmpty()) {
            Text(
                text = task.notes,
                color = Faint, fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 30.dp, top = 2.dp)
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Line)
    )
}