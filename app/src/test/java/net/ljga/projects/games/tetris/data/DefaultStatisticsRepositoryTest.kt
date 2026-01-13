/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ljga.projects.games.tetris.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import net.ljga.projects.games.tetris.data.local.database.Statistics
import net.ljga.projects.games.tetris.data.local.database.StatisticsDao

/**
 * Unit tests for [DefaultStatisticsRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class) // TODO: Remove when stable
class DefaultStatisticsRepositoryTest {

    @Test
    fun statisticss_newItemSaved_itemIsReturned() = runTest {
        val repository = DefaultStatisticsRepository(FakeStatisticsDao())

        repository.add("Repository")

        assertEquals(repository.statisticss.first().size, 1)
    }

}

private class FakeStatisticsDao : StatisticsDao {

    private val data = mutableListOf<Statistics>()

    override fun getStatisticss(): Flow<List<Statistics>> = flow {
        emit(data)
    }

    override suspend fun insertStatistics(item: Statistics) {
        data.add(0, item)
    }
}
