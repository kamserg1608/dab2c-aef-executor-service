package ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.extensions

import io.mockk.MockKGateway

/**
 * Determines if the current object is a mock.
 *
 * @property isMock True if the object is a mock, false otherwise.
 */
@Suppress("SwallowedException")
val <T : Any> T.isMock: Boolean
    get() = try {
        MockKGateway.implementation().mockFactory.isMock(this)
    } catch (e: UninitializedPropertyAccessException) {
        false
    }
