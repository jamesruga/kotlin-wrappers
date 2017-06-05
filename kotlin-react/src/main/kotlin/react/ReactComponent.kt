package react

import kotlinext.js.*

external interface RProps
val RProps.children: Any get() = asDynamic().children
var RProps.key: String
    get() = error("key cannot be read from props")
    set(value) {
        asDynamic().key = value
    }
var RProps.ref: (dynamic) -> Unit
    get() = error("ref cannot be read from props")
    set(value) {
        asDynamic().ref = value
    }


external interface RState

class BoxedState<T>(var state: T) : RState

external interface RContext

external interface RClass<in P : RProps> {
    var displayName: String?
}

external interface ReactUpdater {
    fun enqueueSetState(dest: Any, state: Any?)
    fun enqueueReplaceState(dest: Any, state: Any?)
    fun enqueueCallback(dest: Any, callback: Any, method: String)
}

fun <S: RState> React.Component<*, S>.setState(buildState: S.() -> Unit) =
    setState({ assign(it, buildState) })

inline fun <P: RProps> rFunction(
    displayName: String,
    crossinline render: RBuilder.(P) -> ReactElement?
): RClass<P> {
    val fn = { props: P -> buildElement { render(props) } } as RClass<P>
    fn.displayName = displayName
    return fn
}

abstract class RComponent<P: RProps, S: RState> : React.Component<P, S> {
    constructor(): super() {
        state = jsObject { init() }
    }
    constructor(props: P): super(props) {
        state = jsObject { init(props) }
    }

    open fun S.init() {}
    // if you use this one, don't forget to pass props to constructor
    open fun S.init(props: P) {}

    fun RBuilderMultiple.children() {
        childList.addAll(React.Children.toArray(props.children))
    }

    abstract fun RBuilder.render(): ReactElement?

    override fun render(): ReactElement? = buildElement { render() }
}
