package com.exactpro.bootcamp.contracts

import com.exactpro.bootcamp.states.CurrencyType
import com.exactpro.bootcamp.states.TokenState
import net.corda.core.contracts.ContractState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Test
import kotlin.test.*

class StateTests {
    private val alice = TestIdentity(CordaX500Name("Alice", "Kostroma", "RU")).party
    private val bob = TestIdentity(CordaX500Name("Bob", "Tomsk", "RU")).party

    @Test
    fun tokenStateHasIssuerOwnerAndAmountParamsOfCorrectTypeInConstructor() {
        TokenState(alice, bob, 1.0, CurrencyType.RICK)
    }

    @Test
    fun tokenStateHasGettersForIssuerOwnerAndAmount() {
        val tokenState = TokenState(alice, bob, 1.0, CurrencyType.RICK)
        assertEquals(alice, tokenState.issuer)
        assertEquals(bob, tokenState.owner)
        assertEquals(1.0, tokenState.amount)
    }

    @Test
    @Suppress("USELESS_IS_CHECK")
    fun tokenStateImplementsContractState() {
        assertTrue(TokenState(alice, bob, 1.0, CurrencyType.RICK) is ContractState)
    }

    @Test
    fun tokenStateHasTwoParticipantsTheIssuerAndTheOwner() {
        val tokenState = TokenState(alice, bob, 1.0, CurrencyType.RICK)
        assertEquals(2, tokenState.participants.size)
        assertTrue(tokenState.participants.contains(alice))
        assertTrue(tokenState.participants.contains(bob))
    }
}