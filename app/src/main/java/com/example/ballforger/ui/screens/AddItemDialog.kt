package com.example.ballforger.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ballforger.data.AppItem
import com.example.ballforger.data.ComboSlot
import com.example.ballforger.data.ItemTier
import com.example.ballforger.ui.components.ByteArrayImage
import com.example.ballforger.ui.components.SplitMaterialByteArrayIcon
import com.example.ballforger.viewmodel.MainViewModel
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(viewModel: MainViewModel, itemToEdit: AppItem? = null, onDismiss: () -> Unit) {
    val allCombinations by viewModel.allCombinations.collectAsState()

    // Điền sẵn thông tin nếu là Edit
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var tier by remember { mutableStateOf(itemToEdit?.tier ?: ItemTier.BASE) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Fix Highlight Bug: Sử dụng List thay vì MutableList để Compose theo dõi được thay đổi
    val comboSlots = remember(itemToEdit) {
        val slots = mutableStateListOf<List<String>>(emptyList(), emptyList(), emptyList(), emptyList())
        if (itemToEdit != null && itemToEdit.tier == ItemTier.EVOLVED) {
            val combo = allCombinations.find { it.evolvedItemId == itemToEdit.id }
            combo?.slots?.forEachIndexed { index, slot ->
                if (index < 4) {
                    val list = mutableListOf<String>()
                    list.add(slot.firstMaterialId)
                    slot.secondMaterialId?.let { list.add(it) }
                    slots[index] = list
                }
            }
        }
        slots
    }

    var showMaterialSelectorForSlot by remember { mutableStateOf<Int?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            imageUrl = ""
        }
    }

    // Ưu tiên hiển thị: Ảnh mới tải lên > Link URL mới > Ảnh có sẵn khi Edit
    val previewModel = remember(selectedImageUri, imageUrl, itemToEdit) {
        when {
            selectedImageUri != null -> selectedImageUri
            imageUrl.isNotBlank() -> imageUrl
            itemToEdit != null -> ByteBuffer.wrap(itemToEdit.iconData)
            else -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (itemToEdit == null) "Add New Item" else "Edit Item") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.medium).background(Color.LightGray.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewModel != null) {
                            AsyncImage(
                                model = previewModel,
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Empty", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Button(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Gallery")
                        }
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = {
                                imageUrl = it
                                if (it.isNotBlank()) selectedImageUri = null
                            },
                            label = { Text("Or paste image URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tier == ItemTier.BASE, onClick = { tier = ItemTier.BASE })
                    Text("Base")
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = tier == ItemTier.EVOLVED, onClick = { tier = ItemTier.EVOLVED })
                    Text("Evolved")
                }

                if (tier == ItemTier.EVOLVED) {
                    Text("Materials (Max 4 slots, click to select):", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        for (i in 0..3) {
                            Box(
                                modifier = Modifier.size(48.dp).background(Color.LightGray).clickable { showMaterialSelectorForSlot = i },
                                contentAlignment = Alignment.Center
                            ) {
                                val slotItems = comboSlots[i].mapNotNull { viewModel.getItemById(it) }
                                when (slotItems.size) {
                                    0 -> Icon(Icons.Filled.Add, null)
                                    1 -> ByteArrayImage(slotItems[0].iconData, Modifier.fillMaxSize())
                                    2 -> SplitMaterialByteArrayIcon(slotItems[0].iconData, slotItems[1].iconData, Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && name.isNotBlank() && previewModel != null,
                onClick = {
                    isSaving = true
                    val finalSlots = comboSlots.filter { it.isNotEmpty() }.map {
                        ComboSlot(firstMaterialId = it[0], secondMaterialId = it.getOrNull(1))
                    }
                    viewModel.saveItem(
                        id = itemToEdit?.id,
                        name = name,
                        description = description,
                        imageUri = selectedImageUri,
                        imageUrl = imageUrl,
                        existingIconData = itemToEdit?.iconData,
                        tier = tier,
                        slots = finalSlots
                    )
                    onDismiss()
                }
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") } }
    )

    if (showMaterialSelectorForSlot != null) {
        val slotIndex = showMaterialSelectorForSlot!!
        val availableItems = viewModel.allItems.collectAsState().value

        val baseItems = availableItems.filter { it.tier == ItemTier.BASE }
        val evolvedItems = availableItems.filter { it.tier == ItemTier.EVOLVED }

        Dialog(onDismissRequest = { showMaterialSelectorForSlot = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Select materials (Max 2)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Base", style = MaterialTheme.typography.titleMedium)
                    LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                        items(baseItems) { item ->
                            val isSelected = comboSlots[slotIndex].contains(item.id)
                            MaterialSelectionCell(
                                item = item,
                                isSelected = isSelected,
                                onClick = {
                                    // Tạo list MỚI hoàn toàn để trigger Recomposition lập tức
                                    val currentSlot = comboSlots[slotIndex]
                                    if (isSelected) {
                                        comboSlots[slotIndex] = currentSlot.filter { it != item.id }
                                    } else if (currentSlot.size < 2) {
                                        comboSlots[slotIndex] = currentSlot + item.id
                                    }
                                }
                            )
                        }
                    }

                    HorizontalDivider(thickness = 2.dp, color = Color.Gray)

                    Text("Evolved", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                        items(evolvedItems) { item ->
                            val isSelected = comboSlots[slotIndex].contains(item.id)
                            MaterialSelectionCell(
                                item = item,
                                isSelected = isSelected,
                                onClick = {
                                    val currentSlot = comboSlots[slotIndex]
                                    if (isSelected) {
                                        comboSlots[slotIndex] = currentSlot.filter { it != item.id }
                                    } else if (currentSlot.size < 2) {
                                        comboSlots[slotIndex] = currentSlot + item.id
                                    }
                                }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showMaterialSelectorForSlot = null }) {
                            Text("Done", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialSelectionCell(item: AppItem, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        ByteArrayImage(
            byteArray = item.iconData,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)
        )
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}