package dfk.template

import com.google.gson.JsonObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Socket

class Recode {

    companion object {
        fun sendTemplate(templates: List<DFTemplate>) {
            val socket = Socket("localhost", 31371)
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
            for (template in templates) {
                val data = JsonObject()
                data.addProperty("name", "§b§lDFL §3» §bTemplate")
                data.addProperty("version", 1)
                data.addProperty("data", template.compressed())

                val obj = JsonObject()
                obj.addProperty("type", "template")
                obj.addProperty("data", data.toString())
                obj.addProperty("source", "Source")

                writer.write(obj.toString())
            }
            writer.flush()
            writer.close()
            socket.close()
        }
    }

}