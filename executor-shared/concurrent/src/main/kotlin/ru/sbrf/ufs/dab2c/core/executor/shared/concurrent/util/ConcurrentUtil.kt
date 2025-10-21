package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.util

import java.util.concurrent.CompletableFuture

/**
 * Util class for wrap concurrent operations using [CompletableFuture].
 */
object ConcurrentUtil {

    /**
     * Combines the results of two asynchronous computations into a single
     * result using a provided [combinator] function.
     * Returns new [CompletableFuture] that is completed with the combined
     * result when both input futures complete normally, or completed exceptionally
     * if either of the input futures completes exceptionally.
     */
    fun <F, S, R> zipAsync(
        a: CompletableFuture<F>,
        b: CompletableFuture<S>,
        combinator: (F, S) -> R
    ): CompletableFuture<R> = CompletableFuture.allOf(a, b)
        .thenApply { combinator.invoke(a.get(), b.get()) }

    /**
     * Collects the results of multiple [futures] asynchronous computations into a list.
     * Returns list containing the results of all the input futures, in the order they were passed.
     * @throws [ExecutionException] If any of the input futures completes exceptionally.
     */
    fun <R> zipToList(vararg futures: CompletableFuture<R>): List<R> = CompletableFuture
        .allOf(*futures)
        .thenApply { futures.map { it.get() } }
        .get()
}
