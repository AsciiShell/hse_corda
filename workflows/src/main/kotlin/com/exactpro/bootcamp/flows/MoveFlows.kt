package com.exactpro.bootcamp.flows

import co.paralleluniverse.fibers.Suspendable
import com.exactpro.bootcamp.contracts.TokenContract
import com.exactpro.bootcamp.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class TokenMoveFlowInitiator(
    private val transactionId: SecureHash,
    private val outputIndex: Int,
    private val newOwner: Party
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Initiator flow logic goes here.

        // We choose our transaction's notary (the notary prevents double-spends).
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // We get original state and reference
        val inputState = serviceHub.vaultService.queryBy<TokenState>(
            QueryCriteria.VaultQueryCriteria(stateRefs = listOf(StateRef(transactionId, outputIndex)))
        ).states.single()

        // We get the required signers.
        val signers = inputState.state.data.participants + newOwner

        // We create a TokenState with new owner.
        val outputState = inputState.state.data.copy(owner = newOwner)

        // We create Move command.
        val moveCommand = Command(TokenContract.Commands.Move(), signers.map { it.owningKey })

        // We build our transaction.
        val transactionBuilder = TransactionBuilder(notary)
            .addInputState(inputState)
            .addOutputState(outputState, TokenContract.ID)
            .addCommand(moveCommand)

        // We check our transaction is valid based on its contracts.
        transactionBuilder.verify(serviceHub)

        // We sign the transaction with our private key, making it immutable.
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // Create sessions with the other parties.
        val sessions = (signers - ourIdentity).map { initiateFlow(it) }

        // The counterparties sign the transaction
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, sessions))

        // We get the transaction notarised and recorded automatically by the platform.
        return subFlow(FinalityFlow(fullySignedTransaction, sessions))
    }
}

@InitiatedBy(TokenMoveFlowInitiator::class)
class TokenMoveFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.

        val signedTransactionFlow = object: SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                /* ============================================================================
                 *             TODO 6 - Implement responder flow transaction checks here
                 * ===========================================================================*/
            }
        }

        // The counterparty signs the transaction
        val expectedTransactionId = subFlow(signedTransactionFlow).id

        // The counterparty receives the transaction and saves the state
        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTransactionId))
    }
}
