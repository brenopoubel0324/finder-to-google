package br.com.findertogoogle.routing

/** Somente o valor de client; nunca aceita uma URL ou expressão de correspondência. */
object FinderClientSignature {
    const val MAX_LENGTH = 128
    private val pattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
    fun isValid(value: String): Boolean = value.length <= MAX_LENGTH && pattern.matches(value)
}
