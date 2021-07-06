package com.exactpro.bootcamp.contracts

import com.exactpro.bootcamp.states.TokenState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************

// Our contract, governing how our state will evolve over time.
class TokenContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.exactpro.bootcamp.contracts.TokenContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Issue -> requireThat {
                // Constraints on the shape of the transaction.
                "There must be no input states" using (tx.inputs.isEmpty())
                "There must be one output state" using (tx.outputs.size == 1)

                // Constraints on the content of the transaction.
                val outputState = tx.outputsOfType<TokenState>().single()
                "Token amount must be positive" using (outputState.amount > 0)

                // Constraints on the signers.
                val expectedSigners = outputState.issuer.owningKey
                "Issuer must be required signer" using (command.signers.contains(expectedSigners))
            }

            is Commands.Move -> requireThat {
                /* ============================================================================
                 *                    TODO 2 - Create Move command constraints
                 * ===========================================================================*/
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Move : Commands
    }
}