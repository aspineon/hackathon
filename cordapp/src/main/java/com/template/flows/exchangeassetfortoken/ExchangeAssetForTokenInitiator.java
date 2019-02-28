package com.template.flows.exchangeassetfortoken;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.AssetContract;
import com.template.contracts.TokenContract;
import com.template.states.AssetState;
import com.template.states.TokenState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class ExchangeAssetForTokenInitiator extends FlowLogic<Void> {
    private final ProgressTracker progressTracker = new ProgressTracker();

    private final UUID assetId;
    private final Party counterparty;
    private final Integer amount;

    public ExchangeAssetForTokenInitiator(UUID assetId, Party counterparty, Integer amount) {
        this.assetId = assetId;
        this.counterparty = counterparty;
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

        // We retrieve the asset with the correct ID from our vault.
        LinearStateQueryCriteria assetCriteria = new LinearStateQueryCriteria(null, Arrays.asList(assetId));
        List<StateAndRef<AssetState>> assets = getServiceHub().getVaultService().queryBy(AssetState.class, assetCriteria).getStates();
        if (assets.size() == 0) throw new IllegalArgumentException("An asset with this ID was not found.");
        if (assets.size() > 1) throw new IllegalArgumentException("Multiple assets with this ID were found.");
        StateAndRef<AssetState> inputAssetStateAndRef = assets.get(0);
        AssetState inputAsset = inputAssetStateAndRef.getState().getData();

        // We get the counterparty to retrieve a token for the correct amount from their vault.
        FlowSession counterpartySession = initiateFlow(counterparty);
        counterpartySession.send(amount);
        StateAndRef<TokenState> inputTokenStateAndRef = subFlow(new ReceiveStateAndRefFlow<TokenState>(counterpartySession)).get(0);
        TokenState inputToken = inputTokenStateAndRef.getState().getData();

        // We create the outputs and commands.
        AssetState outputAsset = new AssetState(inputAsset.getIssuer(), counterparty, inputAsset.getDescription(), inputAsset.getLinearId());
        TokenState outputToken = new TokenState(inputToken.getIssuer(), getOurIdentity(), inputToken.getAmount());
        AssetContract.Commands.Transfer command = new AssetContract.Commands.Transfer();
        List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), counterparty.getOwningKey());

        // We build the transaction.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(inputAssetStateAndRef)
                .addInputState(inputTokenStateAndRef)
                .addOutputState(outputAsset, AssetContract.ID)
                .addOutputState(outputToken, TokenContract.ID)
                .addCommand(command, requiredSigners);

        // We sign the transaction.
        SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Arrays.asList(counterpartySession)));

        // We finalise the transaction.
        subFlow(new FinalityFlow(fullySignedTx));

        return null;
    }
}
