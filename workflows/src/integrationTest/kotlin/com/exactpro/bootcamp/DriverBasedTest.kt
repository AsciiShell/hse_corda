package com.exactpro.bootcamp

import com.exactpro.bootcamp.flows.TokenIssueFlowInitiator
import com.exactpro.bootcamp.states.CurrencyType
import com.exactpro.bootcamp.states.TokenState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.*

class DriverBasedTest {
    private val bankA = TestIdentity(CordaX500Name("BankA", "London", "GB"))
    private val bankB = TestIdentity(CordaX500Name("BankB", "Kostroma", "RU"))
    private val bankC = TestIdentity(CordaX500Name("BankC", "Tomsk", "RU"))

    @Test
    fun issueTest() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle, partyCHandle) = startNodes(bankA, bankB, bankC)

        val partyA = partyAHandle.nodeInfo.legalIdentities.first()
        val partyB = partyBHandle.nodeInfo.legalIdentities.first()

        // Run issue transaction using rpc
        partyAHandle.rpc.startFlow(::TokenIssueFlowInitiator, partyB, 100.0,  CurrencyType.RICK).returnValue.getOrThrow()

        // Query Node A
        val tokenStatesA = partyAHandle.rpc.vaultQueryBy<TokenState>()
        assertEquals(1, tokenStatesA.states.size)

        val tokenStateA = tokenStatesA.states.single().state.data
        assertEquals(partyA, tokenStateA.issuer)
        assertEquals(partyB, tokenStateA.owner)
        assertEquals(100.0, tokenStateA.amount)

        // Query Node B
        val tokenStatesB = partyBHandle.rpc.vaultQueryBy<TokenState>()
        assertEquals(1, tokenStatesB.states.size)

        val tokenStateB = tokenStatesB.states.single().state.data
        assertEquals(partyA, tokenStateB.issuer)
        assertEquals(partyB, tokenStateB.owner)
        assertEquals(100.0, tokenStateB.amount)

        // Query Node C
        val tokenStatesC = partyCHandle.rpc.vaultQueryBy<TokenState>()
        assertEquals(0, tokenStatesC.states.size)
    }

    /* ============================================================================
     *                      TODO 5 - Create Move flow tests
     * ===========================================================================*/

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(isDebug = true, startNodesInProcess = true)
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
        .map { startNode(providedName = it.name) }
        .waitForAll()
}