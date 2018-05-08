package com.example.xiaodong.testvideo

import java.util.LinkedHashMap

data class Camera(val name: String, val source: String,
                  val dests: LinkedHashMap<String, String> = LinkedHashMap<String, String>()
) {
    override fun equals(other: Any?): Boolean {
        when(other) {
            is Camera -> return name == other.name
            else -> return false
        }
    }
}