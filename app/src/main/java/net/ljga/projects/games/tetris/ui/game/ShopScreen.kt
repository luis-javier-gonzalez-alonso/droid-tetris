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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    var showConfirmDialog by remember { mutableStateOf<Any?>(null) }

    // Dark Background Gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
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
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Back")
                }
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CA1AF)),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_badge_coin),
                            contentDescription = "Coins",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$coins",
                            color = Color(0xFFFFD700),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Styled Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.Cyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF00E5FF)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Mutations", color = if (selectedTab == 0) Color.White else Color.Gray, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Badges", color = if (selectedTab == 1) Color.White else Color.Gray, fontWeight = FontWeight.Bold) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Mutations Shop
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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
            containerColor = Color(0xFF2C3E50),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text(text = "Purchase $name?") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Cost: $cost Coins", fontSize = 18.sp, color = Color(0xFFFFD700))
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
                    enabled = coins >= cost,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60))
                ) {
                    Text("Purchase")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) {
                    Text("Cancel", color = Color.Gray)
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
            containerColor = if (isUnlocked) Color(0xFF1A1A1A) else Color(0xFF2C3E50).copy(alpha = 0.8f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUnlocked && isEnabled) Color(0xFF00E5FF) else if (isUnlocked) Color.Gray else Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = mutation.iconResId),
                    contentDescription = mutation.name,
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.Center)
                )
                if (isUnlocked) {
                   Switch(
                       checked = isEnabled,
                       onCheckedChange = onToggle,
                       modifier = Modifier.scale(0.7f).align(Alignment.TopEnd),
                       colors = SwitchDefaults.colors(
                           checkedThumbColor = Color(0xFF00E5FF),
                           checkedTrackColor = Color(0xFF004D40)
                       )
                   )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mutation.name,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                minLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "x${mutation.scoreMultiplier}",
                fontSize = 12.sp,
                color = if (mutation.scoreMultiplier < 1.0f) Color(0xFF2ECC71) else Color(0xFFE74C3C)
            )
            if (!isUnlocked) {
                 Spacer(modifier = Modifier.height(4.dp))
                 Text(
                    text = "500",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
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
            .clickable(enabled = !isOwned, onClick = onBuy),
        colors = CardDefaults.cardColors(
            containerColor = if (isOwned) Color(0xFF1A1A1A) else Color(0xFF2C3E50).copy(alpha = 0.8f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOwned) Color.Green else Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = badge.iconResId),
                contentDescription = badge.name,
                modifier = Modifier
                    .size(64.dp)
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = badge.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isOwned) {
                Text(
                    text = "OWNED",
                    fontSize = 10.sp,
                    color = Color(0xFF2ECC71),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "${badge.cost}",
                    fontSize = 12.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
