package ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.util

/**
 * Util class for common classes manipulations.
 */
object ClassUtil {

    /**
     * Method extracts full classes hierarchy from [clazz] in order [clazz] -> superclasses -> interfaces.
     */
    fun getJavaClassHierarchy(clazz: Class<*>): List<Class<*>> {
        val result = mutableListOf(clazz)
        var current: Class<*> = clazz

        while (current != Object::class.java) {
            current = current.superclass
            result.addLast(current)
        }
        result.addAll(clazz.interfaces)

        return result
    }
}
