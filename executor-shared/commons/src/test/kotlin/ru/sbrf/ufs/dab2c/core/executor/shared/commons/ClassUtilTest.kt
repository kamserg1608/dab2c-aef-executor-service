package ru.sbrf.ufs.dab2c.core.executor.shared.commons

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.util.ClassUtil

class ClassUtilTest {

    @Test
    fun `test getJavaClassHierarchy with simple class`() {
        val clazz = SimpleClass::class.java
        val hierarchy = ClassUtil.getJavaClassHierarchy(clazz)
        assertEquals(
            listOf(SimpleClass::class.java, Object::class.java),
            hierarchy
        )
    }

    @Test
    fun `test getJavaClassHierarchy with class that implements interface`() {
        val clazz = ClassWithInterface::class.java
        val hierarchy = ClassUtil.getJavaClassHierarchy(clazz)
        assertEquals(
            listOf(ClassWithInterface::class.java, Object::class.java, MyInterface::class.java),
            hierarchy
        )
    }

    @Test
    fun `test getJavaClassHierarchy with class that extends another class`() {
        val clazz = SubClass::class.java
        val hierarchy = ClassUtil.getJavaClassHierarchy(clazz)
        assertEquals(
            listOf(SubClass::class.java, SuperClass::class.java, Object::class.java),
            hierarchy
        )
    }

    @Test
    @Suppress("FunctionMaxLength")
    fun `test getJavaClassHierarchy with class that extends another class and implements interface`() {
        val clazz = SubClassWithInterface::class.java
        val hierarchy = ClassUtil.getJavaClassHierarchy(clazz)
        assertEquals(
            listOf(
                SubClassWithInterface::class.java,
                SuperClass::class.java,
                Object::class.java,
                MyInterface::class.java
            ),
            hierarchy
        )
    }

    @Test
    fun `test getJavaClassHierarchy with Object class`() {
        val clazz = Object::class.java
        val hierarchy = ClassUtil.getJavaClassHierarchy(clazz)
        assertEquals(listOf(Object::class.java), hierarchy)
    }
}

open class SuperClass
class SubClass : SuperClass()
interface MyInterface
class ClassWithInterface : MyInterface
class SubClassWithInterface : SuperClass(), MyInterface
class SimpleClass
