package com.example.ballforger.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ballforger.data.AppItem
import com.example.ballforger.data.Combination
import com.example.ballforger.data.ItemCategory
import com.example.ballforger.data.ItemTier
import com.example.ballforger.ui.components.ByteArrayImage
import com.example.ballforger.ui.components.SplitMaterialByteArrayIcon
import com.example.ballforger.viewmodel.MainViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentCategory by viewModel.currentCategory.collectAsState()
    val allItems by viewModel.allItems.collectAsState()

    // States
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<AppItem?>(null) }
    var itemMenuAction by remember { mutableStateOf<AppItem?>(null) }

    // Search & Menu States
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSettingsMenu by remember { mutableStateOf(false) }

    // Navigation & Panel States
    var selectedItemForDetail by remember { mutableStateOf<AppItem?>(null) }
    // Hỗ trợ chọn 1 hoặc NHIỀU item để hiện trong Quick Panel (cho icon chéo)
    var selectedItemsForQuickPanel by remember { mutableStateOf<List<AppItem>?>(null) }
    val quickPanelSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // (LƯU Ý: Thêm dòng val context = LocalContext.current ở ngay đầu hàm MainScreen nếu chưa có)
    val context = androidx.compose.ui.platform.LocalContext.current

    // CÔNG CỤ TẠO FILE ĐỂ EXPORT
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) viewModel.exportData(context, uri)
    }

    // CÔNG CỤ MỞ FILE ĐỂ IMPORT
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importData(context, uri)
    }

    // LINK GITHUB ĐỂ PULL (Dán link RAW text file của github vào đây)
    val githubRawUrl = "https://raw.githubusercontent.com/..."

    BackHandler(enabled = selectedItemForDetail != null) {
        selectedItemForDetail = null
    }

    if (selectedItemForDetail == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearchMode) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search...", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("Ball Forger", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    actions = {
                        if (isSearchMode) {
                            IconButton(onClick = { isSearchMode = false; searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close Search", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        } else {
                            IconButton(onClick = { isSearchMode = true }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            IconButton(onClick = { showSettingsMenu = true }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onPrimary)
                            }

                            DropdownMenu(
                                expanded = showSettingsMenu,
                                onDismissRequest = { showSettingsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Pull data from GitHub") },
                                    onClick = {
                                        showSettingsMenu = false
                                        if (githubRawUrl.contains("http")) {
                                            viewModel.pullDataFromGithub(context, githubRawUrl)
                                        } else {
                                            Toast.makeText(context, "Vui lòng cấu hình Link Github trong code", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export to txt") },
                                    onClick = {
                                        showSettingsMenu = false
                                        // Tạo tên file mặc định
                                        exportLauncher.launch("BallForger_Data.txt")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import txt") },
                                    onClick = {
                                        showSettingsMenu = false
                                        // Chỉ cho phép chọn file text
                                        importLauncher.launch(arrayOf("text/plain"))
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
            }
            // Loại bỏ floatingActionButton mặc định để gom chung xuống Bottom Row
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)))

                AnimatedContent(
                    targetState = currentCategory,
                    transitionSpec = {
                        if (targetState == ItemCategory.PASSIVE) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                        }
                    },
                    label = "CategoryTransition"
                ) { category ->
                    // 1. Lọc toàn bộ item theo Category và Từ khóa tìm kiếm trước
                    val filteredItems = allItems.filter {
                        it.category == category &&
                                it.name.contains(searchQuery, ignoreCase = true)
                    }

                    Column(modifier = Modifier.fillMaxSize()) {

                        if (isSearchMode) {
                            // ====================================================
                            // CHẾ ĐỘ TÌM KIẾM: Gộp chung thành 1 Grid phẳng duy nhất
                            // ====================================================
                            Spacer(modifier = Modifier.height(12.dp)) // Tạo một chút padding với TopBar
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            ) {
                                items(filteredItems) { item ->
                                    EnhancedItemCell(item, onClick = { selectedItemForDetail = item }, onLongClick = { itemMenuAction = item })
                                }
                            }
                        } else {
                            // ====================================================
                            // CHẾ ĐỘ BÌNH THƯỜNG: Chia 50/50 Base và Evolved
                            // ====================================================
                            val baseItems = filteredItems.filter { it.tier == ItemTier.BASE }
                            val evolvedItems = filteredItems.filter { it.tier == ItemTier.EVOLVED }

                            SectionHeader(title = "Base ${category.name.lowercase().replaceFirstChar { it.uppercase() }}")
                            LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                items(baseItems) { item ->
                                    EnhancedItemCell(item, onClick = { selectedItemForDetail = item }, onLongClick = { itemMenuAction = item })
                                }
                            }

                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                            SectionHeader(title = "Evolved ${category.name.lowercase().replaceFirstChar { it.uppercase() }}")
                            LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                items(evolvedItems) { item ->
                                    EnhancedItemCell(item, onClick = { selectedItemForDetail = item }, onLongClick = { itemMenuAction = item })
                                }
                            }
                        }

                        // Chừa chỗ cho thanh Bottom Bar
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                // ==========================================
                // BOTTOM BAR (Toggle + Add Button nằm ngang)
                // ==========================================
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .zIndex(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VipProToggle(
                        currentCategory = currentCategory,
                        onCategorySelected = { viewModel.setCategory(it) },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    FloatingActionButton(
                        onClick = { itemToEdit = null; showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = { selectedItemForDetail = null }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                )
            }
        ) { paddingValues ->
            DetailScreenContent(
                item = selectedItemForDetail!!,
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues),
                onIconClick = { clickedItems -> selectedItemsForQuickPanel = clickedItems }
            )
        }
    }

    // Action Menu Dialog (Edit / Delete)
    val actionItem = itemMenuAction // Gán vào biến tĩnh cục bộ để tránh lỗi null
    if (actionItem != null) {
        AlertDialog(
            onDismissRequest = { itemMenuAction = null },
            title = { Text("Action") },
            text = { Text("What do you want to do with ${actionItem.name}?") },
            confirmButton = {
                Button(onClick = {
                    itemToEdit = actionItem
                    itemMenuAction = null
                    showAddDialog = true
                }) { Text("Edit") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.deleteItem(actionItem)
                        itemMenuAction = null
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            }
        )
    }

    if (showAddDialog) {
        AddItemDialog(viewModel = viewModel, itemToEdit = itemToEdit, onDismiss = { showAddDialog = false; itemToEdit = null })
    }

    // ==========================================
    // SIÊU QUICK PANEL (Hỗ trợ nhiều item + Render công thức)
    // ==========================================
    if (selectedItemsForQuickPanel != null) {
        ModalBottomSheet(onDismissRequest = { selectedItemsForQuickPanel = null }, sheetState = quickPanelSheetState) {
            val allCombinations = viewModel.allCombinations.collectAsState().value

            // FIX CRASH Ở ĐÂY: Dùng biến đệm để tránh NullPointerException khi đóng panel
            val itemsToRender = selectedItemsForQuickPanel ?: emptyList()

            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(itemsToRender) { panelItem ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        ByteArrayImage(panelItem.iconData, modifier = Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)).background(Color.LightGray))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(panelItem.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(panelItem.description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)

                        // Nếu là Evolved, render công thức ghép của chính nó
                        if (panelItem.tier == ItemTier.EVOLVED) {
                            val itsCombo = allCombinations.find { it.evolvedItemId == panelItem.id }
                            if (itsCombo != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Recipe:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                // Render công thức nhưng chặn click để không mở panel đè lên panel
                                EvolutionFormulaRow(combo = itsCombo, evolvedItem = panelItem, viewModel = viewModel, onIconClick = {})
                            }
                        }
                    }

                    // Ngăn cách giữa các item nếu có nhiều item
                    if (itemsToRender.size > 1 && panelItem != itemsToRender.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: NÚT TOGGLE VIP PRO (Tự dãn theo weight)
// ==========================================
@Composable
fun VipProToggle(currentCategory: ItemCategory, onCategorySelected: (ItemCategory) -> Unit, modifier: Modifier = Modifier) {
    val isPassives = currentCategory == ItemCategory.PASSIVE

    BoxWithConstraints(modifier = modifier.height(56.dp)) {
        val switchWidth = maxWidth
        val indicatorWidth = switchWidth / 2

        val indicatorOffset by animateDpAsState(
            targetValue = if (isPassives) indicatorWidth else 0.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "toggleAnimation"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)))
            )

            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onCategorySelected(ItemCategory.BALL) }, contentAlignment = Alignment.Center) {
                    Text("Balls", fontWeight = FontWeight.Bold, color = if (!isPassives) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onCategorySelected(ItemCategory.PASSIVE) }, contentAlignment = Alignment.Center) {
                    Text("Passives", fontWeight = FontWeight.Bold, color = if (isPassives) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: Ô VẬT PHẨM (Đã fix font siêu nhỏ)
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedItemCell(item: AppItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(0.85f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.radialGradient(colors = listOf(Color.White, Color.LightGray)))
            ) {
                ByteArrayImage(byteArray = item.iconData, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.name,
                style = TextStyle(
                    fontSize = 10.sp, // Cố định cỡ chữ nhỏ để vừa màn hình bé
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary
    )
}

// ==========================================
// CÁC COMPONENT DETAIL SCREEN & CÔNG THỨC
// ==========================================
@Composable
fun DetailScreenContent(item: AppItem, viewModel: MainViewModel, modifier: Modifier, onIconClick: (List<AppItem>) -> Unit) {
    val allCombinations = viewModel.allCombinations.collectAsState().value

    Column(modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ByteArrayImage(
                byteArray = item.iconData,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onIconClick(listOf(item)) }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(item.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(item.tier.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        if (item.tier == ItemTier.EVOLVED) {
            val itsCombo = allCombinations.find { it.evolvedItemId == item.id }
            if (itsCombo != null) {
                Text("Recipe:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(12.dp))
                EvolutionFormulaRow(combo = itsCombo, evolvedItem = item, viewModel = viewModel, onIconClick = onIconClick)
            }
        }

        val relatedCombos = allCombinations.filter { combo -> combo.slots.any { it.firstMaterialId == item.id || it.secondMaterialId == item.id } }

        if (relatedCombos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Used in Evolutions:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(12.dp))
            relatedCombos.forEach { combo ->
                val evolvedResult = viewModel.getItemById(combo.evolvedItemId)
                if (evolvedResult != null) {
                    EvolutionFormulaRow(combo = combo, evolvedItem = evolvedResult, viewModel = viewModel, onIconClick = onIconClick)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun EvolutionFormulaRow(combo: Combination, evolvedItem: AppItem, viewModel: MainViewModel, onIconClick: (List<AppItem>) -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        // Evolved Icon
        FormulaIcon(item = evolvedItem, onClick = { onIconClick(listOf(evolvedItem)) })

        Text(" = ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp).padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurface)

        combo.slots.forEachIndexed { index, slot ->
            val mat1 = viewModel.getItemById(slot.firstMaterialId)
            val mat2 = slot.secondMaterialId?.let { viewModel.getItemById(it) }

            if (mat1 != null && mat2 != null) {
                // Hiển thị Icon cắt chéo + Tên ghép
                FormulaSplitIcon(mat1 = mat1, mat2 = mat2, onClick = { onIconClick(listOf(mat1, mat2)) })
            } else if (mat1 != null) {
                FormulaIcon(item = mat1, onClick = { onIconClick(listOf(mat1)) })
            }

            if (index < combo.slots.size - 1) {
                Text(" + ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp).padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// Icon cho 1 item
@Composable
fun FormulaIcon(item: AppItem, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Box(
            modifier = Modifier.size(56.dp).shadow(2.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(Brush.radialGradient(colors = listOf(Color.White, Color.LightGray))).clickable { onClick() }.padding(2.dp)
        ) {
            ByteArrayImage(byteArray = item.iconData, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.name,
            style = TextStyle(fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
            maxLines = 2, minLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
        )
    }
}

// Icon cắt chéo cho 2 item trong công thức
@Composable
fun FormulaSplitIcon(mat1: AppItem, mat2: AppItem, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) { // Rộng hơn 1 xíu để chứa 2 tên
        Box(
            modifier = Modifier.size(56.dp).shadow(2.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(Brush.radialGradient(colors = listOf(Color.White, Color.LightGray))).clickable { onClick() }.padding(2.dp)
        ) {
            SplitMaterialByteArrayIcon(iconData1 = mat1.iconData, iconData2 = mat2.iconData, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${mat1.name} / ${mat2.name}",
            style = TextStyle(fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
            maxLines = 2, minLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
        )
    }
}