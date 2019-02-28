package com.hackathon.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.hackathon.contracts.TokenContract;
import com.hackathon.states.TokenState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class IssueToken extends FlowLogic<Void> {
    private final ProgressTracker progressTracker = new ProgressTracker();

    private final Party recipient;
    private final int amount;

    public IssueToken(Party recipient, int amount) {
        this.recipient = recipient;
        this.amount = amount;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        TokenState output = new TokenState(getOurIdentity(), recipient, amount);
        TokenContract.Commands.Issue command = new TokenContract.Commands.Issue();
        PublicKey requiredSigner = getOurIdentity().getOwningKey();

        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(output, TokenContract.ID)
                .addCommand(command, requiredSigner);

        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        subFlow(new FinalityFlow(signedTx));

        return null;
    }
}
