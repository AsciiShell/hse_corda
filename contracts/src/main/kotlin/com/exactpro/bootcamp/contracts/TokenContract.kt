package com.exactpro.bootcamp.contracts

import com.exactpro.bootcamp.states.TokenState
import com.exactpro.bootcamp.states.CurrencyType
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

        // Используется для сравнения float
        const val eta = 0.00001
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
            is Commands.Split -> requireThat {
                // Constraints on the shape of the transaction.
                "There must be one input state" using (tx.inputs.size == 1)
                "There must be two output states" using (tx.outputs.size == 2)

                // Constraints on the content of the transaction.
                val inputState = tx.inputsOfType<TokenState>().single()
                val outputState1 = tx.outputsOfType<TokenState>()[0]
                val outputState2 = tx.outputsOfType<TokenState>()[1]
                "Token amount must be positive" using (outputState1.amount > 0)
                "Token amount must be positive" using (outputState2.amount > 0)
                "Amount should be the same" using (kotlin.math.abs(outputState1.amount + outputState2.amount - inputState.amount) < eta)

                // Constraints on the signers.
                val expectedSigners = outputState1.owner.owningKey
                "Owner must be required signer" using (command.signers.contains(expectedSigners))
            }
            is Commands.Join -> requireThat {
                // Constraints on the shape of the transaction.
                "There must be two input states" using (tx.inputs.size == 2)
                "There must be one output state" using (tx.outputs.size == 1)

                // Constraints on the content of the transaction.
                val outputState = tx.outputsOfType<TokenState>().single()
                "Token amount must be positive" using (outputState.amount > 0)

                val inputState1 = tx.inputsOfType<TokenState>()[0]
                val inputState2 = tx.inputsOfType<TokenState>()[1]
                "CurrencyType must be same" using (inputState1.currencyType == inputState2.currencyType)
                "Owners must be same" using (inputState1.owner == inputState2.owner)

                "Amount should be the same" using (kotlin.math.abs(outputState.amount - inputState1.amount - inputState2.amount) < eta)
                // Constraints on the signers.
                val expectedSigners = outputState.owner.owningKey
                "Owner must be required signer" using (command.signers.contains(expectedSigners))
            }
            is Commands.Swap -> requireThat {
                // Constraints on the shape of the transaction.
                "There must be two input states" using (tx.inputs.size == 2)
                "There must be from two to four output states" using ((tx.outputs.size >= 2) and (tx.outputs.size <= 4))

                // Constraints on the content of the transaction.
                val outputStates = tx.outputsOfType<TokenState>()
                val counter = mutableMapOf<CurrencyType, Double>()
                for (item in outputStates) {
                    "Token amount must be positive" using (item.amount > 0)
                    if (item.currencyType in counter) {
                        counter[item.currencyType] = item.amount + counter[item.currencyType]!!
                    } else {
                        counter[item.currencyType] = item.amount
                    }
                }

                val inputState1 = tx.inputsOfType<TokenState>()[0]
                val inputState2 = tx.inputsOfType<TokenState>()[1]
                "CurrencyType must be different" using (inputState1.currencyType != inputState2.currencyType)

                "Amount should be the same" using (kotlin.math.abs(counter[inputState1.currencyType]!! - inputState1.amount) < eta)
                "Amount should be the same" using (kotlin.math.abs(counter[inputState2.currencyType]!! - inputState2.amount) < eta)
                // Constraints on the signers.
                "Owner 1 must be required signer" using (command.signers.contains(inputState1.owner.owningKey))
                "Owner 2 must be required signer" using (command.signers.contains(inputState2.owner.owningKey))
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Move : Commands
        class Split : Commands
        class Join : Commands
        class Swap : Commands
    }
}