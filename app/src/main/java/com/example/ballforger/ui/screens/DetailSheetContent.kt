package com.example.ballforger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ballforger.data.AppItem
import com.example.ballforger.ui.components.ByteArrayImage
import com.example.ballforger.ui.components.SplitMaterialByteArrayIcon
import com.example.ballforger.viewmodel.MainViewModel

@Composable
fun DetailSheetContent(item: AppItem, viewModel: MainViewModel, onIconClick: (AppItem) -> Unit) {
    val allCombinations = viewModel.allCombinations.collectAsState().value

    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ByteArrayImage(item.iconData, Modifier.size(64.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(item.name, style = MaterialTheme.typography.headlineMedium)
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        // Nếu item này là Evolved, hiển thị công thức của nó
        val itsCombo = allCombinations.find { it.evolvedItemId == item.id }
        if (itsCombo != null) {
            Text("Công thức tạo ra:", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                itsCombo.slots.forEach { slot ->
                    val mat1 = viewModel.getItemById(slot.firstMaterialId)
                    val mat2 = slot.secondMaterialId?.let { viewModel.getItemById(it) }

                    Box(modifier = Modifier.padding(4.dp).size(48.dp)) {
                        if (mat2 != null && mat1 != null) {
                            SplitMaterialByteArrayIcon(mat1.iconData, mat2.iconData, Modifier.fillMaxSize())
                        } else if (mat1 != null) {
                            ByteArrayImage(mat1.iconData, Modifier.fillMaxSize().clickable { onIconClick(mat1) })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reverse Lookup: Tìm các Evolved Ball/Passive có chứa item này làm nguyên liệu
        val relatedCombos = allCombinations.filter { combo ->
            combo.slots.any { it.firstMaterialId == item.id || it.secondMaterialId == item.id }
        }

        if (relatedCombos.isNotEmpty()) {
            Text("Là nguyên liệu để tiến hóa thành:", style = MaterialTheme.typography.titleMedium)
            relatedCombos.forEach { combo ->
                val evolvedResult = viewModel.getItemById(combo.evolvedItemId)
                if (evolvedResult != null) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp).clickable { onIconClick(evolvedResult) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ByteArrayImage(evolvedResult.iconData, Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(evolvedResult.name)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp)) // Tránh bị Bottom Nav Bar che
    }
}