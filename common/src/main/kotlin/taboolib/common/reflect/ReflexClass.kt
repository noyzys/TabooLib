package taboolib.common.reflect

import taboolib.common.util.nonPrimitive
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * TabooLib
 * taboolib.common.reflect.ReflexClass
 *
 * @author sky
 * @since 2021/6/18 1:59 下午
 */
class ReflexClass(val clazz: Class<*>) {

    private var superclass: ReflexClass? = null
    private var interfaces = ArrayList<ReflexClass>()

    val savingFields = ArrayList<Field>()
    val savingMethods = ArrayList<Method>()
    val savingConstructor = ArrayList<Constructor<*>>()

    init {
        try {
            savingFields.addAll(clazz.declaredFields.map {
                it.isAccessible = true
                it
            })
            savingMethods.addAll(clazz.declaredMethods.map {
                it.isAccessible = true
                it
            })
            savingConstructor.addAll(clazz.declaredConstructors.map {
                it.isAccessible = true
                it
            })
            if (clazz.superclass != null && clazz.superclass != Object::class.java) {
                superclass = ReflexClass(clazz.superclass)
            }
            clazz.interfaces.forEach {
                interfaces.add(ReflexClass(it))
            }
        } catch (ex: Throwable) {
        }
    }

    fun findField(f: String): Field? {
        var field = f
        Reflex.remapper.forEach {
            field = it.field(clazz.name, field)
        }
        savingFields.firstOrNull { it.name == field }?.run {
            return this
        }
        superclass?.findField(field)?.run {
            return this
        }
        interfaces.forEach {
            it.findField(field)?.run {
                return this
            }
        }
        return null
    }

    fun findMethod(m: String, vararg parameter: Any?): Method? {
        var method = m
        Reflex.remapper.forEach {
            method = it.method(clazz.name, method)
        }
        savingMethods.firstOrNull { it.name == method && compare(it.parameterTypes, parameter.map { p -> p?.javaClass }.toTypedArray()) }?.run {
            return this
        }
        superclass?.findMethod(method, *parameter)?.run {
            return this
        }
        interfaces.forEach {
            it.findMethod(method, *parameter)?.run {
                return this
            }
        }
        return null
    }

    fun findConstructor(vararg parameter: Any?): Constructor<*>? {
        savingConstructor.firstOrNull {
            if (it.parameterCount == parameter.size) {
                var checked = true
                it.parameterTypes.forEachIndexed { index, p ->
                    if (parameter[index] != null && !p.nonPrimitive().isInstance(parameter[index])) {
                        checked = false
                    }
                }
                return@firstOrNull checked
            }
            return@firstOrNull false
        }?.run {
            return this
        }
        return null
    }

    fun compare(primary: Array<Class<*>>, secondary: Array<Class<*>?>): Boolean {
        if (primary.size != secondary.size) {
            return false
        }
        for (index in primary.indices) {
            val primaryClass = primary[index].nonPrimitive()
            val secondaryClass = secondary[index]?.nonPrimitive() ?: continue
            if (primaryClass == secondaryClass || primaryClass.isAssignableFrom(secondaryClass)) {
                continue
            }
            return false
        }
        return true
    }

    companion object {

        val savingClass = ConcurrentHashMap<String, ReflexClass>()

        fun find(clazz: Class<*>) = savingClass.computeIfAbsent(clazz.name) { ReflexClass(clazz) }
    }
}