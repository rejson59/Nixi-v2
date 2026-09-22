package com.example.service

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.NixiStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class NixiEngineResponse {
    data class VoiceResponse(val speechText: String, val actionNotice: String? = null) : NixiEngineResponse()
    data class ToolCall(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val preVoiceConfirmation: String? = null
    ) : NixiEngineResponse()
    data class ScreenControlRequested(val message: String) : NixiEngineResponse()
    data class Error(val message: String) : NixiEngineResponse()
}

class NixiGeminiEngine(
    private val context: Context,
    private val storageManager: NixiStorageManager
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun processUserSpeech(userUtterance: String): NixiEngineResponse = withContext(Dispatchers.IO) {
        val config = storageManager.config.value
        val effectiveApiKey = when {
            config.geminiApiKey.isNotBlank() -> config.geminiApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        // Fast local routine / quick command match check to save TPM and battery!
        val normalized = userUtterance.lowercase(Locale.getDefault()).trim()
        val routineMatch = checkLocalRoutines(normalized)
        if (routineMatch != null) {
            return@withContext routineMatch
        }

        if (effectiveApiKey.isBlank()) {
            return@withContext NixiEngineResponse.VoiceResponse(
                speechText = "Szefie, aby móc w pełni odpowiadać i działać, proszę podać klucz API Gemini w ustawieniach NIXI.",
                actionNotice = "Brak klucza API Gemini"
            )
        }

        // Estimate tokens: roughly 1 token per 3 chars
        val estimatedTokens = (userUtterance.length / 3) + 350
        if (!storageManager.canConsumeTokens(estimatedTokens)) {
            return@withContext NixiEngineResponse.VoiceResponse(
                speechText = "Szefie, zbliżamy się do limitu tokenów na minutę. Zwalniam procesy, aby nie przekroczyć budżetu.",
                actionNotice = "Ochrona TPM aktywna"
            )
        }

        try {
            val systemPrompt = buildSystemPrompt()
            val requestJson = JSONObject().apply {
                // Contents
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", userUtterance))
                        })
                    })
                })
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                // Generation Config
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 600)
                })
                // Tools declaration
                put("tools", buildToolsDeclarations())
            }

            val modelName = if (config.geminiModel.isNotBlank()) config.geminiModel else "gemini-3.5-flash"
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$effectiveApiKey"

            val body = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("NixiGeminiEngine", "Gemini HTTP error ${response.code}: $responseBody")
                return@withContext NixiEngineResponse.VoiceResponse(
                    speechText = "Wystąpił problem z połączeniem, Szefie. Kod błędu: ${response.code}.",
                    actionNotice = "Błąd API Gemini ${response.code}"
                )
            }

            storageManager.recordTokenUsage(estimatedTokens)

            val parsedResponse = JSONObject(responseBody)
            val candidates = parsedResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts == null || parts.length() == 0) {
                return@withContext NixiEngineResponse.VoiceResponse(
                    speechText = "Słucham, Szefie. W czym mogę pomóc?"
                )
            }

            // Check if there is a functionCall
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("functionCall")) {
                    val functionCall = part.getJSONObject("functionCall")
                    val fnName = functionCall.optString("name")
                    val fnArgs = functionCall.optJSONObject("args")
                    val argsMap = mutableMapOf<String, Any?>()
                    if (fnArgs != null) {
                        val keys = fnArgs.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            argsMap[k] = fnArgs.get(k)
                        }
                    }
                    return@withContext NixiEngineResponse.ToolCall(
                        toolName = fnName,
                        arguments = argsMap
                    )
                }
            }

            // Otherwise extract text response
            val speechText = parts.getJSONObject(0).optString("text", "Tak, Szefie?")
            // Sanitize markdown tags for spoken audio
            val sanitized = sanitizeForVoice(speechText)
            NixiEngineResponse.VoiceResponse(speechText = sanitized)

        } catch (e: Exception) {
            Log.e("NixiGeminiEngine", "Error calling Gemini", e)
            storageManager.logAction("ERROR", "Błąd zapytania AI", e.message ?: "Nieznany błąd", isError = true)
            NixiEngineResponse.VoiceResponse(
                speechText = "Przepraszam Szefie, napotkałam niespodziewany błąd sieciowy.",
                actionNotice = "Błąd sieci: ${e.localizedMessage}"
            )
        }
    }

    private fun checkLocalRoutines(userUtterance: String): NixiEngineResponse? {
        val routines = storageManager.routines.value
        val matched = routines.firstOrNull { it.isEnabled && userUtterance.contains(it.triggerPhrase.lowercase()) }
        if (matched != null) {
            if (matched.id == "r1") { // Dzień dobry routine
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val calendar = storageManager.calendarEvents.value.firstOrNull()
                val nextLessonText = if (calendar != null) {
                    val eventTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(calendar.startTimeEpochMs))
                    "Twoje pierwsze zaplanowane zajęcia to ${calendar.title} o godzinie $eventTime."
                } else {
                    "Nie masz dziś wcześnie zaplanowanych lekcji."
                }
                val speech = "Dzień dobry, Szefie! Jest godzina $timeStr. Pamiętaj, że każdy dzień to nowa szansa na wielkie osiągnięcia. $nextLessonText Życzę owocnego dnia!"
                return NixiEngineResponse.VoiceResponse(speechText = speech, actionNotice = "Wykonano rutynę: ${matched.name}")
            } else if (matched.id == "r2") { // Idę spać
                val activeAlarm = storageManager.alarms.value.firstOrNull { it.isEnabled }
                val alarmInfo = if (activeAlarm != null) "Twój budzik jest ustawiony na ${activeAlarm.hour}:${String.format("%02d", activeAlarm.minute)}." else "Nie masz aktywnego budzika na jutro."
                return NixiEngineResponse.VoiceResponse(speechText = "Spokojnej nocy, Szefie. Wyciszam powiadomienia. $alarmInfo Odpocznij dobrze!", actionNotice = "Wykonano rutynę: ${matched.name}")
            }
        }

        // Fast actions (offline / zero-latency)
        if (userUtterance.contains("otwórz spotify") || userUtterance.contains("włącz spotify")) {
            return NixiEngineResponse.ToolCall(
                toolName = "spotify_control",
                arguments = mapOf("action" to "open"),
                preVoiceConfirmation = "Otwieram Spotify, Szefie."
            )
        }
        if (userUtterance.contains("zatrzymaj muzykę") || userUtterance.contains("pauza") || userUtterance.contains("wyłącz muzykę")) {
            return NixiEngineResponse.ToolCall(
                toolName = "spotify_control",
                arguments = mapOf("action" to "pause"),
                preVoiceConfirmation = "Zatrzymuję muzykę, Szefie."
            )
        }
        if (userUtterance.contains("wznów muzykę") || userUtterance.contains("graj dalej") || userUtterance.contains("play")) {
            return NixiEngineResponse.ToolCall(
                toolName = "spotify_control",
                arguments = mapOf("action" to "resume"),
                preVoiceConfirmation = "Wznawiam odtwarzanie, Szefie."
            )
        }
        if (userUtterance.contains("następny utwór") || userUtterance.contains("przełącz piosenkę") || userUtterance.contains("następna piosenka")) {
            return NixiEngineResponse.ToolCall(
                toolName = "spotify_control",
                arguments = mapOf("action" to "next"),
                preVoiceConfirmation = "Następny utwór, Szefie."
            )
        }
        if (userUtterance.contains("poprzedni utwór") || userUtterance.contains("poprzednia piosenka")) {
            return NixiEngineResponse.ToolCall(
                toolName = "spotify_control",
                arguments = mapOf("action" to "previous"),
                preVoiceConfirmation = "Poprzedni utwór, Szefie."
            )
        }
        if (userUtterance.contains("tryb ręczny") || userUtterance.contains("przejmij ekran") || userUtterance.contains("kontroluj ekran")) {
            return NixiEngineResponse.ScreenControlRequested("Przechodzę w tryb ręczny i podświetlam ramki ekranu, Szefie.")
        }
        return null
    }

    private fun sanitizeForVoice(text: String): String {
        return text
            .replace(Regex("[*#_`>]"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .replace(Regex("\\n+"), " ")
            .trim()
    }

    private fun buildSystemPrompt(): String {
        val config = storageManager.config.value
        val allowedTables = config.allowedSupabaseTables.joinToString(", ")
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        return """
            Jesteś NIXI — profesjonalną, lojalną i wysoce inteligentną osobistą asystentką AI działającą w telefonie swojego użytkownika.
            Aktualny czas: $currentTime.
            
            Kluczowe zasady zachowania:
            1. Zawsze zwracaj się do użytkownika per "Szefie".
            2. Twoje odpowiedzi są bezpośrednio ODCZYTYWANE GŁOSOWO przez syntezator mowy (TTS). Nigdy nie używaj gwiazdek, pogrubień Markdown (**tekst**), nagłówków ani list punktowanych. Mów czystym, płynnym tekstem do wymówienia.
            3. Zasada zwięzłości przy akcjach: Jeśli Szef prosi o prostą akcję (np. włączenie muzyki, dodanie budzika, dodanie wpisu, sprawdzenie czegoś), odpowiedz bardzo krótko i konkretnie, np.: "Otwieram Spotify, Szefie.", "Budzik został ustawiony na siódmą rano, Szefie.", "Dodałam wydarzenie do kalendarza, Szefie.".
            4. Kiedy Szef prowadzi swobodną rozmowę lub zadaje pytania, rozmawiaj naturalnie, inteligentnie, z szacunkiem i profesjonalizmem.
            5. Dostępne tabele w Supabase z uprawnieniami dla NIXI: [$allowedTables].
            6. Jeśli Szef prosi o akcję na ekranie lub nie masz dedykowanego narzędzia, możesz wywołać 'manual_screen_control' aby przejść w tryb ręczny z podświetlonymi ramkami ekranu.
        """.trimIndent()
    }

    private fun buildToolsDeclarations(): JSONArray {
        return JSONArray().apply {
            put(JSONObject().apply {
                put("functionDeclarations", JSONArray().apply {
                    // Spotify Tool
                    put(JSONObject().apply {
                        put("name", "spotify_control")
                        put("description", "Sterowanie odtwarzaniem w Spotify: odtwarzanie, pauza, następny, poprzedni, sprawdzanie utworu.")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("action", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Akcja: play, pause, next, previous, get_current, open")
                                })
                                put("query", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Opcjonalna nazwa utworu lub wykonawcy do wyszukania")
                                })
                            })
                            put("required", JSONArray().apply { put("action") })
                        })
                    })

                    // Supabase Data Tool
                    put(JSONObject().apply {
                        put("name", "supabase_action")
                        put("description", "Operacje na bazie Supabase (pamięć długotrwała): odczyt, dodawanie, modyfikacja danych w tabelach.")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("action", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Akcja: read, insert, update, delete")
                                })
                                put("tableName", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Nazwa tabeli (np. user_profile, conversations_memory, reminders, calendar_events)")
                                })
                                put("recordData", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Dane w formacie JSON do zapisu lub aktualizacji")
                                })
                            })
                            put("required", JSONArray().apply { put("action"); put("tableName") })
                        })
                    })

                    // Alarm Tool
                    put(JSONObject().apply {
                        put("name", "alarm_control")
                        put("description", "Ustawianie lub sprawdzanie budzików w systemie telefonu.")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("action", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Akcja: set, show, dismiss")
                                })
                                put("hour", JSONObject().apply {
                                    put("type", "INTEGER")
                                    put("description", "Godzina budzika (0-23)")
                                })
                                put("minute", JSONObject().apply {
                                    put("type", "INTEGER")
                                    put("description", "Minuta budzika (0-59)")
                                })
                                put("label", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Etykieta budzika")
                                })
                            })
                            put("required", JSONArray().apply { put("action") })
                        })
                    })

                    // Calendar Tool
                    put(JSONObject().apply {
                        put("name", "calendar_action")
                        put("description", "Zarządzanie wewnętrznym kalendarzem NIXI połączonym z Supabase.")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("action", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Akcja: add, list, delete")
                                })
                                put("title", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Tytuł wydarzenia")
                                })
                                put("timeInfo", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Data lub godzina wydarzenia")
                                })
                                put("description", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Opis wydarzenia")
                                })
                            })
                            put("required", JSONArray().apply { put("action") })
                        })
                    })

                    // Screen control
                    put(JSONObject().apply {
                        put("name", "manual_screen_control")
                        put("description", "Włącza tryb ręczny ze świecącymi ramkami ekranu gdy NIXI ma bezpośrednio wchodzić w interakcję z ekranem.")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("reason", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Powód przejścia w tryb ekranowy")
                                })
                            })
                        })
                    })
                })
            })
        }
    }
}
