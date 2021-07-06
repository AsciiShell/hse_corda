package com.exactpro.bootcamp

import com.exactpro.bootcamp.contracts.TokenContract
import com.exactpro.bootcamp.flows.TokenIssueFlowInitiator
import com.exactpro.bootcamp.flows.TokenIssueFlowResponder
import com.exactpro.bootcamp.states.TokenState
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*


class FlowTests {

    lateinit var mockNetwork: MockNetwork
    lateinit var nodeA: StartedMockNode
    lateinit var nodeB: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(
                listOf("com.exactpro.bootcamp"),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        )
        nodeA = mockNetwork.createNode(MockNodeParameters())
        nodeB = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(nodeA, nodeB)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach { it.registerInitiatedFlow(TokenIssueFlowResponder::class.java) }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    @Test
    fun transactionConstructedByFlowUsesTheCorrectNotary() {
        val flow = TokenIssueFlowInitiator(nodeB.info.legalIdentities.first(), 99)
        val future = nodeA.startFlow(flow)
        mockNetwork.runNetwork()

        val signedTransaction = future.getOrThrow()
        assertEquals(1, signedTransaction.tx.outputs.size)

        val output = signedTransaction.tx.outputs.single()
        assertEquals(mockNetwork.notaryNodes.first().info.legalIdentities.first(), output.notary)
    }

    @Test
    fun transactionConstructedByFlowHasOneTokenStateOutputWithTheCorrectAmountAndOwner() {
        val flow = TokenIssueFlowInitiator(nodeB.info.legalIdentities.first(), 99)
        val future = nodeA.startFlow(flow)
        mockNetwork.runNetwork()

        val signedTransaction = future.getOrThrow()
        assertEquals(1, signedTransaction.tx.outputs.size)

        val output = signedTransaction.tx.outputsOfType<TokenState>().single()
        assertEquals(nodeB.info.legalIdentities.first(), output.owner)
        assertEquals(99, output.amount)
    }

    @Test
    fun transactionConstructedByFlowHasOneOutputUsingTheCorrectContract() {
        val flow = TokenIssueFlowInitiator(nodeB.info.legalIdentities.first(), 99)
        val future = nodeA.startFlow(flow)
        mockNetwork.runNetwork()

        val signedTransaction = future.getOrThrow()
        assertEquals(1, signedTransaction.tx.outputs.size)

        val output = signedTransaction.tx.outputs.single()
        assertEquals("com.exactpro.bootcamp.contracts.TokenContract", output.contract)
    }

    @Test
    fun transactionConstructedByFlowHasOneIssueCommand() {
        val flow = TokenIssueFlowInitiator(nodeB.info.legalIdentities.first(), 99)
        val future = nodeA.startFlow(flow)
        mockNetwork.runNetwork()

        val signedTransaction = future.getOrThrow()
        assertEquals(1, signedTransaction.tx.commands.size)

        val command = signedTransaction.tx.commands.single()
        assertTrue(command.value is TokenContract.Commands)
    }

    @Test
    fun transactionConstructedByFlowHasOneCommandWithTheIssuerAndTheOwnerAsASigners() {
        val flow = TokenIssueFlowInitiator(nodeB.info.legalIdentities.first(), 99)
        val future = nodeA.startFlow(flow)
        mockNetwork.runNetwork()

        val signedTransaction = future.getOrThrow()
        assertEquals(1, signedTransaction.tx.commands.size)

        val signers = signedTransaction.tx.commands.single().signers
        assertEquals(2, signers.size)
        assertTrue(signers.contains(nodeA.info.legalIdentities.first().owningKey))
        assertTrue(signers.contains(nodeB.info.legalIdentities.first().owningKey))
    }

    @Test
    fun transactionConstructedByFlowHasNoInputsAttachmentsOrTimeWindows() {
        val flow = TokenIssueFlowInitiator(nodeB.info.legalIdentities.first(), 99)
        val future = nodeA.startFlow(flow)
        mockNetwork.runNetwork()

        val signedTransaction = future.getOrThrow()
        assertEquals(0, signedTransaction.tx.inputs.size)
        // The single attachment is the contract attachment.
        assertEquals(1, signedTransaction.tx.attachments.size)
        assertNull(signedTransaction.tx.timeWindow)
    }

    /* ============================================================================
     *                      TODO 4 - Create Move flow tests
     * ===========================================================================*/
}