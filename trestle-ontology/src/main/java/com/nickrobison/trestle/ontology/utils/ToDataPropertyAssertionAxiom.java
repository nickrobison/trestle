package com.nickrobison.trestle.ontology.utils;

import com.nickrobison.trestle.ontology.types.TrestleResult;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.FlowableOperator;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;

/**
 * Created by nickrobison on 12/21/20.
 * <p>
 * Custom RxJava {@link FlowableOperator} which converts results from a {@link com.nickrobison.trestle.ontology.types.TrestleResultSet} into {@link OWLDataPropertyAssertionAxiom}
 * <p>
 * This expects that the {@link TrestleResult} has at least these three bindings: `individual`, 'property', 'object`.
 */
public class ToDataPropertyAssertionAxiom implements FlowableOperator<OWLDataPropertyAssertionAxiom, TrestleResult> {

    private final OWLDataFactory df;

    private ToDataPropertyAssertionAxiom(OWLDataFactory df) {
        super();
        this.df = df;
    }

    @Override
    public @NonNull Subscriber<? super TrestleResult> apply(@NonNull Subscriber<? super OWLDataPropertyAssertionAxiom> subscriber) {
        return new Op(df, subscriber);
    }

    public static ToDataPropertyAssertionAxiom toDataPropertyAssertionAxiom(OWLDataFactory df) {
        return new ToDataPropertyAssertionAxiom(df);
    }

    static final class Op implements FlowableSubscriber<TrestleResult>, Subscription {

        private final OWLDataFactory df;
        private final @NonNull Subscriber<? super OWLDataPropertyAssertionAxiom> subscriber;

        private Subscription s;

        Op(OWLDataFactory df, @NonNull Subscriber<? super OWLDataPropertyAssertionAxiom> subscriber) {
            this.df = df;
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(@NonNull Subscription s) {
            this.s = s;
            this.subscriber.onSubscribe(this);
        }

        @Override
        public void onNext(TrestleResult result) {
            try {
                final OWLDataPropertyAssertionAxiom axiom = df.getOWLDataPropertyAssertionAxiom(
                        df.getOWLDataProperty(IRI.create(result.getIndividual("property").orElseThrow(() -> new RuntimeException("Unable to get property")).toStringID())),
                        df.getOWLNamedIndividual(IRI.create(result.getIndividual("individual").orElseThrow(() -> new RuntimeException("Unable to get individual")).toStringID())),
                        result.getLiteral("object").orElseThrow(() -> new RuntimeException("Unable to get object")));
                this.subscriber.onNext(axiom);
            } catch (Exception e) {
                this.subscriber.onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            this.subscriber.onError(t);
        }

        @Override
        public void onComplete() {
            this.subscriber.onComplete();
        }

        @Override
        public void request(long n) {
            this.s.request(n);
        }

        @Override
        public void cancel() {
            this.s.cancel();
        }
    }
}
