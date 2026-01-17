package net.ljga.projects.games.tetris.ui.game

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import net.ljga.projects.games.tetris.ui.game.GameViewModel.GameState
import kotlin.reflect.KClass

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class RuntimeTypeAdapterFactory<T : Any>(
    private val baseType: KClass<T>,
    private val typeFieldName: String = "type"
) : TypeAdapterFactory {

    private val labelToSubtype = mutableMapOf<String, KClass<out T>>()
    private val subtypeToLabel = mutableMapOf<KClass<out T>, String>()

    fun registerSubtype(subtype: KClass<out T>, label: String = subtype.simpleName!!): RuntimeTypeAdapterFactory<T> {
        if (subtypeToLabel.containsKey(subtype) || labelToSubtype.containsKey(label)) {
            throw IllegalArgumentException("Types and labels must be unique.")
        }
        labelToSubtype[label] = subtype
        subtypeToLabel[subtype] = label
        return this
    }

    override fun <R : Any> create(gson: Gson, type: TypeToken<R>): TypeAdapter<R>? {
        if (type.rawType != baseType.java) {
            return null
        }

        val labelToDelegate = mutableMapOf<String, TypeAdapter<out T>>()
        val subtypeToDelegate = mutableMapOf<KClass<out T>, TypeAdapter<out T>>()

        for ((label, subtype) in labelToSubtype) {
            val delegate = gson.getDelegateAdapter(this, TypeToken.get(subtype.java))
            labelToDelegate[label] = delegate
            subtypeToDelegate[subtype] = delegate
        }

        return object : TypeAdapter<R>() {
            override fun write(out: JsonWriter, value: R) {
                val subtype = (value as Any)::class
                val label = subtypeToLabel[subtype] ?: throw IllegalArgumentException("cannot serialize ${subtype.simpleName}; did you forget to register it?")
                @Suppress("UNCHECKED_CAST")
                val delegate = subtypeToDelegate[subtype] as TypeAdapter<R>? ?: throw IllegalArgumentException("cannot serialize ${subtype.simpleName}; did you forget to register it?")
                val jsonObject = delegate.toJsonTree(value).asJsonObject
                if (jsonObject.has(typeFieldName)) {
                    throw IllegalArgumentException("cannot serialize ${subtype.simpleName} because it already defines a field named $typeFieldName")
                }
                jsonObject.addProperty(typeFieldName, label)
                gson.toJson(jsonObject, out)
            }

            override fun read(`in`: JsonReader): R? {
                val jsonObject = gson.getAdapter(JsonElement::class.java).read(`in`).asJsonObject
                val labelJsonElement = jsonObject.remove(typeFieldName) ?: throw IllegalArgumentException("cannot deserialize ${baseType.simpleName} because it does not define a field named $typeFieldName")
                val label = labelJsonElement.asString
                val delegate = labelToDelegate[label] ?: throw IllegalArgumentException("cannot deserialize ${baseType.simpleName}; unexpected type label '$label'")
                return delegate.fromJsonTree(jsonObject) as R?
            }
        }.nullSafe()
    }
}

class PreferenceDataStore(private val context: Context) {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(
            RuntimeTypeAdapterFactory(Artifact::class)
                .registerSubtype(SwiftnessCharmArtifact::class)
                .registerSubtype(LineClearerArtifact::class)
                .registerSubtype(ScoreMultiplierArtifact::class)
                .registerSubtype(SpringLoadedRotatorArtifact::class)
                .registerSubtype(ChaosOrbArtifact::class)
                .registerSubtype(FallingFragmentsArtifact::class)
                .registerSubtype(BoardWipeArtifact::class)
                .registerSubtype(InvertedRotationArtifact::class)
                .registerSubtype(PieceSwapperArtifact::class)
                .registerSubtype(BoardShrinkerArtifact::class)
        )
        .registerTypeAdapterFactory(
            RuntimeTypeAdapterFactory(Mutation::class)
                .registerSubtype(UnyieldingMutation::class)
                .registerSubtype(FeatherFallMutation::class)
                .registerSubtype(LeadFallMutation::class)
                .registerSubtype(ClairvoyanceMutation::class)
                .registerSubtype(ColorblindMutation::class)
                .registerSubtype(MoreIsMutation::class)
                .registerSubtype(GarbageCollectorMutation::class)
                .registerSubtype(TimeWarpMutation::class)
                .registerSubtype(FairPlayMutation::class)
                .registerSubtype(PhantomPieceMutation::class)
        )
        .create()

    private val highScoreKey = intPreferencesKey("high_score")
    private val gameStateKey = stringPreferencesKey("game_state")
    private val unlockedMutationsKey = stringSetPreferencesKey("unlocked_mutations")
    private val enabledMutationsKey = stringSetPreferencesKey("enabled_mutations")
    private val coinsKey = intPreferencesKey("coins")
    private val ownedBadgesKey = stringSetPreferencesKey("owned_badges")
    
    // Settings Keys
    private val languageCodeKey = stringPreferencesKey("language_code")
    private val isClassicModeKey = booleanPreferencesKey("is_classic_mode")
    private val touchSensitivityKey = floatPreferencesKey("touch_sensitivity")

    val highScore: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[highScoreKey] ?: 0
        }

    val gameState: Flow<GameViewModel.GameState?> = context.dataStore.data
        .map { preferences ->
            val json = preferences[gameStateKey]
            if (json.isNullOrBlank()) {
                return@map null
            }
            try {
                gson.fromJson(json, GameState::class.java)
            } catch (e: Exception) {
                Log.e("PreferenceDataStore", "Failed to deserialize game state, clearing it.", e)
                runBlocking { clearGameState() } // Clear the invalid state
                null // Return null to signal no valid game state
            }
        }

    val unlockedMutations: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[unlockedMutationsKey] ?: emptySet()
        }

    val enabledMutations: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[enabledMutationsKey] ?: emptySet()
        }

    suspend fun updateHighScore(score: Int) {
        context.dataStore.edit {
            it[highScoreKey] = score
        }
    }

    suspend fun saveGameState(gameState: GameViewModel.GameState) {
        context.dataStore.edit {
            val json = gson.toJson(gameState)
            it[gameStateKey] = json
        }
    }

    suspend fun clearGameState() {
        context.dataStore.edit {
            it.remove(gameStateKey)
        }
    }

    suspend fun unlockMutation(mutationName: String) {
        context.dataStore.edit {
            val currentMutations = it[unlockedMutationsKey] ?: emptySet()
            if (!currentMutations.contains(mutationName)) {
                it[unlockedMutationsKey] = currentMutations + mutationName
                // Auto-enable new mutations by default
                val currentEnabled = it[enabledMutationsKey] ?: emptySet()
                it[enabledMutationsKey] = currentEnabled + mutationName
            }
        }
    }

    suspend fun setMutationEnabled(mutationName: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val currentEnabled = prefs[enabledMutationsKey] ?: emptySet()
            prefs[enabledMutationsKey] = if (enabled) {
                currentEnabled + mutationName
            } else {
                currentEnabled - mutationName
            }
        }
    }

    suspend fun clearSavedGame() {
        context.dataStore.edit {
            it.remove(gameStateKey)
        }
    }

    val coins: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[coinsKey] ?: 0
        }

    val ownedBadges: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[ownedBadgesKey] ?: emptySet()
        }

    // Settings Flows
    val languageCode: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[languageCodeKey] ?: "system" }

    val isClassicMode: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[isClassicModeKey] ?: false }

    val touchSensitivity: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[touchSensitivityKey] ?: 2.0f }

    // Settings Updates
    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { it[languageCodeKey] = code }
    }

    suspend fun setClassicMode(enabled: Boolean) {
        context.dataStore.edit { it[isClassicModeKey] = enabled }
    }

    suspend fun setTouchSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[touchSensitivityKey] = sensitivity }
    }

    suspend fun addCoins(amount: Int) {
        context.dataStore.edit {
            val current = it[coinsKey] ?: 0
            it[coinsKey] = current + amount
        }
    }

    suspend fun purchaseBadge(badgeName: String, cost: Int): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val currentCoins = prefs[coinsKey] ?: 0
            val ownedBadges = prefs[ownedBadgesKey] ?: emptySet()
            if (currentCoins >= cost && !ownedBadges.contains(badgeName)) {
                prefs[coinsKey] = currentCoins - cost
                prefs[ownedBadgesKey] = ownedBadges + badgeName
                success = true
            }
        }
        return success
    }

    suspend fun purchaseMutation(mutationName: String, cost: Int): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val currentCoins = prefs[coinsKey] ?: 0
            val unlockedMutations = prefs[unlockedMutationsKey] ?: emptySet()
            if (currentCoins >= cost && !unlockedMutations.contains(mutationName)) {
                prefs[coinsKey] = currentCoins - cost
                prefs[unlockedMutationsKey] = unlockedMutations + mutationName
                // Auto-enable
                val currentEnabled = prefs[enabledMutationsKey] ?: emptySet()
                prefs[enabledMutationsKey] = currentEnabled + mutationName
                success = true
            }
        }
        return success
    }
}
