package styled

import kotlinx.css.CssBuilder
import kotlinx.css.properties.KeyframesBuilder
import react.StateInstance
import styled.sheets.CSSOMPersistentSheet
import styled.sheets.DevSheet
import styled.sheets.RuleType
import kotlin.collections.*

internal typealias InjectedCssHolder = LinkedHashMap<StyledCss, ClassName>

/**
 * Inject CSS rules defined in [css] into the DOM
 */
fun injectGlobal(css: CssBuilder) {
    GlobalStyles.sheet.scheduleToInject(css.toStyledCss().getCssRules(null))
    GlobalStyles.sheet.injectScheduled()
}

internal val isDevelopment by lazy {
    js("process.env.NODE_ENV !== 'production'") as Boolean
}

object GlobalStyles {
    internal var sheet = if (isDevelopment) DevSheet() else CSSOMPersistentSheet()

    private var incrementedClassName: Int = 0
        get() {
            field++
            return field
        }

    internal var styledClasses = InjectedCssHolder()
    internal val injectedStyleSheetRules = mutableSetOf<Selector>()

    private fun getInjectedClassName(css: StyledCss): ClassName {
        val className = styledClasses[css]
        return className ?: scheduleToInjectClassName(css)
    }

    private fun scheduleToInjectClassName(css: StyledCss): ClassName {
        val className = "ksc-$incrementedClassName"
        val selector = ".$className"
        val rules = css.getCssRules(selector)
        sheet.scheduleToInject(rules)

        styledClasses[css] = className
        return className
    }

    /**
     * Inject all scheduled rules into the DOM and clear scheduled rules.
     * If the rule cannot be parsed by the browser, it gets thrown away.
     */
    fun injectScheduled() {
        sheet.injectScheduled()
    }

    /**
     * Schedule CSS from [builder] for injection into the DOM with the corresponding [selector].
     * They will be injected when the [injectScheduled] function is called the next time.
     */
    fun scheduleToInject(selector: Selector, builder: CssBuilder) {
        if (!injectedStyleSheetRules.contains(selector)) {
            val styled = builder.toStyledCss()
            sheet.scheduleToInject(styled.getCssRules(selector))
            injectedStyleSheetRules.add(selector)
        }
    }

    fun scheduleImports(imports: Iterable<Import>) {
        sheet.scheduleToInject(imports.map { it.build() }, RuleType.IMPORT)
    }

    internal val injectedKeyframes = mutableMapOf<StyledKeyframes, ClassName>()

    /**
     * Schedule keyframes CSS in [builder] for injection into the DOM.
     * They will be injected when the [injectScheduled] function is called the next time.
     */
    fun scheduleToInject(builder: KeyframesBuilder.() -> Unit): ClassName {
        val keyframes = KeyframesBuilder().apply(builder).toStyledKeyframes()
        return injectedKeyframes[keyframes] ?: "ksc-keyframe-$incrementedClassName".also { keyframeName ->
            val css = keyframes.toString()
            injectedKeyframes[keyframes] = keyframeName
            val prefixes = listOf("@-webkit-keyframes", "@keyframes")
            sheet.scheduleToInject(prefixes.map { prefix -> "$prefix $keyframeName { $css }" })
        }
    }

    /**
     * @return pair of generated class name and a list of CSS class names, declared in [css].
     * If the CSS code for the [css] was not injected into the DOM previously, it is injected after function call.
     */
    internal fun getInjectedClassNames(styledCss: StyledCss): Pair<ClassName, List<ClassName>> {
        val selfClassName = getInjectedClassName(styledCss)
        val externalClassNames = styledCss.classes
        return Pair(selfClassName, externalClassNames)
    }

    /**
     * Show a warning when too many css blocks are created for one component
     */
    internal fun checkGeneratedCss(state: StateInstance<HashSet<String>?>, className: ClassName, type: String) {
        val (classes, setClasses) = state
        // Message was already shown
        if (classes == null) return

        val maxStylesForElement = 50
        val size = classes.size
        classes.add(className)
        if (classes.size > maxStylesForElement) {
            console.warn(
                "Over $maxStylesForElement were generated for $type. Consider using inline styles for frequently changed styles:\n\n" +
                        "styledDiv {\n" +
                        "    inlineStyles {\n" +
                        "        width = 100.px\n" +
                        "        backgroundColor = Color.blue\n" +
                        "    }\n" +
                        "}\n"
            )
            setClasses(null)
        } else {
            if (size != classes.size) {
                setClasses(classes)
            }
        }
    }
}
