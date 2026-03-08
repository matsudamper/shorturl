package io.ktor.network.selector

import kotlinx.coroutines.CancellableContinuation
import java.util.concurrent.atomic.AtomicReference

public class InterestSuspensionsMap {
    private val readHandlerReference = AtomicReference<CancellableContinuation<Unit>?>(null)
    private val writeHandlerReference = AtomicReference<CancellableContinuation<Unit>?>(null)
    private val connectHandlerReference = AtomicReference<CancellableContinuation<Unit>?>(null)
    private val acceptHandlerReference = AtomicReference<CancellableContinuation<Unit>?>(null)

    private val references = SelectInterest.AllInterests.map { interest ->
        when (interest) {
            SelectInterest.READ -> readHandlerReference
            SelectInterest.WRITE -> writeHandlerReference
            SelectInterest.ACCEPT -> acceptHandlerReference
            SelectInterest.CONNECT -> connectHandlerReference
        }
    }.toTypedArray()

    public fun addSuspension(interest: SelectInterest, continuation: CancellableContinuation<Unit>) {
        val reference = reference(interest)
        if (!reference.compareAndSet(null, continuation)) {
            error("Handler for ${interest.name} is already registered")
        }
    }

    public inline fun invokeForEachPresent(readyOps: Int, block: CancellableContinuation<Unit>.() -> Unit) {
        val flags = SelectInterest.flags
        for (ordinal in flags.indices) {
            if (flags[ordinal] and readyOps != 0) {
                removeSuspension(ordinal)?.block()
            }
        }
    }

    public inline fun invokeForEachPresent(block: CancellableContinuation<Unit>.(SelectInterest) -> Unit) {
        for (interest in SelectInterest.AllInterests) {
            removeSuspension(interest)?.run { block(interest) }
        }
    }

    public fun removeSuspension(interest: SelectInterest): CancellableContinuation<Unit>? =
        reference(interest).getAndSet(null)

    public fun removeSuspension(interestOrdinal: Int): CancellableContinuation<Unit>? =
        references[interestOrdinal].getAndSet(null)

    override fun toString(): String {
        return "R ${readHandlerReference.get()} W ${writeHandlerReference.get()} C ${connectHandlerReference.get()} A ${acceptHandlerReference.get()}"
    }

    private fun reference(interest: SelectInterest): AtomicReference<CancellableContinuation<Unit>?> =
        references[interest.ordinal]
}
