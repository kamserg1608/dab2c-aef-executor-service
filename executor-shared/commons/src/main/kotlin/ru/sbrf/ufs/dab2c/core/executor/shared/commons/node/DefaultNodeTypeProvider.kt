package ru.sbrf.ufs.dab2c.core.executor.shared.commons.node

import ru.sbrf.ufs.platform.core.env.NodeType
import ru.sbrf.ufs.platform.core.env.NodeTypeProvider

/**
 * Default [NodeTypeProvider] implementation.
 */
object DefaultNodeTypeProvider : NodeTypeProvider {

    /**
     *  Returns the default node type for this provider.
     *  @return [NodeType.PRIMARY].
     */
    override fun getNodeType(): NodeType = NodeType.PRIMARY
}
