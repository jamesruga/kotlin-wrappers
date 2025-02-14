// Automatically generated - do not modify!

package react.dom

import org.w3c.dom.Element

external interface PointerEvent<T : Element> : MouseEvent<T, NativePointerEvent> {
    val pointerId: Int
    val pressure: Double
    val tangentialPressure: Double
    val tiltX: Double
    val tiltY: Double
    val twist: Int
    val width: Double
    val height: Double
    val pointerType: String // 'mouse' | 'pen' | 'touch'
    val isPrimary: Boolean
}
