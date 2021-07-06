package com.exactpro.bootcamp.contracts

import com.exactpro.bootcamp.states.TokenState
import net.corda.core.contracts.Contract
import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.DummyCommandData
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.transaction
import org.junit.Test
import kotlin.test.*

class ContractTests {
    private val alice = TestIdentity(CordaX500Name("Alice", "Kostroma", "RU")).party
    private val bob = TestIdentity(CordaX500Name("Bob", "Tomsk", "RU")).party

    private val tokenState = TokenState(alice, bob, 1)

    private val ledgerServices = MockServices()

    @Test
    @Suppress("USELESS_IS_CHECK")
    fun tokenContractImplementsContract() {
        assertTrue(TokenContract() is Contract)
    }

    @Test
    fun tokenContractRequiresZeroInputsInTheTransaction() {
        ledgerServices.transaction {
            // Has an input, will fail.
            input(TokenContract.ID, tokenState)
            output(TokenContract.ID, tokenState)
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.fails()
        }

        ledgerServices.transaction {
            // Has no input, will verify.
            output(TokenContract.ID, tokenState)
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.verifies()
        }
    }

    @Test
    fun tokenContractRequiresOneOutputInTheTransaction() {
        ledgerServices.transaction {
            // Has two outputs, will fail.
            output(TokenContract.ID, tokenState)
            output(TokenContract.ID, tokenState)
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.fails()
        }

        ledgerServices.transaction {
            // Has one output, will verify.
            output(TokenContract.ID, tokenState)
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.verifies()
        }
    }

    @Test
    fun tokenContractRequiresOneCommandInTheTransaction() {
        ledgerServices.transaction {
            output(TokenContract.ID, tokenState)
            // Has two commands, will fail.
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.fails()
        }

        ledgerServices.transaction {
            output(TokenContract.ID, tokenState)
            // Has one command, will verify.
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.verifies()
        }
    }

    @Test
    fun tokenContractRequiresTheTransactionsOutputToBeATokenState() {
        ledgerServices.transaction {
            // Has wrong output type, will fail.
            output(TokenContract.ID, DummyState())
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.fails()
        }

        ledgerServices.transaction {
            // Has correct output type, will verify.
            output(TokenContract.ID, tokenState)
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.verifies()
        }
    }

    @Test
    fun tokenContractRequiresTheTransactionsOutputToHaveAPositiveAmount() {
        val zeroTokenState = TokenState(alice, bob, 0)
        val negativeTokenState = TokenState(alice, bob, -1)
        val positiveTokenState = TokenState(alice, bob, 2)

        ledgerServices.transaction {
            // Has zero-amount TokenState, will fail.
            output(TokenContract.ID, zeroTokenState)
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.fails()
        }

        ledgerServices.transaction {
            // Has negative-amount TokenState, will fail.
            output(TokenContract.ID, negativeTokenState)
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.fails()
        }

        ledgerServices.transaction {
            // Has positive-amount TokenState, will verify.
            output(TokenContract.ID, tokenState)
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.verifies()
        }

        ledgerServices.transaction {
            // Also has positive-amount TokenState, will verify.
            output(TokenContract.ID, positiveTokenState)
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.verifies()
        }
    }

    @Test
    fun tokenContractRequiresTheTransactionsCommandToBeAnIssueCommand() {
        ledgerServices.transaction {
            output(TokenContract.ID, tokenState)
            // Has wrong command type, will fail.
            command(listOf(alice.owningKey, bob.owningKey), DummyCommandData)

            this.fails()
        }

        ledgerServices.transaction {
            output(TokenContract.ID, tokenState)
            // Has correct command type, will verify.
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())

            this.verifies()
        }
    }

    @Test
    fun tokenContractRequiresTheIssuerToBeARequiredSignerInTheTransaction() {
        val tokenStateWhereBobIsIssuer = TokenState(bob, alice, 1);

        ledgerServices.transaction {
            output(TokenContract.ID, tokenState)
            // Issuer is not a required signer, will fail.
            command(listOf(bob.owningKey), TokenContract.Commands.Issue())

            this.fails()
        }

        ledgerServices.transaction {
            output(TokenContract.ID, tokenStateWhereBobIsIssuer)
            // Issuer is not a required signer, will fail.
            command(listOf(alice.owningKey), TokenContract.Commands.Issue())

            this.fails()
        }

        ledgerServices.transaction {
            output(TokenContract.ID, tokenState)
            // Issuer is not a required signer, will fail.
            command(listOf(alice.owningKey), TokenContract.Commands.Issue())

            this.verifies()
        }

        ledgerServices.transaction {
            output(TokenContract.ID, tokenStateWhereBobIsIssuer)
            // Issuer is not a required signer, will fail.
            command(listOf(bob.owningKey), TokenContract.Commands.Issue())

            this.verifies()
        }
    }

    /* ============================================================================
     *                     TODO 1 - Create Move command tests
     * ===========================================================================*/
}