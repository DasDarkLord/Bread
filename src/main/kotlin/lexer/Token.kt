package lexer

import com.google.gson.JsonObject

class Token(val type: TokenType, val value: Any) {
    override fun toString(): String {
        val json = JsonObject()
        json.addProperty("type", type.id)
        if (value is Number) json.addProperty("value", value.toDouble())
        else json.addProperty("value", value.toString())
        return json.toString()
    }
}