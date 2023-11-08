package lexer

import com.google.gson.JsonObject

class Token(var type: TokenType, var value: Any) {
    override fun toString(): String {
        val json = JsonObject()
        json.addProperty("type", type.id)
        if (value is Number) json.addProperty("value", (value as Number).toDouble())
        else json.addProperty("value", value.toString())
        return json.toString()
    }
}