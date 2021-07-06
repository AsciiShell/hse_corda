package com.exactpro.bootcamp.flows

import co.paralleluniverse.fibers.Suspendable
import com.exactpro.bootcamp.contracts.TokenContract
import com.exactpro.bootcamp.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class TokenIssueFlowInitiator(
    private val owner: Party,
    private val amount: Int
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Initiator flow logic goes here.

        // We choose our transaction's notary (the notary prevents double-spends).
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // We get a reference to our own identity.
        val issuer = ourIdentity

        // We create our new TokenState.
        val outputState = TokenState(
            issuer,
            owner,
            amount
        )

        // We create Issue command.
        val issueCommand = Command(TokenContract.Commands.Issue(), listOf(issuer.owningKey, owner.owningKey))

        // We build our transaction.
        val transactionBuilder = TransactionBuilder(notary)
            .addOutputState(outputState, TokenContract.ID)
            .addCommand(issueCommand)

        // We check our transaction is valid based on its contracts.
        transactionBuilder.verify(serviceHub)

        // We sign the transaction with our private key, making it immutable.
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // Create a session with the other party.
        val session = initiateFlow(owner)

        // The counterparty signs the transaction
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))

        // We get the transaction notarised and recorded automatically by the platform.
        return subFlow(FinalityFlow(fullySignedTransaction, listOf(session)))
    }
}

@InitiatedBy(TokenIssueFlowInitiator::class)
class TokenIssueFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.

        val signedTransactionFlow = object: SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                /* ============================================================================
                 *             TODO 3 - Implement responder flow transaction checks here
                 * ===========================================================================*/
            }
        }

        // The counterparty signs the transaction
        val expectedTransactionId = subFlow(signedTransactionFlow).id

        // The counterparty receives the transaction and saves the state
        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTransactionId))
    }
}
