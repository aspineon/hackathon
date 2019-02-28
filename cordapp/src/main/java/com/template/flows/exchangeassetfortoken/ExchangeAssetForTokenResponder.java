package com.template.flows.exchangeassetfortoken;

import co.paralleluniverse.fibers.Suspendable;
import com.template.states.TokenState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

import java.util.Arrays;
import java.util.List;

// ******************
// * Responder flow *
// ******************
@InitiatedBy(ExchangeAssetForTokenInitiator.class)
public class ExchangeAssetForTokenResponder extends FlowLogic<Void> {
    private FlowSession counterpartySession;

    public ExchangeAssetForTokenResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // The counterparty tells us how much to pay.
        int amount = counterpartySession.receive(Integer.class).unwrap(it -> it);

        // We find a token for the correct amount.
        List<StateAndRef<TokenState>> tokens = getServiceHub().getVaultService().queryBy(TokenState.class).getStates();
        StateAndRef<TokenState> inputTokenStateAndRef = tokens.stream().filter(artStateAndRef ->
                artStateAndRef.getState().getData().getAmount() == amount)
                .findAny().orElseThrow(() -> new FlowException("A token for the correct amount was not found."));

        // We send the token to the counterparty.
        subFlow(new SendStateAndRefFlow(counterpartySession, Arrays.asList(inputTokenStateAndRef)));

        // We sign the transaction.
        subFlow(new SignTransactionFlow(counterpartySession, SignTransactionFlow.tracker()) {
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                // TODO: Up to the hackathon participant!
            }
        });

        return null;
    }
}
