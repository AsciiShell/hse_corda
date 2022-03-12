package com.exactpro.bootcamp.flows

import co.paralleluniverse.fibers.Suspendable
import com.exactpro.bootcamp.contracts.TokenContract
import com.exactpro.bootcamp.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.requireThat
import com.exactpro.bootcamp.states.CurrencyType
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import kotlin.math.round

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class TokenSwapFlowInitiator(
    private val transactionId1: SecureHash,
    private val outputIndex1: Int,
    private val transactionId2: SecureHash,
    private val outputIndex2: Int,
    private val needAmount: Double
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    @Throws(FlowException::class)
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

        // We create Swap command.
        val swapCommand = Command(TokenContract.Commands.Swap(), signers.map { it.owningKey })

        // We build our transaction.
        var transactionBuilder = TransactionBuilder(notary)
            .addInputState(inputState1)
            .addInputState(inputState2)
//            .addOutputState(outputState1, TokenContract.ID)
//            .addOutputState(outputState2, TokenContract.ID)
//            .addCommand(splitCommand)

        val issuer = ourIdentity

        var rate: Double = 2.0 // 1 * RICK == 2 * MORTY
        if (inputState1.state.data.currencyType == CurrencyType.MORTY)
            rate = 1 / rate
        if (((inputState1.state.data.amount * rate).roundTo(2) < needAmount) or
            (inputState2.state.data.amount < needAmount)
        ) {
            throw FlowException("Не хватает минералов")
        }
        val outputState1give = TokenState(
            issuer,
            inputState2.state.data.owner,
            (needAmount / rate).roundTo(2),
            inputState1.state.data.currencyType
        )
        transactionBuilder = transactionBuilder.addOutputState(outputState1give, TokenContract.ID)

        if ((inputState1.state.data.amount * rate).roundTo(2) > needAmount) {
            val outputState1leave = TokenState(
                issuer,
                issuer,
                (inputState1.state.data.amount - needAmount / rate).roundTo(2),
                inputState1.state.data.currencyType
            )
            transactionBuilder = transactionBuilder.addOutputState(outputState1leave, TokenContract.ID)
        }

        val outputState2give = TokenState(
            inputState2.state.data.owner,
            issuer,
            needAmount,
            inputState2.state.data.currencyType
        )
        transactionBuilder = transactionBuilder.addOutputState(outputState2give, TokenContract.ID)

        if (inputState2.state.data.amount > needAmount) {
            val outputState2leave = TokenState(
                inputState2.state.data.owner,
                inputState2.state.data.owner,
                inputState2.state.data.amount - needAmount,
                inputState2.state.data.currencyType
            )
            transactionBuilder = transactionBuilder.addOutputState(outputState2leave, TokenContract.ID)
        }
        transactionBuilder = transactionBuilder.addCommand(swapCommand)
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

private fun Double.roundTo(i: Int): Double {
    var multiplier = 1.0
    repeat(i) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

@InitiatedBy(TokenSwapFlowInitiator::class)
class TokenSwapFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.

        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
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
