package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonHelper {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun <T> toJson(data: T, clazz: Class<T>): String {
        return moshi.adapter(clazz).toJson(data)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T? {
        return try {
            moshi.adapter(clazz).fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun <T> listToJson(list: List<T>, clazz: Class<T>): String {
        val type = Types.newParameterizedType(List::class.java, clazz)
        return moshi.adapter<List<T>>(type).toJson(list)
    }

    fun <T> listFromJson(json: String, clazz: Class<T>): List<T> {
        if (json.isEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, clazz)
        return try {
            moshi.adapter<List<T>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
