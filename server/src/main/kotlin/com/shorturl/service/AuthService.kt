package com.shorturl.service

import com.shorturl.model.User
import com.shorturl.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt

object AuthService {
    fun authenticate(username: String, password: String): User? {
        val user = UserRepository.findByUsername(username) ?: return null
        return if (BCrypt.checkpw(password, user.passwordHash)) user else null
    }

    fun hashPassword(password: String): String =
        BCrypt.hashpw(password, BCrypt.gensalt())
}
