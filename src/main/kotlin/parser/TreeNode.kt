package parser

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

data class TreeNode(val type: String, val left: TreeNode? = null, val right: TreeNode? = null, val value: Any? = null, val arguments: MutableList<TreeNode> = mutableListOf()) {
    override fun toString(): String {
        return json().toString()
    }

    fun json(): JsonObject {
        val json = JsonObject()
        json.addProperty("type", type)
        if (value != null) json.add("value", valueJson(value))
        if (left != null) json.add("left", left.json())
        if (right != null) json.add("right", right.json())
        if (arguments.isNotEmpty()) json.add("arguments", listJson(arguments))
        return json
    }

    fun listJson(list: List<*>): JsonArray {
        val arr = JsonArray()
        for (entry in list) {
            arr.add(valueJson(entry ?:" null"))
        }
        return arr
    }

    fun mapJson(map: Map<*, *>): JsonObject {
        val obj = JsonObject()
        for (entry in map) {
            obj.add(entry.toString(), valueJson(entry.value ?: "null"))
        }
        return obj
    }

    fun valueJson(value: Any): JsonElement {
        return if (value is Number) JsonPrimitive(value.toDouble())
        else if (value is List<*>) listJson(value)
        else if (value is Map<*, *>) mapJson(value)
        else if (value is TreeNode) value.json()
        else JsonPrimitive(value.toString())
    }
}