package com.example.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.lerp

private val Bg = Color(0xFF0A0A0A)
private val Ink = Color(0xFFF2F2F2)
private val Dim = Color(0xFF8A8A8A)
private val Faint = Color(0xFF525252)
private val Line = Color(0xFF1E1E1E)
private val LineSoft = Color(0xFF262626)

private val MorphEase = CubicBezierEasing(0.32f, 0.72f, 0.35f, 1f)
private const val morphMs = 300

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val animatedProgress by animateFloatAsState(t
        targetValue = progress,
        animationSpec = tween(durationMillis = morphMs, easing = MorphEase),
        label = "progress"
    )
    
    val morph by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = morphMs, easing = MorphEase),
        label = "morph",
    )

    val inputSize by animateFloatAsState(
        targetValue = if (active) 16f else 30f,
        animationSpec = tween(durationMillis = morphMs, easing = MorphEase),
        label = "inputSize",
    )

    val inputTracking by animateFloatAsState(
        targetValue = if (active) 0f else -0.5f,
        animationSpec = tween(durationMillis = morphMs, easing = MorphEase),
        label = "inputTracking",
    )

    val topPad = lerp(132.dp, 14.dp, morph)
    val bottomPad = lerp(8.dp, 0.dp, morph)
    val promptH = lerp(34.dp, 0.dp, morph)
    val counterH = lerp(0.dp, 40.dp, morph)

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
                    Canvas(
                        modifier = Modifier
                            .size(24.dp)
                            .alpha(morph)
                            .clickable(enabled = active) { goHero() }
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
            if (active) {
                Text(
                    text = "$doneCount / ${tasks.size} done",
                    color = Faint, fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.alpha(morph),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPad, bottom = bottomPad)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(promptH)
                    .clipToBounds(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = "CHECKLIST"
                    color = Faint, fontSize = 11.sp, letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .alpha(1f - morph),
                )
            }
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = null,
                singleLine = true,
                keyboardOptions = keyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = keyboardActions(onDone = { addTask(input) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF6B6B6B),
                    unfocusedIndicatorColor = LineSoft,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                    cursorColor = Ink,
                ),
                textStyle = TextStyle(
                    fontSize = inputSize.sp,
                    letterSpacing = inputTracking.sp,
                    fontWeight = if (active) FontWeight.Normal else FontWeight.Medium,
                    textAlign = if (active) TextAlign.Start else TextAlign.Center,
                ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .alpha(morph)
            ) {
                Box(Modifier.matchParentSize().background(Color(0xFF1C1C1C)))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(Ink)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(counterH)
                    .clipToBounds(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .alpha(morph),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${doneCount} / ${tasks.size} done",
                        color = Faint, fontSize = 11.sp, letterSpacing = 2.sp,
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        color = Faint, fontSize = 11.sp, letterSpacing = 2.sp,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = morph
                        translationY = 22.dp.toPx() * (1f - morph)
                        val s = 0.98f + 0.02f * morph
                        scaleX = s
                        scaleY = s
                    }
            ) {
                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Nothing yet", color = Color(0xFF4D4D4D), fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = active,
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                focused = focusedId == task.id,
                                onTap = { focusedId = if (focusedId == task.id) null else task.id },
                                onToggle = { toggleDone(task) },
                                onDelete = {deleteTask(task.id) },
                                onNotes = { notes ->
                                    tasks = tasks.map { if (it.id == task.id) it.copy(notes = notes) else it }
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }

            if (!active) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {}
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = 1f - morph
                        translationY = -18.dp.toPx() * morph
                        val s = 1f - 0.02f * morph
                        scaleX = s 
                        scaleY = s
                    }
            ) {
                HeroTeaser(
                    tasks = tasks,
                    needle = input.trim().lowercase(),
                    tappable = !active,
                    onTap = { active = true },
                )
            }
        }
    }
}

@Composable
private fun HeroState(
    tasks: List<Task>,
    needle: String,
    tappable: Boolean,
    onTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 18.dp, start = 8.dp, end = 8.dp)
    ) {

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
                    .then(if (tappable) Modifier.clickable { onTap() } else Modifier)
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