package org.fcitx.fcitx5.android.input.calculator

object ExpressionEvaluator {

    fun evaluate(expression: String): Double? {
        if (expression.isBlank()) return null
        val tokens = tokenize(expression) ?: return null
        if (tokens.isEmpty()) return null
        return try {
            val parser = Parser(tokens)
            val result = parser.parseExpression()
            if (parser.pos != tokens.size) null else result
        } catch (_: Exception) {
            null
        }
    }

    private sealed class Token {
        data class Number(val value: Double) : Token()
        data class Op(val op: Char) : Token()
    }

    private fun tokenize(expr: String): List<Token>? {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c in "0123456789." -> {
                    val start = i
                    var dotCount = 0
                    while (i < expr.length && (expr[i].isDigit() || (expr[i] == '.' && dotCount++ == 0))) i++
                    val numStr = expr.substring(start, i)
                    if (numStr == "." || numStr.endsWith(".") || numStr.count { it == '.' } > 1) return null
                    val value = numStr.toDoubleOrNull() ?: return null
                    tokens.add(Token.Number(value))
                }
                c in "+-*/" -> {
                    tokens.add(Token.Op(c))
                    i++
                }
                else -> return null
            }
        }
        return tokens
    }

    private class Parser(private val tokens: List<Token>) {
        var pos = 0

        fun parseExpression(): Double {
            var left = parseTerm()
            while (pos < tokens.size) {
                when (val token = tokens[pos]) {
                    is Token.Op -> when (token.op) {
                        '+', '-' -> {
                            pos++
                            val right = parseTerm()
                            left = if (token.op == '+') left + right else left - right
                        }
                        else -> break
                    }
                    else -> break
                }
            }
            return left
        }

        private fun parseTerm(): Double {
            var left = parseFactor()
            while (pos < tokens.size) {
                when (val token = tokens[pos]) {
                    is Token.Op -> when (token.op) {
                        '*', '/' -> {
                            pos++
                            val right = parseFactor()
                            left = if (token.op == '*') {
                                left * right
                            } else {
                                if (right == 0.0) throw ArithmeticException("Division by zero")
                                left / right
                            }
                        }
                        else -> break
                    }
                    else -> break
                }
            }
            return left
        }

        private fun parseFactor(): Double {
            val token = tokens.getOrNull(pos) ?: throw IllegalArgumentException("Unexpected end")
            return when (token) {
                is Token.Number -> {
                    pos++
                    token.value
                }
                else -> throw IllegalArgumentException("Unexpected token: $token")
            }
        }
    }
}
