package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EnglishLevel
import com.example.data.GeneratedWordDraft
import com.example.data.UiState
import com.example.data.VocabSource
import com.example.data.VocabularyWord
import com.example.ui.components.EmptyStateCard

private enum class VocabFilter(val label: String) {
    ALL("All"),
    TO_REVIEW("To Review"),
    LEARNED("Learned")
}

@Composable
fun VocabularyScreen(
    currentLevel: EnglishLevel,
    vocabularyState: UiState<List<VocabularyWord>>,
    dailySuggestions: List<GeneratedWordDraft>,
    isGeneratingDailyVocab: Boolean,
    isEnrichingWord: Boolean,
    onGenerateDailyVocab: () -> Unit,
    onSaveSuggestedWord: (GeneratedWordDraft, VocabSource) -> Unit,
    onAddManualWord: (String, String, String, String, String, () -> Unit) -> Unit,
    onAutoFillWord: (String, (GeneratedWordDraft) -> Unit) -> Unit,
    onToggleLearned: (VocabularyWord) -> Unit,
    onDeleteWord: (VocabularyWord) -> Unit,
    onSpeakWord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(VocabFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    val savedWords = (vocabularyState as? UiState.Success)?.data.orEmpty()
    val filteredWords = savedWords.filter { item ->
        val matchesFilter = when (activeFilter) {
            VocabFilter.ALL -> true
            VocabFilter.TO_REVIEW -> !item.learned
            VocabFilter.LEARNED -> item.learned
        }
        val q = searchQuery.trim()
        val matchesQuery = q.isBlank() ||
            item.word.contains(q, ignoreCase = true) ||
            item.marathiMeaning.contains(q, ignoreCase = true) ||
            item.exampleSentence.contains(q, ignoreCase = true)
        matchesFilter && matchesQuery
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_vocabulary_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add vocabulary word")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 860.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vocabulary Builder",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "${savedWords.count { it.learned }} learned • ${savedWords.size} saved words",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onGenerateDailyVocab,
                            enabled = !isGeneratingDailyVocab,
                            modifier = Modifier.testTag("generate_daily_vocab_button")
                        ) {
                            if (isGeneratingDailyVocab) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Daily Words")
                        }
                    }

                    // Daily AI Word Suggestions
                    if (dailySuggestions.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Today's ${currentLevel.label} Words",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                dailySuggestions.forEach { draft ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${draft.word} (${draft.partOfSpeech})",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (draft.pronunciation.isNotBlank()) {
                                                        Text(
                                                            text = "/${draft.pronunciation}/",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                }
                                                Row {
                                                    IconButton(onClick = { onSpeakWord("${draft.word}. ${draft.exampleSentence}") }) {
                                                        Icon(
                                                            Icons.AutoMirrored.Filled.VolumeUp,
                                                            contentDescription = "Pronounce word",
                                                            tint = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                    TextButton(onClick = { onSaveSuggestedWord(draft, VocabSource.DAILY) }) {
                                                        Text("Save")
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Marathi: ${draft.marathiMeaning}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = draft.exampleSentence,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search & Filter
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search English words or Marathi meanings...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vocab_search_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VocabFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick = { activeFilter = filter },
                                label = { Text(filter.label) }
                            )
                        }
                    }

                    if (filteredWords.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.Book,
                            title = if (savedWords.isEmpty()) "Your vocabulary bank is empty" else "No matching words",
                            subtitle = if (savedWords.isEmpty()) {
                                "Tap 'Daily Words' for AI word picks or '+' to add your own English word with Marathi meaning."
                            } else {
                                "Try clearing your search or switching filter tabs."
                            }
                        )
                    }
                }
            }

            items(filteredWords, key = { it.id }) { word ->
                Column(modifier = Modifier.widthIn(max = 860.dp)) {
                    VocabularyCard(
                        item = word,
                        onSpeak = { onSpeakWord("${word.word}. ${word.exampleSentence}") },
                        onToggleLearned = { onToggleLearned(word) },
                        onDelete = { onDeleteWord(word) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddVocabularyDialog(
            isEnriching = isEnrichingWord,
            onAutoFill = onAutoFillWord,
            onDismiss = { showAddDialog = false },
            onSave = { word, marathi, example, pron, pos ->
                onAddManualWord(word, marathi, example, pron, pos) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun VocabularyCard(
    item: VocabularyWord,
    onSpeak: () -> Unit,
    onToggleLearned: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.learned) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.partOfSpeech.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = item.partOfSpeech,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (item.pronunciation.isNotBlank()) {
                        Text(
                            text = "/${item.pronunciation}/",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSpeak) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Hear pronunciation",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onToggleLearned) {
                        Icon(
                            imageVector = if (item.learned) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (item.learned) "Learned" else "Mark as learned",
                            tint = if (item.learned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete word",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Marathi: ${item.marathiMeaning}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"${item.exampleSentence}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Saved: ${item.dateLearned} • ${item.source} • ${if (item.learned) "Learned" else "To Review"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddVocabularyDialog(
    isEnriching: Boolean,
    onAutoFill: (String, (GeneratedWordDraft) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var word by remember { mutableStateOf("") }
    var marathi by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var pronunciation by remember { mutableStateOf("") }
    var pos by remember { mutableStateOf("word") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Vocabulary Word") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = word,
                        onValueChange = { word = it },
                        label = { Text("English Word") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            onAutoFill(word) { draft ->
                                word = draft.word
                                marathi = draft.marathiMeaning
                                example = draft.exampleSentence
                                pronunciation = draft.pronunciation
                                pos = draft.partOfSpeech
                            }
                        },
                        enabled = word.isNotBlank() && !isEnriching
                    ) {
                        if (isEnriching) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("AI Fill")
                        }
                    }
                }
                OutlinedTextField(
                    value = marathi,
                    onValueChange = { marathi = it },
                    label = { Text("Marathi Meaning (Roman Marathi)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pronunciation,
                    onValueChange = { pronunciation = it },
                    label = { Text("Pronunciation (e.g. kon-fi-duhnt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text("Example Sentence") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(word, marathi, example, pronunciation, pos) },
                enabled = word.isNotBlank() && marathi.isNotBlank() && example.isNotBlank()
            ) {
                Text("Save Word")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
