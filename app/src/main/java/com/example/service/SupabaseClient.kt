package com.example.service

import android.util.Log
import com.example.data.NixiStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseClient(private val storageManager: NixiStorageManager) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchTableData(tableName: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val config = storageManager.config.value
        if (config.supabaseUrl.isBlank() || config.supabaseAnonKey.isBlank()) {
            return@withContext storageManager.getTableRows(tableName)
        }

        try {
            val url = "${config.supabaseUrl.trimEnd('/')}/rest/v1/$tableName?select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", config.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${config.supabaseAnonKey}")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val array = JSONArray(body)
                val list = mutableListOf<Map<String, Any>>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val map = mutableMapOf<String, Any>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        map[k] = obj.get(k)
                    }
                    list.add(map)
                }
                return@withContext list
            } else {
                Log.w("SupabaseClient", "Supabase error ${response.code}: $body. Falling back to local cache.")
                return@withContext storageManager.getTableRows(tableName)
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Fetch failed, using local", e)
            return@withContext storageManager.getTableRows(tableName)
        }
    }

    suspend fun insertRecord(tableName: String, record: Map<String, Any>): Boolean = withContext(Dispatchers.IO) {
        // Always store in local manager first
        storageManager.insertRow(tableName, record)

        val config = storageManager.config.value
        if (config.supabaseUrl.isBlank() || config.supabaseAnonKey.isBlank()) {
            return@withContext true
        }

        try {
            val url = "${config.supabaseUrl.trimEnd('/')}/rest/v1/$tableName"
            val jsonBody = JSONObject(record).toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", config.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${config.supabaseAnonKey}")
                .addHeader("Prefer", "return=representation")
                .post(jsonBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Insert remote failed", e)
            false
        }
    }

    suspend fun updateRecord(tableName: String, rowId: String, updatedValues: Map<String, Any>): Boolean = withContext(Dispatchers.IO) {
        storageManager.updateRow(tableName, rowId, updatedValues)

        val config = storageManager.config.value
        if (config.supabaseUrl.isBlank() || config.supabaseAnonKey.isBlank()) {
            return@withContext true
        }

        try {
            val url = "${config.supabaseUrl.trimEnd('/')}/rest/v1/$tableName?id=eq.$rowId"
            val jsonBody = JSONObject(updatedValues).toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", config.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${config.supabaseAnonKey}")
                .patch(jsonBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Update remote failed", e)
            false
        }
    }

    suspend fun deleteRecord(tableName: String, rowId: String): Boolean = withContext(Dispatchers.IO) {
        storageManager.deleteRow(tableName, rowId)

        val config = storageManager.config.value
        if (config.supabaseUrl.isBlank() || config.supabaseAnonKey.isBlank()) {
            return@withContext true
        }

        try {
            val url = "${config.supabaseUrl.trimEnd('/')}/rest/v1/$tableName?id=eq.$rowId"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", config.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${config.supabaseAnonKey}")
                .delete()
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Delete remote failed", e)
            false
        }
    }
}
