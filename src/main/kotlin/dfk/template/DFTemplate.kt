package dfk.template

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dfk.codeblock.DFCodeBlock
import dfk.codeblock.DFCodeType
import dfk.item.DFVarType
import dfk.item.DFVariable
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream


class DFTemplate {
    val codeBlocks: MutableList<DFCodeBlock> = mutableListOf()

    fun addCodeBlock(cb: DFCodeBlock) {
        codeBlocks.add(cb)
    }

    fun getJson(): JsonObject {
        val json = JsonObject()

        val blocks = JsonArray()
        for (cb in codeBlocks) {
            val cbJson = JsonObject()

            if (cb.type == DFCodeType.BRACKET) {
                cbJson.addProperty("id", "bracket")
                cbJson.addProperty("direct", if (cb.bracketOpening) "open" else "close")
                cbJson.addProperty("type", if (cb.bracketRepeating) "repeat" else "norm")

                blocks.add(cbJson)
                continue
            }

            cbJson.addProperty("id", "block")
            cbJson.addProperty("block", cb.type.jsonName)

            if (cb.type == DFCodeType.FUNCTION ||
                cb.type == DFCodeType.CALL_FUNCTION ||
                cb.type == DFCodeType.PROCESS ||
                cb.type == DFCodeType.START_PROCESS) {
                cbJson.addProperty("data", cb.action)
            } else cbJson.addProperty("action", cb.action)

            val argsJson = JsonObject()
            val itemsArr = JsonArray()

            for (entry in cb.contents) {
                val slot = entry.key
                val item = entry.value

                val itemJson = JsonObject()
                val itemPropertiesObject = JsonObject()
                itemPropertiesObject.addProperty("id", item.type.id)

                val itemDataPropertiesObject = JsonObject()

                when (item.type) {
                    DFVarType.NUMBER, DFVarType.STRING, DFVarType.STYLED_TEXT -> {
                        itemDataPropertiesObject.addProperty("name", item.value.toString())
                    }
                    DFVarType.VARIABLE -> {
                        item as DFVariable
                        item.value as Map<String, *>

                        itemDataPropertiesObject.addProperty("name", item.value["name"] as String)
                        itemDataPropertiesObject.addProperty("scope", (item.value["scope"] as DFVariable.VariableScope).jsonName)
                    }
                    DFVarType.GAME_VALUE -> {
                        item.value as Map<String, String>

                        itemDataPropertiesObject.addProperty("type", item.value["type"])
                        itemDataPropertiesObject.addProperty("target", item.value["target"])
                    }
                    else -> throw IllegalStateException("Unsupported item type ${item.type}")
                }

                itemPropertiesObject.add("data", itemDataPropertiesObject)
                itemJson.add("item", itemPropertiesObject)
                itemJson.addProperty("slot", slot)

                itemsArr.add(itemJson)
            }

            argsJson.add("items", itemsArr)
            cbJson.add("args", argsJson)

            if (cb.target != "") {
                cbJson.addProperty("target", cb.target)
                cbJson.addProperty("subAction", cb.target)
            }

            if (cb.inverter != "") {
                cbJson.addProperty("inverted", cb.inverter)
            }

            blocks.add(cbJson)
        }
        json.add("blocks", blocks)

        return json
    }

    fun compressed(): String {
        return java.util.Base64.getEncoder().encodeToString(compress(getJson().toString()))
    }

    private fun compress(stringToCompress: String): ByteArray {
        try {
            ByteArrayOutputStream().use { baos ->
                GZIPOutputStream(baos).use { gzipOutput ->
                    gzipOutput.write(stringToCompress.toByteArray(StandardCharsets.UTF_8))
                    gzipOutput.finish()
                    return baos.toByteArray()
                }
            }
        } catch (e: IOException) {
            throw UncheckedIOException("Error while compression!", e)
        }
    }

}