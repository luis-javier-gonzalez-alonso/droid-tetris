package net.ljga.projects.games.tetris.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ljga.projects.games.tetris.R

@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val coins by viewModel.coins.collectAsState()
    val ownedBadges by viewModel.ownedBadges.collectAsState()
    val unlockedMutations by viewModel.unlockedMutations.collectAsState()
    val enabledMutations by viewModel.enabledMutations.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showConfirmDialog by remember { mutableStateOf<Any?>(null) } // Can be Mutation or Badge

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
            
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Mutations") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Badges") })
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Mutations Shop
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.allMutations) { mutation ->
                        val isUnlocked = unlockedMutations.contains(mutation.name)
                        val isEnabled = enabledMutations.contains(mutation.name)
                        
                        MutationShopCard(
                            mutation = mutation,
                            isUnlocked = isUnlocked,
                            isEnabled = isEnabled,
                            onToggle = { enabled ->
                                viewModel.toggleMutation(mutation.name, enabled)
                            },
                            onBuy = {
                                showConfirmDialog = mutation
                            }
                        )
                    }
                }
            } else {
                // Badges Shop
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.allBadges) { badge ->
                        val isOwned = ownedBadges.contains(badge.id)
                        BadgeShopCard(
                            badge = badge,
                            isOwned = isOwned,
                            onBuy = {
                                showConfirmDialog = badge
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Purchase Confirmation Dialog
    showConfirmDialog?.let { item ->
        val isMutation = item is Mutation
        val name = if (isMutation) (item as Mutation).name else (item as GameViewModel.Badge).name
        val cost = if (isMutation) 500 else (item as GameViewModel.Badge).cost
        val icon = if (isMutation) (item as Mutation).iconResId else (item as GameViewModel.Badge).iconResId

        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            title = { Text(text = "Purchase $name?") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Cost: $cost Coins")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (coins >= cost) {
                            if (isMutation) {
                                viewModel.purchaseMutation((item as Mutation).name, cost)
                            } else {
                                viewModel.purchaseBadge((item as GameViewModel.Badge).id, cost)
                            }
                            showConfirmDialog = null
                        }
                    },
                    enabled = coins >= cost
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
fun MutationShopCard(
    mutation: Mutation,
    isUnlocked: Boolean,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isUnlocked, onClick = onBuy),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color.DarkGray else Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Image(
                    painter = painterResource(id = mutation.iconResId),
                    contentDescription = mutation.name,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(4.dp)
                )
                if (isUnlocked) {
                   Switch(
                       checked = isEnabled,
                       onCheckedChange = onToggle,
                       modifier = Modifier.scale(0.8f)
                   )
                }
            }
            Text(
                text = mutation.name,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) Color.White else Color.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "x${mutation.scoreMultiplier}",
                fontSize = 12.sp,
                color = if (mutation.scoreMultiplier < 1.0f) Color.Green else Color.Red
            )
            if (!isUnlocked) {
                 Text(
                    text = "500",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD35400)
                )
            }
        }
    }
}

@Composable
fun BadgeShopCard(
    badge: GameViewModel.Badge,
    isOwned: Boolean,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f) 
            .clickable(enabled = !isOwned, onClick = onBuy),
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
                painter = painterResource(id = badge.iconResId),
                contentDescription = badge.name,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(4.dp)
            )
            
            Text(
                text = badge.name,
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
                    text = "${badge.cost}",
                    fontSize = 12.sp,
                    color = Color(0xFFD35400),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
