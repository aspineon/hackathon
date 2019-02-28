package com.hackathon.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.hackathon.contracts.AssetContract;
import com.hackathon.states.AssetState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.UUID;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class IssueAsset extends FlowLogic<UUID> {
    private final ProgressTracker progressTracker = new ProgressTracker();

    private final Party recipient;
    private final String description;

    public IssueAsset(Party recipient, String description) {
        this.recipient = recipient;
        this.description = description;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public UUID call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        AssetState output = new AssetState(getOurIdentity(), recipient, description);
        AssetContract.Commands.Issue command = new AssetContract.Commands.Issue();
        PublicKey requiredSigner = getOurIdentity().getOwningKey();

        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(output, AssetContract.ID)
                .addCommand(command, requiredSigner);

        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        subFlow(new FinalityFlow(signedTx));

        return output.getLinearId().getId();
    }
}
