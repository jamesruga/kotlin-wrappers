package styled.sheets

import kotlinx.dom.appendText

// A stylesheet that is injected by setting the text of a <style> tag. Useful in development mode,
// because the stylesheet can be easily viewed using devtools, but relatively slow.
internal class DevSheet : AbstractSheet() {
    private val style by lazy { appendStyleElement(styleId) }
    private val importsStyle by lazy { appendStyleElement(importStyleId) }
    private val scheduledRules = mutableListOf<String>()
    private val scheduledImportRules = mutableListOf<String>()

    override fun scheduleToInject(rules: Iterable<String>, type: RuleType) {
        when (type) {
            RuleType.REGULAR -> scheduledRules.addAll(rules)
            RuleType.IMPORT -> scheduledImportRules.addAll(rules)
        }
    }

    override fun injectScheduled() {
        style.appendText(scheduledRules.joinToString("\n"))
        importsStyle.appendText(scheduledImportRules.joinToString("\n"))
    }
}