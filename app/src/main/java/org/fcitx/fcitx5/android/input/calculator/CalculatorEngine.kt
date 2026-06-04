package org.fcitx.fcitx5.android.input.calculator

import org.fcitx.fcitx5.android.core.CandidateWord
import timber.log.Timber

class CalculatorEngine {

    companion object {
        const val KP_0 = 0xffb0
        const val KP_1 = 0xffb1
        const val KP_2 = 0xffb2
        const val KP_3 = 0xffb3
        const val KP_4 = 0xffb4
        const val KP_5 = 0xffb5
        const val KP_6 = 0xffb6
        const val KP_7 = 0xffb7
        const val KP_8 = 0xffb8
        const val KP_9 = 0xffb9
        const val KP_Add = 0xffab
        const val KP_Subtract = 0xffad
        const val KP_Multiply = 0xffaa
        const val KP_Divide = 0xffaf
        const val KP_Decimal = 0xffae
        const val KP_Equal = 0xffbd
        const val Key_BackSpace = 0xff08

        val calculatorSyms = setOf(
            KP_0, KP_1, KP_2, KP_3, KP_4, KP_5, KP_6, KP_7, KP_8, KP_9,
            KP_Add, KP_Subtract, KP_Multiply, KP_Divide, KP_Decimal, KP_Equal,
            Key_BackSpace
        )
    }

    private val expression = StringBuilder()

    var onCandidatesChanged: ((List<CandidateWord>, String) -> Unit)? = null
    var onCommitResult: ((String) -> Unit)? = null

    val hasExpression: Boolean get() = expression.isNotEmpty()
    val currentExpression: String get() = expression.toString()

    fun canHandleSym(sym: Int): Boolean = sym in calculatorSyms

    fun handleSym(sym: Int): Boolean {
        if (sym !in calculatorSyms) return false
        Timber.d("Calculator: handleSym sym=0x%04x expr=\"%s\"", sym, expression)
        when (sym) {
            KP_0, KP_1, KP_2, KP_3, KP_4, KP_5, KP_6, KP_7, KP_8, KP_9 -> {
                val digit = (sym - KP_0).toString()[0]
                expression.append(digit)
                update()
            }
            KP_Add -> {
                appendOperator('+')
            }
            KP_Subtract -> {
                appendOperator('-')
            }
            KP_Multiply -> {
                appendOperator('*')
            }
            KP_Divide -> {
                appendOperator('/')
            }
            KP_Decimal -> {
                if (expression.isEmpty() || expression.last() !in "0123456789.") {
                    expression.append("0")
                }
                val lastNumStart = expression.lastIndexOfAny(
                    charArrayOf('+', '-', '*', '/')
                ) + 1
                val lastNum = expression.substring(lastNumStart)
                if ('.' !in lastNum) {
                    expression.append('.')
                }
                update()
            }
            KP_Equal -> {
                reset()
                return false
            }
            Key_BackSpace -> {
                if (expression.isNotEmpty()) {
                    expression.deleteCharAt(expression.length - 1)
                    update()
                    return true
                }
                return false
            }
        }
        return true
    }

    private fun appendOperator(op: Char) {
        if (expression.isEmpty()) return
        val last = expression.last()
        if (last in "+-*/") {
            expression.setCharAt(expression.length - 1, op)
        } else {
            expression.append(op)
        }
        update()
    }

    private fun update() {
        val expr = expression.toString()
        val candidates = mutableListOf<CandidateWord>()
        val result = ExpressionEvaluator.evaluate(expr)
        Timber.d("Calculator: update expr=\"%s\" evaluateResult=%s", expr, result)
        if (result != null) {
            val resultStr = formatResult(result)
            candidates.add(CandidateWord("", resultStr, ""))
            candidates.add(CandidateWord("", "${expr}=${resultStr}", ""))
        }
        onCandidatesChanged?.invoke(candidates, expr)
    }

    private fun formatResult(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    fun reset() {
        expression.clear()
        onCandidatesChanged?.invoke(emptyList(), "")
    }
}
