package ru.developer.press.myearningkot.helpers

import android.icu.lang.UCharacter
import com.google.gson.*
import ru.developer.press.myearningkot.model.Column
import java.lang.reflect.Type

inline fun <reified T> clone(source: T): T {
    val stringProject = Gson().toJson(source, UCharacter.GraphemeClusterBreak.T::class.java)
    return Gson().fromJson<T>(stringProject, UCharacter.GraphemeClusterBreak.T::class.java)
}

inline fun <reified T> Any.equalByGson(equalObject: T): Boolean {
    val sourceAny = Gson().toJson(equalObject, UCharacter.GraphemeClusterBreak.T::class.java)
    val any = Gson().toJson(this, UCharacter.GraphemeClusterBreak.T::class.java)
    return sourceAny == any
}


const val column_cast_gson = "col_cast_gson"
const val column_type_gson = "col_type_gson"

class JsonDeserializerWithColumn : JsonDeserializer<Column> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext
    ): Column {
        val jsonObject = json.asJsonObject
        val classNamePrimitive = jsonObject[column_cast_gson] as JsonPrimitive
        val className = classNamePrimitive.asString
        val clazz: Class<*> = try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            throw JsonParseException(e.message)
        }
        return context.deserialize(jsonObject, clazz)
    }
}