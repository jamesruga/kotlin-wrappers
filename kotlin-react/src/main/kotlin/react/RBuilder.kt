package react

import kotlinext.js.*
import kotlin.reflect.*

@DslMarker
annotation class ReactDsl

@ReactDsl
abstract class RBuilder {
    abstract fun child(element: ReactElement): ReactElement

    fun <P: RProps> child(type: Any, props: P, children: List<Any>) =
        child(React.createElement(type, props, *children.toTypedArray()))

    fun <P : RProps> child(type: Any, props : P, handler: RHandler<P>): ReactElement {
        val children = with(RElementBuilder(props)) {
            handler()
            childList
        }
        return child(type, props, children)
    }

    operator fun <P : RProps> RClass<P>.invoke(handler: RHandler<P>) =
        child(this, jsObject {}, handler)

    fun <P : RProps> RClass<P>.node(
        props: P,
        children: List<Any> = emptyList()
    ) = child(this, clone(props), children)

    fun <P : RProps, C: React.Component<P, *>> child(klazz: KClass<C>, handler: RHandler<P>): ReactElement {
        val rClass = klazz.js as RClass<P>
        return rClass(handler)
    }

    inline fun <P: RProps, reified C: React.Component<P, *>> child(noinline handler: RHandler<P>) =
        child(C::class, handler)

    fun <P : RProps, C: React.Component<P, *>> node(
        klazz: KClass<C>,
        props: P,
        children: List<Any> = emptyList()
    ): ReactElement {
        val rClass = klazz.js as RClass<P>
        return rClass.node(props, children)
    }

    inline fun <P: RProps, reified C: React.Component<P, *>> node(props: P, children: List<Any> = emptyList()) =
        node(C::class, props, children)
}

open class RBuilderMultiple : RBuilder() {
    val childList = mutableListOf<Any>()

    override fun child(element: ReactElement): ReactElement {
        childList.add(element)
        return element
    }

    operator fun String.unaryPlus() {
        childList.add(this)
    }
}

fun buildElements(handler: RBuilderMultiple.() -> Unit): List<Any> =
    RBuilderMultiple().apply(handler).childList

open class RBuilderSingle : RBuilder() {
    var result: ReactElement? = null

    override fun child(element: ReactElement): ReactElement {
        if (result != null) {
            throw IllegalStateException("React only allows single element be returned from render() function")
        }

        result = element
        return element
    }
}

inline fun buildElement(handler: RBuilder.() -> Unit): ReactElement? =
    RBuilderSingle().apply(handler).result

open class RElementBuilder<out P : RProps>(open val attrs: P) : RBuilderMultiple() {
    fun attrs(handler: P.() -> Unit) {
        attrs.handler()
    }

    var key: String
        get() = attrs.key
        set(value) {
            attrs.key = value
        }

    fun ref(handler: (dynamic) -> Unit) {
        attrs.ref = handler
    }
}

typealias RHandler<P> = RElementBuilder<P>.() -> Unit
