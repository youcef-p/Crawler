package com.example.reelscraper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reelscraper.data.model.ExtractionRule
import com.example.reelscraper.data.model.SiteProfile
import com.example.reelscraper.ui.viewmodel.SiteProfilesViewModel
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaCardBg
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteProfilesScreen(
    viewModel: SiteProfilesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val snackMessage by viewModel.snackMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingProfile by remember { mutableStateOf<SiteProfile?>(null) }
    var isAddingProfile by remember { mutableStateOf(false) }

    var editingRule by remember { mutableStateOf<ExtractionRule?>(null) }
    var isAddingRule by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CinemaBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = CinemaSurface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "Site Profiles & Rules",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) isAddingProfile = true else isAddingRule = true
                },
                containerColor = NeonCyan,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_profile_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CinemaSurface,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Site Profiles (${profiles.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Extraction Rules (${rules.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileItemCard(
                            profile = profile,
                            onToggle = { viewModel.toggleProfile(profile) },
                            onEdit = { editingProfile = profile },
                            onDuplicate = { viewModel.duplicateProfile(profile) },
                            onDelete = { viewModel.deleteProfile(profile) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        RuleItemCard(
                            rule = rule,
                            onDelete = { viewModel.deleteRule(rule) }
                        )
                    }
                }
            }
        }

        if (isAddingProfile || editingProfile != null) {
            ProfileEditDialog(
                initialProfile = editingProfile,
                onDismiss = {
                    isAddingProfile = false
                    editingProfile = null
                },
                onSave = { profile ->
                    viewModel.saveProfile(profile)
                    isAddingProfile = false
                    editingProfile = null
                }
            )
        }

        if (isAddingRule) {
            RuleEditDialog(
                onDismiss = { isAddingRule = false },
                onSave = { rule ->
                    viewModel.saveRule(rule)
                    isAddingRule = false
                }
            )
        }
    }
}

@Composable
private fun ProfileItemCard(
    profile: SiteProfile,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CinemaCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.domain,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Switch(
                    checked = profile.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Color.DarkGray,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Depth: ${profile.defaultDepth}",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (profile.enableDynamicStreaming) {
                    Surface(
                        color = NeonPurple.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Dynamic: ${profile.dynamicMode}",
                            color = NeonPurple,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NeonCyan, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun RuleItemCard(
    rule: ExtractionRule,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CinemaCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = rule.ruleType,
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = rule.domain, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rule.regex ?: rule.selector ?: rule.attribute ?: "Target: ${rule.targetType}",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ProfileEditDialog(
    initialProfile: SiteProfile?,
    onDismiss: () -> Unit,
    onSave: (SiteProfile) -> Unit
) {
    var domain by remember { mutableStateOf(initialProfile?.domain ?: "") }
    var depth by remember { mutableIntStateOf(initialProfile?.defaultDepth ?: 2) }
    var enableDynamic by remember { mutableStateOf(initialProfile?.enableDynamicStreaming ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurface,
        title = {
            Text(
                text = if (initialProfile == null) "New Site Profile" else "Edit Site Profile",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain (e.g. example.com)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scan Depth: $depth", color = Color.White)
                    Row {
                        IconButton(onClick = { if (depth > 1) depth-- }) {
                            Text("-", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { if (depth < 10) depth++ }) {
                            Text("+", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Dynamic Streaming", color = Color.White)
                    Switch(
                        checked = enableDynamic,
                        onCheckedChange = { enableDynamic = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (domain.isNotBlank()) {
                        val profile = initialProfile?.copy(
                            domain = domain.trim().lowercase(),
                            defaultDepth = depth,
                            enableDynamicStreaming = enableDynamic,
                            updatedAt = System.currentTimeMillis()
                        ) ?: SiteProfile(
                            domain = domain.trim().lowercase(),
                            defaultDepth = depth,
                            enableDynamicStreaming = enableDynamic
                        )
                        onSave(profile)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
private fun RuleEditDialog(
    onDismiss: () -> Unit,
    onSave: (ExtractionRule) -> Unit
) {
    var domain by remember { mutableStateOf("") }
    var ruleType by remember { mutableStateOf("REGEX") }
    var pattern by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurface,
        title = {
            Text("Add Extraction Rule", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain (e.g. example.com)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Regex or CSS Selector") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (domain.isNotBlank() && pattern.isNotBlank()) {
                        val rule = ExtractionRule(
                            domain = domain.trim().lowercase(),
                            ruleType = ruleType,
                            regex = pattern.trim(),
                            targetType = "MEDIA_URL"
                        )
                        onSave(rule)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Add Rule", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
