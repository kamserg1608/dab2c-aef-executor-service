package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.extension

import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.util.ConcurrentUtil
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

/**
 * Combines the results of two lambdas async computations into a single
 * result using a provided [combinator] function.
 * Returns a new [CompletableFuture] that is completed with the combined result
 * when both input futures complete normally,
 * or completed exceptionally if either of the input futures completes exceptionally.
 */
@Suppress("SpreadOperator")
fun <F, S, R> ExecutorService.zipAsync(
    a: () -> F,
    b: () -> S,
    combinator: (F, S) -> R
): CompletableFuture<R> = ConcurrentUtil.zipAsync(
    CompletableFuture.supplyAsync(a, this),
    CompletableFuture.supplyAsync(b, this),
    combinator
)

/**
 * Collects the results of list of lambdas asynchronously into a list.
 * Returns list containing the results of all the input lambdas, in the order they were passed.
 * @throws [ExecutionException] If any of the input futures completes exceptionally.
 */
@Suppress("SpreadOperator")
fun <R> ExecutorService.zipToList(futures: List<() -> R>): List<R> = zipToList(*futures.toTypedArray())

/**
 * Collects the results of multiple lambdas asynchronously into a list.
 * Returns list containing the results of all the input lambdas, in the order they were passed.
 * @throws [ExecutionException] If any of the input futures completes exceptionally.
 */
@Suppress("SpreadOperator")
fun <R> ExecutorService.zipToList(vararg futures: () -> R): List<R> = ConcurrentUtil.zipToList(
    *futures
        .map { CompletableFuture.supplyAsync(it, this) }
        .toTypedArray()
)
