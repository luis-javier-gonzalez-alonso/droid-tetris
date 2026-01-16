package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import net.ljga.projects.games.tetris.R

@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val coins by viewModel.coins.collectAsState()
    val ownedBadges by viewModel.ownedBadges.collectAsState()
    
    val allItems = remember {
        viewModel.allMutations + viewModel.allArtifacts
    }

    var showConfirmDialog by remember { mutableStateOf<GameMechanic?>(null) }
    
    // Background gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C3E50),
                        Color(0xFF4CA1AF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onBack) {
                    Text("Back")
                }
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coins: $coins",
                            color = Color(0xFFFFD700),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Text(
                text = "Badge Shop",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            // Grid of Badges
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allItems) { item ->
                    val isOwned = ownedBadges.contains(item.name)
                    ShopItemCard(
                        item = item,
                        isOwned = isOwned,
                        cost = 500, // Fixed cost for now
                        onItemClick = {
                            if (!isOwned) {
                                showConfirmDialog = item
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Purchase Confirmation Dialog
    showConfirmDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            title = { Text(text = "Purchase Badge?") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = item.name, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = item.description)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Cost: 500 Coins")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (coins >= 500) {
                            viewModel.purchaseBadge(item.name, 500)
                            showConfirmDialog = null
                        }
                    },
                    enabled = coins >= 500
                ) {
                    Text("Purchase")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ShopItemCard(
    item: GameMechanic,
    isOwned: Boolean,
    cost: Int,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f) // Taller than wide
            .clickable(enabled = !isOwned, onClick = onItemClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isOwned) Color.DarkGray else Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = item.iconResId),
                contentDescription = item.name,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(4.dp)
            )
            
            Text(
                text = item.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = if (isOwned) Color.LightGray else Color.Black
            )
            
            if (isOwned) {
                Text(
                    text = "OWNED",
                    fontSize = 10.sp,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "$cost",
                    fontSize = 12.sp,
                    color = Color(0xFFD35400), // Dark orange
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
