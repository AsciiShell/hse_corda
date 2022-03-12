package com.exactpro.bootcamp.flows

import co.paralleluniverse.fibers.Suspendable
import com.exactpro.bootcamp.contracts.TokenContract
import com.exactpro.bootcamp.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
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
class TokenJoinFlowInitiator(
    private val transactionId1: SecureHash,
    private val outputIndex1: Int,
    private val transactionId2: SecureHash,
    private val outputIndex2: Int
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Initiator flow logic goes here.

        // We choose our transaction's notary (the notary prevents double-spends).
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // We get original state and reference
        val inputState1 = serviceHub.vaultService.queryBy<TokenState>(
            QueryCriteria.VaultQueryCriteria(stateRefs = listOf(StateRef(transactionId1, outputIndex1)))
        ).states.single()

        val inputState2 = serviceHub.vaultService.queryBy<TokenState>(
            QueryCriteria.VaultQueryCriteria(stateRefs = listOf(StateRef(transactionId2, outputIndex2)))
        ).states.single()

        // We get the required signers.
        val signers1 = inputState1.state.data.participants
        val signers2 = inputState2.state.data.participants
        val signers = (signers1 + signers2).distinct()

        val issuer = ourIdentity

        val outputState = TokenState(
            issuer,
            issuer,
            inputState1.state.data.amount + inputState2.state.data.amount,
            inputState1.state.data.currencyType
        )

        // We create Move command.
        val joinCommand = Command(TokenContract.Commands.Join(), signers.map { it.owningKey })

        // We build our transaction.
        val transactionBuilder = TransactionBuilder(notary)
            .addInputState(inputState1)
            .addInputState(inputState2)
            .addOutputState(outputState, TokenContract.ID)
            .addCommand(joinCommand)

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

@InitiatedBy(TokenJoinFlowInitiator::class)
class TokenJoinFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
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
