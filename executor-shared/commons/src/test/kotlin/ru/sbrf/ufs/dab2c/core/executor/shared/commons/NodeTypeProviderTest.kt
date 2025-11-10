package ru.sbrf.ufs.dab2c.core.executor.shared.commons


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.node.NodeTypeProviderAutoConfiguration
import ru.sbrf.ufs.platform.core.env.NodeType
import ru.sbrf.ufs.platform.core.env.NodeTypeProvider

//@SuppressFBWarnings("NP_NULL_ON_SOME_PATH")
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [NodeTypeProviderAutoConfiguration::class])
class NodeTypeProviderTest {

    @Autowired
    private lateinit var nodeTypeProvider: NodeTypeProvider

    @Test
    fun `should use default node type provider`() {
        val nodeType = nodeTypeProvider.nodeType

        assertEquals(NodeType.PRIMARY, nodeType, "Node type should be PRIMARY")
    }
}
