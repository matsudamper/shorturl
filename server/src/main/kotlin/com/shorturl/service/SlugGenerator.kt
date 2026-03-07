package com.shorturl.service

import com.shorturl.repository.UrlRepository

object SlugGenerator {

    private val ALPHANUMERIC = (('a'..'z') + ('A'..'Z') + ('0'..'9')).toList()
    private val LOWERCASE_DIGITS = (('a'..'z') + ('0'..'9')).toList()
    private val DIGITS = ('0'..'9').toList()
    private val CONSONANTS = "bcdfghjklmnpqrstvwxyz".toList()
    private val VOWELS = "aeiou".toList()
    private val EMOJIS = listOf(
        "😀", "😂", "🥰", "😎", "🤔", "🎉", "🔥", "💯", "⭐", "🌟",
        "🎸", "🌊", "🦄", "🍕", "🎮", "🚀", "💡", "🎯", "🌈", "🍀",
        "🎵", "🏆", "💎", "🌸", "🦋", "🐬", "🦁", "🌺", "🎨", "🍜",
        "🐱", "🐶", "🐸", "🦊", "🐺", "🦝", "🐧", "🦜", "🌙", "☀️",
    )

    enum class Type { ALPHANUMERIC, LOWERCASE_DIGITS, DIGITS, PRONOUNCEABLE, EMOJI }

    fun defaultLength(type: Type): Int = when (type) {
        Type.ALPHANUMERIC, Type.LOWERCASE_DIGITS, Type.DIGITS -> 3
        Type.PRONOUNCEABLE -> 4
        Type.EMOJI -> 2
    }

    fun minLength(type: Type): Int = when (type) {
        Type.ALPHANUMERIC, Type.LOWERCASE_DIGITS, Type.DIGITS -> 2
        Type.PRONOUNCEABLE -> 4
        Type.EMOJI -> 2
    }

    fun maxLength(type: Type): Int = when (type) {
        Type.ALPHANUMERIC, Type.LOWERCASE_DIGITS, Type.DIGITS -> 10
        Type.PRONOUNCEABLE -> 12
        Type.EMOJI -> 6
    }

    fun generate(type: Type, length: Int): String = when (type) {
        Type.ALPHANUMERIC -> ALPHANUMERIC.shuffled().take(length).let {
            (1..length).map { ALPHANUMERIC.random() }.joinToString("")
        }
        Type.LOWERCASE_DIGITS -> (1..length).map { LOWERCASE_DIGITS.random() }.joinToString("")
        Type.DIGITS -> (1..length).map { DIGITS.random() }.joinToString("")
        Type.PRONOUNCEABLE -> generatePronounceable(length)
        Type.EMOJI -> (1..length).map { EMOJIS.random() }.joinToString("")
    }

    fun generateUnique(type: Type, length: Int, maxRetries: Int = 5): String? {
        repeat(maxRetries) {
            val slug = generate(type, length)
            if (!UrlRepository.existsBySlug(slug)) return slug
        }
        return null
    }

    fun parseType(name: String): Type? = when (name.uppercase()) {
        "ALPHANUMERIC" -> Type.ALPHANUMERIC
        "LOWERCASE_DIGITS" -> Type.LOWERCASE_DIGITS
        "DIGITS" -> Type.DIGITS
        "PRONOUNCEABLE" -> Type.PRONOUNCEABLE
        "EMOJI" -> Type.EMOJI
        else -> null
    }

    private fun generatePronounceable(length: Int): String {
        val sb = StringBuilder()
        var useConsonant = true
        repeat(length) {
            sb.append(if (useConsonant) CONSONANTS.random() else VOWELS.random())
            useConsonant = !useConsonant
        }
        return sb.toString()
    }
}
