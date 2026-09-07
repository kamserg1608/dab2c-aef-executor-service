package ru.sbrf.dab2c.executor.domain.voice

import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Dialog context exchanged with the agent; replaced wholesale by an agent response that carries one,
 * kept as it was by a response that does not.
 */
data class DialogContext(val data: ObjectNode)
